package Raytracer.Objects;

import Raytracer.BVH.BoundingBox;
import Raytracer.Intersection;
import Raytracer.Ray;
import Raytracer.Vector3D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static java.lang.Math.clamp;

/**
 * Triangle class represents a triangular face in 3D space for ray tracing.
 * Extends Object3D and implements ray-triangle intersection using the Möller-Trumbore algorithm.
 * Supports vertex normals for smooth shading and texture mapping.
 */
public class Triangle extends Object3D {
    // Small epsilon value for floating-point comparisons to avoid numerical precision issues
    public static final double EPSILON = 1e-6;

    // Array storing the three vertices of the triangle
    private Vector3D[] vertices;

    // Array storing the normal vectors at each vertex for smooth shading interpolation
    private Vector3D[] normals;

    // Precomputed edge vectors for efficient intersection calculations
    private Vector3D edge1, edge2;

    // Face normal vector computed from the triangle's geometry
    private Vector3D faceNormal;

    // Static counter to track the number of ray-triangle intersection tests performed.
    // Uses LongAdder instead of a plain long: under parallel rendering, many threads
    // incrementing a shared `long` field via `triangleTests++` cause cache-line contention
    // (false sharing) severe enough to noticeably slow down the render itself, since this is
    // called on the order of hundreds of millions of times. LongAdder stripes the counter
    // internally across cells to avoid that contention.
    public static java.util.concurrent.atomic.LongAdder triangleTests = new java.util.concurrent.atomic.LongAdder();

    // Texture coordinates for each vertex (u,v mapping)
    private Vector3D[] texCoords = null;

    // Texture image for surface appearance
    private BufferedImage texture = null;

    // Cached bounding box — see getBoundingBox() for why this is lazily computed once
    private BoundingBox cachedBoundingBox = null;

