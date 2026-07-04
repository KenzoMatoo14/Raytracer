package Raytracer.Objects;

import java.awt.*;
import java.awt.image.BufferedImage;

import Raytracer.BVH.BoundingBox;
import Raytracer.Intersection;
import Raytracer.Material;
import Raytracer.Ray;
import Raytracer.Vector3D;

/**
 * Represents a sphere object in the raytracer.
 * Spheres are defined by a center point and radius, and support
 * ray intersection testing, texture mapping, and material properties.
 */
public class Sphere extends Object3D {
    // Radius of the sphere
    private double radius;

    /**
     * Constructor for creating a sphere with specified center, radius, and color.
     * @param center The 3D position of the sphere's center
     * @param radius The radius of the sphere
     * @param color The base color of the sphere
     */
    public Sphere(Vector3D center, double radius, Color color) {
        // Call parent Object3D constructor with center as position
        super(center, color);
        this.radius = radius;
    }

    /**
     * Gets the center position of the sphere.
     * @return Vector3D representing the sphere's center (same as position)
     */
    public Vector3D getCenter() {
        return getPosition();
    }

    /**
     * Gets the radius of the sphere.
     * @return The sphere's radius value
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Factory method to create a sphere with material and texture properties.
     * @param position The 3D position of the sphere's center
     * @param radius The radius of the sphere
     * @param color The base color of the sphere
     * @param material Material properties for lighting calculations
     * @param texture Optional texture image to apply to the sphere
     * @return A new Sphere object with the specified properties
     */
    public static Sphere loadSphere(Vector3D position, double radius, Color color, Material material, BufferedImage texture) {
        Sphere sphere = new Sphere(position, radius, color);
        sphere.setMaterial(material);
        sphere.setTexture(texture);
        return sphere;
    }

    /**
     * Tests for ray-sphere intersection using the geometric method.
     * This method solves the quadratic equation formed by substituting the ray equation
     * into the sphere equation to find intersection points.
     * @param ray The ray to test for intersection
     * @return Intersection object if ray hits sphere, null otherwise
     */
    @Override
    public Intersection intersect(Ray ray) {
        Vector3D center = getPosition();
        // Vector from ray origin to sphere center
        Vector3D L = center.subtract(ray.getOrigin()); // L = C - O
        // Project L onto ray direction to find closest approach point
        double tca = L.dot(ray.getDirection()); // tca = L · D

        // If tca < 0, sphere is behind the ray origin
        if (tca < 0) return null;  // Sphere is behind the ray, no intersection

        // Calculate squared distance from ray to sphere center
        double d2 = L.dot(L) - tca * tca; // d² = L·L - tca²
        double radius2 = radius * radius;

        // If distance is greater than radius, ray misses sphere
        if (d2 > radius2) return null; // No intersection

        // Calculate half-chord length (distance from closest approach to intersection points)
        double thc = Math.sqrt(radius2 - d2); // thc = sqrt(radius² - d²)
        // Calculate the two intersection parameters
        double t0 = tca - thc; // Near intersection
        double t1 = tca + thc; // Far intersection

        // Choose the closest positive intersection
        double t = (t0 > 0) ? t0 : (t1 > 0 ? t1 : -1);
        if (t < 0) return null; // No valid intersection

        // Compute intersection point and normal
        Vector3D intersectionPoint = ray.getPoint(t);
        // Normal vector points from center to intersection point
        Vector3D normal = intersectionPoint.subtract(center).normalize();

        // === UV Mapping for Spheres ===
        // Convert 3D surface normal to 2D texture coordinates (spherical mapping)
        // u ranges from 0 to 1 around the sphere (longitude)
        double u = 0.5 + (Math.atan2(normal.getZ(), normal.getX()) / (2 * Math.PI));
        // v ranges from 0 to 1 from bottom to top of sphere (latitude)
        double v = 0.5 - (Math.asin(normal.getY()) / Math.PI);

        // Apply texture if available, otherwise use base color
        Color texColor = sampleTextureUV(u, v);

        // Create intersection result with texture color
        Intersection result = new Intersection(t, intersectionPoint, normal, this);
        result.setColor(texColor);

        return result;
    }

    /**
     * Computes the axis-aligned bounding box for this sphere.
     * The bounding box extends from (center - radius) to (center + radius) in all dimensions.
     * @return BoundingBox that completely encloses the sphere
     */
    @Override
    public BoundingBox getBoundingBox() {
        Vector3D center = getCenter();
        double r = getRadius();
        // Create bounding box by extending radius in all directions from center
        Vector3D min = new Vector3D(center.getX() - r, center.getY() - r, center.getZ() - r);
        Vector3D max = new Vector3D(center.getX() + r, center.getY() + r, center.getZ() + r);
        return new BoundingBox(min, max);
    }

    /**
     * Returns a string representation of the sphere for debugging purposes.
     * @return String containing sphere's center, radius, and color information
     */
    @Override
    public String toString() {
        return String.format("Sphere(Center: %s, Radius: %.2f, Color: %s)", getCenter(), radius, getColor());
    }
}