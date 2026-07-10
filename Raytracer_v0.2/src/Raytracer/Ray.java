package Raytracer;

/**
 * Represents a ray in 3D space used for ray tracing calculations.
 * A ray is defined by an origin point and a direction vector.
 */
public class Ray {
    private Vector3D origin;    // Starting point of the ray
    private Vector3D direction; // Direction vector of the ray (normalized)
    // Precomputed component-wise inverse of direction (1/dx, 1/dy, 1/dz).
    // BoundingBox.intersect() needs 1/dir for each of the 3 axes on every ray-box test — with
    // many box tests per ray, computing this once here instead of on every test avoids a lot
    // of redundant divisions.
    private Vector3D invDirection;

    /**
     * Constructor to create a new ray
     * @param origin The starting point of the ray in 3D space
     * @param direction The direction vector of the ray (will be normalized)
     */
    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize(); // Ensure it's a unit vector for consistent calculations
        this.invDirection = new Vector3D(1.0 / this.direction.x, 1.0 / this.direction.y, 1.0 / this.direction.z);
    }

    /**
     * Gets the precomputed component-wise inverse of the ray's direction.
     * @return Vector3D where each component is 1/direction component
     */
    public Vector3D getInvDirection() {
        return invDirection;
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