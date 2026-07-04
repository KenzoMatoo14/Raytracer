package Raytracer;

/**
 * Represents a ray in 3D space used for ray tracing calculations.
 * A ray is defined by an origin point and a direction vector.
 */
public class Ray {
    private Vector3D origin;    // Starting point of the ray
    private Vector3D direction; // Direction vector of the ray (normalized)

    /**
     * Constructor to create a new ray
     * @param origin The starting point of the ray in 3D space
     * @param direction The direction vector of the ray (will be normalized)
     */
    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize(); // Ensure it's a unit vector for consistent calculations
    }

    /**
     * Gets the origin point of the ray
     * @return The starting point of the ray
     */
    public Vector3D getOrigin() {
        return origin;
    }

    /**
     * Gets the normalized direction vector of the ray
     * @return The direction vector of the ray
     */
    public Vector3D getDirection() {
        return direction;
    }

    /**
     * Computes a point along the ray at a given distance parameter 't'
     * Uses the parametric equation: P(t) = origin + t * direction
     * @param t The distance parameter along the ray
     * @return The point at distance 't' along the ray
     */
    public Vector3D getPoint(double t) {
        return origin.add(direction.multiply(t));
    }

    /**
     * Returns a string representation of the ray for debugging purposes
     * @return Formatted string showing origin and direction
     */
    @Override
    public String toString() {
        return String.format("Ray(Origin: %s, Direction: %s)", origin, direction);
    }
}