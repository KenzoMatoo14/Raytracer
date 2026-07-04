package Raytracer;

import Raytracer.Objects.Object3D;

import java.awt.*;

/**
 * Intersection class stores information about a ray-object intersection.
 * Contains all data needed for shading calculations including position,
 * surface normal, distance, and the intersected object.
 */
public class Intersection {
    // Distance from ray origin to intersection point (parameter t in ray equation)
    private double t;

    // 3D coordinates of the intersection point in world space
    private Vector3D point;

    // Surface normal vector at the intersection point (normalized)
    private Vector3D normal;

    // Reference to the 3D object that was intersected
    private Object3D object;

    // Color at the intersection point (may be textured or base object color)
    private Color color = null;

    /**
     * Constructor creates an intersection record with essential information.
     * @param t Distance parameter along the ray to intersection
     * @param point 3D coordinates of intersection point
     * @param normal Surface normal vector at intersection
     * @param object The 3D object that was hit by the ray
     */
    public Intersection(double t, Vector3D point, Vector3D normal, Object3D object) {
        // Store distance parameter
        this.t = t;

        // Store intersection point coordinates
        this.point = point;

        // Store and normalize the surface normal to ensure unit length
        this.normal = normal.normalize();

        // Store reference to intersected object
        this.object = object;
    }

    /**
     * Gets the distance parameter t along the ray to the intersection.
     * Used for depth testing and ray equation calculations.
     * @return Distance from ray origin as double
     */
    public double getT() {
        return t;
    }

    /**
     * Gets the 3D coordinates of the intersection point.
     * Used for lighting calculations and further ray generation.
     * @return Intersection point as Vector3D
     */
    public Vector3D getPoint() {
        return point;
    }

    /**
     * Gets the surface normal vector at the intersection point.
     * Essential for lighting calculations and reflection/refraction.
     * @return Normalized surface normal as Vector3D
     */
    public Vector3D getNormal() {
        return normal;
    }

    /**
     * Gets the 3D object that was intersected by the ray.
     * Provides access to object properties like material and base color.
     * @return Reference to intersected Object3D
     */
    public Object3D getObject() {
        return object;
    }

    /**
     * Sets the color at the intersection point.
     * Typically used after texture sampling or procedural color calculation.
     * @param color Color value to assign to this intersection
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Gets the color at the intersection point.
     * Returns the specifically set color if available, otherwise falls back
     * to the base color of the intersected object.
     * @return Color at intersection point
     */
    public Color getColor() {
        return color != null ? color : object.getColor();
    }

    /**
     * Creates a string representation of the intersection for debugging.
     * Includes intersection point coordinates, normal vector, and distance.
     * @return Formatted string describing the intersection
     */
    @Override
    public String toString() {
        return String.format("Intersection at %s with normal %s (t=%.2f)", point, normal, t);
    }
}