    /**
     * Constructor for triangle with explicit vertex normals for smooth shading.
     * @param v0 First vertex position
     * @param v1 Second vertex position
     * @param v2 Third vertex position
     * @param n0 Normal vector at first vertex (null uses face normal)
     * @param n1 Normal vector at second vertex (null uses face normal)
     * @param n2 Normal vector at third vertex (null uses face normal)
     * @param color Base color of the triangle
     */
    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Vector3D n0, Vector3D n1, Vector3D n2, Color color) {
        // Call parent constructor with centroid position and color
        super(calculateCentroid(v0, v1, v2), color);

        // Store the three vertices
        this.vertices = new Vector3D[]{v0, v1, v2};

        // Precompute edge vectors for intersection algorithm
        this.edge1 = v1.subtract(v0);
        this.edge2 = v2.subtract(v0);

        // Calculate face normal using cross product of edges
        this.faceNormal = edge1.cross(edge2).normalize();

        // Set vertex normals, using face normal as fallback if null provided
        this.normals = new Vector3D[]{
                n0 != null ? n0.normalize() : faceNormal,
                n1 != null ? n1.normalize() : faceNormal,
                n2 != null ? n2.normalize() : faceNormal
        };
    }

    /**
     * Constructor for flat-shaded triangle (same normal for all vertices).
     * @param v0 First vertex position
     * @param v1 Second vertex position
     * @param v2 Third vertex position
     * @param color Base color of the triangle
     */
    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Color color) {
        // Call parent constructor with centroid position and color
        super(calculateCentroid(v0, v1, v2), color);

        // Store the three vertices
        this.vertices = new Vector3D[]{v0, v1, v2};

        // Precompute edge vectors for intersection algorithm
        this.edge1 = v1.subtract(v0);
        this.edge2 = v2.subtract(v0);

        // Calculate face normal using cross product of edges
        this.faceNormal = edge1.cross(edge2).normalize();

        // Use the same face normal for all vertices (flat shading)
        this.normals = new Vector3D[]{faceNormal, faceNormal, faceNormal};
    }

    /**
     * Calculates the centroid (geometric center) of the triangle.
     * @param v0 First vertex
     * @param v1 Second vertex
     * @param v2 Third vertex
     * @return Centroid position as Vector3D
     */
    private static Vector3D calculateCentroid(Vector3D v0, Vector3D v1, Vector3D v2) {
        return new Vector3D(
                (v0.getX() + v1.getX() + v2.getX()) / 3.0,
                (v0.getY() + v1.getY() + v2.getY()) / 3.0,
                (v0.getZ() + v1.getZ() + v2.getZ()) / 3.0
        );
    }

    /**
     * Gets the triangle's vertex positions.
     * @return Array of three vertex positions
     */
    public Vector3D[] getVertices() {
        return vertices;
    }

    /**
     * Gets the triangle's vertex normals.
     * @return Array of three vertex normal vectors
     */
    public Vector3D[] getNormals() {
        return normals;
    }

    /**
     * Sets texture coordinates for the triangle vertices.
     * @param texCoords Array of three texture coordinate vectors (u,v,w)
     */
    public void setTextureCoords(Vector3D[] texCoords) {
        this.texCoords = texCoords;
    }

    /**
     * Sets the texture image for this triangle.
     * @param texture BufferedImage containing the texture data
     */
    public void setTexture(BufferedImage texture) {
        this.texture = texture;
    }

    /**
     * Performs ray-triangle intersection using the Möller-Trumbore algorithm.
     * Returns detailed intersection information including interpolated normals and texture sampling.
     * @param ray The ray to test for intersection
     * @return Intersection object if hit occurs, null otherwise
     */
    @Override
    public Intersection intersect(Ray ray) {
        // Increment global counter for performance statistics
        triangleTests.increment();

        // Get vertex positions and normals for easier access
        Vector3D v0 = vertices[0], v1 = vertices[1], v2 = vertices[2];
        Vector3D n0 = normals[0], n1 = normals[1], n2 = normals[2];

        // Get ray direction vector
        Vector3D rayDir = ray.getDirection();

        // Begin Möller-Trumbore intersection algorithm
        // Calculate determinant using cross product of ray direction and edge2
        Vector3D h = rayDir.cross(edge2);
        double determinant = edge1.dot(h);

        // If determinant is near zero, ray is parallel to triangle plane
        if (Math.abs(determinant) < EPSILON) return null;

        // Calculate inverse determinant for efficiency
        double invDet = 1.0 / determinant;

        // Calculate vector from vertex 0 to ray origin
        Vector3D s = ray.getOrigin().subtract(v0);

        // Calculate first barycentric coordinate (u)
        double u = invDet * s.dot(h);

        // Check if intersection point is outside triangle (u coordinate test)
        if (u < -EPSILON || u > 1.0 + EPSILON) return null;

        // Calculate second barycentric coordinate (v)
        Vector3D q = s.cross(edge1);
        double v = invDet * rayDir.dot(q);

        // Check if intersection point is outside triangle (v coordinate test)
        if (v < -EPSILON || (u + v) > 1.0 + EPSILON) return null;

        // Calculate distance along ray to intersection point
        double t = invDet * edge2.dot(q);

        // Check if intersection is in positive ray direction and not too close to origin
        if (t > EPSILON) {
            // Calculate actual intersection point in 3D space
            Vector3D intersectionPoint = ray.getOrigin().add(rayDir.multiply(t));

            // Calculate third barycentric coordinate (w)
            double w = 1.0 - u - v;

            // Interpolate the surface normal using barycentric coordinates
            Vector3D interpolatedNormal;
            if (normals[0].equals(normals[1]) && normals[0].equals(normals[2])) {
                // All normals are the same (flat shading) - use directly
                interpolatedNormal = normals[0];
            } else {
                // Smooth shading - interpolate normals using barycentric coordinates
                interpolatedNormal = normals[0].multiply(w)
                        .add(normals[1].multiply(u))
                        .add(normals[2].multiply(v));
                // Normalize the interpolated normal vector
                interpolatedNormal = interpolatedNormal.normalize();
            }

            // Start with base object color
            Color texColor = getColor();

            // Sample texture if both texture and texture coordinates are available
            if (texture != null && texCoords != null) {
                // Calculate texture coordinates using barycentric interpolation
                double wTex = 1 - u - v;

                // Interpolate U texture coordinate
                double uTex = texCoords[0].getX() * wTex + texCoords[1].getX() * u + texCoords[2].getX() * v;
                // Interpolate V texture coordinate
                double vTex = texCoords[0].getY() * wTex + texCoords[1].getY() * u + texCoords[2].getY() * v;

                // Convert normalized texture coordinates to pixel coordinates
                // Clamp to valid texture bounds to prevent out-of-bounds access
                int texX = clamp((int)(uTex * (texture.getWidth() - 1)), 0, texture.getWidth() - 1);
                int texY = clamp((int)((1 - vTex) * (texture.getHeight() - 1)), 0, texture.getHeight() - 1);

                // Sample the texture color at the calculated coordinates
                texColor = new Color(texture.getRGB(texX, texY));
            }

            // Create intersection object with all calculated data
            Intersection hit = new Intersection(t, intersectionPoint, interpolatedNormal, this);
            hit.setColor(texColor);
            return hit;
        }

        // No valid intersection found
        return null;
    }

    /**
     * Calculates the axis-aligned bounding box for this triangle.
     * Used for spatial acceleration structures like BVH.
     * @return BoundingBox object containing the triangle
     */
    @Override
    public BoundingBox getBoundingBox() {
        if (cachedBoundingBox != null) {
            return cachedBoundingBox;
        }

        // Find minimum coordinates across all three vertices
        Vector3D min = new Vector3D(
                Math.min(vertices[0].getX(), Math.min(vertices[1].getX(), vertices[2].getX())),
                Math.min(vertices[0].getY(), Math.min(vertices[1].getY(), vertices[2].getY())),
                Math.min(vertices[0].getZ(), Math.min(vertices[1].getZ(), vertices[2].getZ()))
        );

        // Find maximum coordinates across all three vertices
        Vector3D max = new Vector3D(
                Math.max(vertices[0].getX(), Math.max(vertices[1].getX(), vertices[2].getX())),
                Math.max(vertices[0].getY(), Math.max(vertices[1].getY(), vertices[2].getY())),
                Math.max(vertices[0].getZ(), Math.max(vertices[1].getZ(), vertices[2].getZ()))
        );

        cachedBoundingBox = new BoundingBox(min, max);
        return cachedBoundingBox;
    }
}