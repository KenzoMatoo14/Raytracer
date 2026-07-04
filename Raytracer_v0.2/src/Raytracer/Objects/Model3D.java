package Raytracer.Objects;

import Raytracer.BVH.BoundingBox;
import Raytracer.Intersection;
import Raytracer.Material;
import Raytracer.Ray;
import Raytracer.Vector3D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static Raytracer.Vector3D.rotate;

/**
 * Represents a complex 3D model composed of multiple triangles.
 * This class allows for creating composite objects made up of triangle meshes,
 * such as imported 3D models or procedurally generated geometry like planes.
 */
public class Model3D extends Object3D {
    // List of triangles that make up this 3D model
    private List<Triangle> triangles;

    /**
     * Constructor for creating a 3D model from an array of triangles.
     * The triangles are positioned relative to the model's position.
     * @param position The world position of the model
     * @param triangles Array of triangles that compose the model
     * @param color The base color of the model
     */
    public Model3D(Vector3D position, Triangle[] triangles, Color color) {
        // Call parent Object3D constructor
        super(position, color);
        // Set up the triangles with position transformation
        setTriangles(triangles);
    }

    /**
     * Gets the list of triangles that make up this model.
     * @return List of Triangle objects composing the model
     */
    public List<Triangle> getTriangles() {
        return triangles;
    }

    /**
     * Sets the triangles for this model and transforms their vertices to world space.
     * This method finds all unique vertices in the triangles and translates them
     * by the model's position to place the model in the correct world location.
     * @param triangles Array of triangles to set for this model
     */
    public void setTriangles(Triangle[] triangles) {
        Vector3D position = getPosition();
        // Use a set to find all unique vertices across all triangles
        Set<Vector3D> uniqueVertices = new HashSet<>();
        for(Triangle triangle : triangles){
            // Add all vertices from each triangle to the set
            uniqueVertices.addAll(Arrays.asList(triangle.getVertices()));
        }

        // Transform all unique vertices by the model's position
        for(Vector3D vertex : uniqueVertices){
            // Translate each vertex by adding the model's position
            vertex.setX(vertex.getX() + position.getX());
            vertex.setY(vertex.getY() + position.getY());
            vertex.setZ(vertex.getZ() + position.getZ());
        }

        // Store the triangles as a list
        this.triangles = Arrays.asList(triangles);
    }

    /**
     * Factory method to create a rectangular plane model from specified parameters.
     * Creates a quad (two triangles) in 3D space with optional rotation and texturing.
     * @param position World position where the plane should be placed
     * @param sizeX Width of the plane along the X axis
     * @param sizeY Height of the plane along the Y axis
     * @param rotation Rotation angles (X, Y, Z) to apply to the plane
     * @param color Base color of the plane
     * @param material Material properties for lighting calculations
     * @param texture Optional texture image to apply to the plane
     * @return A new Model3D representing the plane
     */
    public static Model3D createPlane(Vector3D position, double sizeX, double sizeY, Vector3D rotation, Color color, Material material, BufferedImage texture) {
        // Calculate half-sizes for centering the plane
        double halfX = sizeX / 2.0;
        double halfY = sizeY / 2.0;

        // Define quad vertices on XY plane centered at origin
        Vector3D v0 = new Vector3D(-halfX, -halfY, 0); // Bottom-left
        Vector3D v1 = new Vector3D( halfX, -halfY, 0); // Bottom-right
        Vector3D v2 = new Vector3D( halfX,  halfY, 0); // Top-right
        Vector3D v3 = new Vector3D(-halfX,  halfY, 0); // Top-left

        // Apply rotation transformations to each vertex
        v0 = rotate(v0, rotation.getX(), rotation.getY(), rotation.getZ());
        v1 = rotate(v1, rotation.getX(), rotation.getY(), rotation.getZ());
        v2 = rotate(v2, rotation.getX(), rotation.getY(), rotation.getZ());
        v3 = rotate(v3, rotation.getX(), rotation.getY(), rotation.getZ());

        // Translate vertices to world position
        v0 = v0.add(position);
        v1 = v1.add(position);
        v2 = v2.add(position);
        v3 = v3.add(position);

        // Create two triangles to form the quad (winding order matters for normals)
        Triangle t1 = new Triangle(v2, v1, v0, color); // First triangle
        Triangle t2 = new Triangle(v3, v2, v0, color); // Second triangle

        Triangle[] triangles = new Triangle[]{t1, t2};

        // Construct the plane model
        Model3D plane = new Model3D(position, triangles, color);
        // Set material properties
        plane.setMaterial(material);
        // Apply texture if provided
        plane.setTexture(texture);

        return plane;
    }

    /**
     * Applies a texture to all triangles in this model.
     * @param texture The BufferedImage texture to apply to all triangles
     */
    public void setTexture(BufferedImage texture) {
        if (triangles != null) {
            // Set the texture on each triangle in the model
            for (Triangle tri : triangles) {
                tri.setTexture(texture);
            }
        }
    }

    /**
     * Sets the reflectivity property for this model and propagates it to all triangles.
     * Reflectivity controls how much light is reflected for mirror-like effects.
     * @param reflectivity Reflectivity coefficient (0.0 = no reflection, 1.0 = perfect mirror)
     */
    @Override
    public void setReflectivity(double reflectivity) {
        // Set reflectivity on the model wrapper
        super.setReflectivity(reflectivity);

        // Propagate reflectivity setting to all constituent triangles
        if (triangles != null) {
            for (Triangle triangle : triangles) {
                triangle.setReflectivity(reflectivity);
            }
        }
    }

    /**
     * Sets whether this model is refractive (transparent) and propagates to all triangles.
     * Refractive objects bend light as it passes through them (like glass).
     * @param refractive true if the model should refract light, false otherwise
     */
    @Override
    public void setRefractive(boolean refractive) {
        // Set refractive property on the model wrapper
        super.setRefractive(refractive);

        // Propagate refractive setting to all constituent triangles
        if (triangles != null) {
            for (Triangle triangle : triangles) {
                triangle.setRefractive(refractive);
            }
        }
    }

    /**
     * Sets the Index of Refraction (IOR) for this model and propagates to all triangles.
     * IOR determines how much light bends when entering/exiting the material.
     * @param ior Index of Refraction (1.0 = air, 1.33 = water, 1.5 = glass, etc.)
     */
    @Override
    public void setIOR(double ior) {
        // Set IOR on the model wrapper
        super.setIOR(ior);

        // Propagate IOR setting to all constituent triangles
        if (triangles != null) {
            for (Triangle triangle : triangles) {
                triangle.setIOR(ior);
            }
        }
    }

    /**
     * Tests for ray intersection with this model by checking all constituent triangles.
     * Returns the closest intersection found among all triangles in the model,
     * preserving texture color data from the triangle.
     * @param ray The ray to test for intersections
     * @return The closest Intersection object, or null if no intersection found
     */
    @Override
    public Intersection intersect(Ray ray) {
        Intersection closest = null;
        double minT = Double.POSITIVE_INFINITY;

        for (Triangle triangle : getTriangles()) {
            Intersection hit = triangle.intersect(ray);
            if (hit != null && hit.getT() > 0 && hit.getT() < minT) {
                minT = hit.getT();
                closest = hit;
            }
        }

        // Return the triangle's intersection directly to preserve texture color.
        // We only remap the 'object' reference to this Model3D so material lookups work.
        if (closest == null) return null;

        Intersection result = new Intersection(closest.getT(), closest.getPoint(), closest.getNormal(), this);
        result.setColor(closest.getColor()); // Preserve texture-sampled color from triangle
        return result;
    }

    /**
     * Computes the axis-aligned bounding box that encloses all triangles in this model.
     * The bounding box is used by the BVH for spatial partitioning and acceleration.
     * @return BoundingBox that contains all vertices of all triangles, or null if no triangles
     */
    @Override
    public BoundingBox getBoundingBox() {
        // Handle empty model case
        if (triangles == null || triangles.isEmpty()) {
            return null;
        }

        // Initialize min/max values to extreme values for comparison
        Vector3D min = new Vector3D(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Vector3D max = new Vector3D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        // Iterate through all triangles and their vertices
        for (Triangle triangle : triangles) {
            for (Vector3D vertex : triangle.getVertices()) {
                // Update minimum and maximum bounds for each coordinate
                min = Vector3D.min(min, vertex);
                max = Vector3D.max(max, vertex);
            }
        }

        // Create and return the bounding box
        return new BoundingBox(min, max);
    }
}