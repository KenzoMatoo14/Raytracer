package Raytracer.Objects;

import Raytracer.Intersection;
import Raytracer.Vector3D;

import java.awt.*;

/**
 * Represents a directional light source in the raytracer.
 * Directional lights simulate distant light sources (like the sun) where all light rays
 * are parallel and come from the same direction. The light's position is not used
 * for calculations - only the direction matters.
 */
public class DirectionalLight extends Light{
    // The direction from which light is coming (normalized vector)
    private Vector3D direction;

    /**
     * Constructor for creating a directional light.
     * @param direction The direction from which light is coming (will be normalized)
     * @param color The color of the light
     * @param intensity The intensity/brightness of the light
     */
    public DirectionalLight(Vector3D direction, Color color, double intensity) {
        // Call parent constructor with zero position (not used for directional lights)
        super(Vector3D.ZERO(), color, intensity);
        // Set and normalize the direction vector
        setDirection(direction);
    }

    /**
     * Gets the normalized direction vector of the light.
     * @return The direction from which light is coming
     */
    public Vector3D getDirection() {
        return direction;
    }

    /**
     * Sets the direction of the light and normalizes it.
     * @param direction The new direction vector (will be normalized)
     */
    public void setDirection(Vector3D direction) {
        // Store the normalized direction to ensure it's a unit vector
        this.direction = direction.normalize();
    }

    /**
     * Calculates the dot product between the surface normal and light direction (N · L).
     * This value is used in lighting calculations to determine how much light
     * hits the surface based on the angle between the normal and light direction.
     * @param intersection The intersection point containing surface normal and position
     * @return The N·L dot product clamped to [0, 1] (negative values become 0)
     */
    @Override
    public double getNDotL(Intersection intersection) {
        // Calculate N · L, where L is the inverse of the light direction
        // We negate the direction because the direction vector points FROM the light,
        // but we need the vector pointing TO the light for lighting calculations
        Vector3D L = direction.negate(); // Light direction is opposite to the directional vector
        // Calculate dot product and clamp to 0 (surfaces facing away from light get no illumination)
        return Math.max(intersection.getNormal().dot(L), 0.0); // Use instance dot
    }

    /**
     * Returns a randomly jittered version of this light's direction, used for soft-shadow
     * sampling. Unlike PointLight/SpotLight (which jitter a world-space position), a
     * directional light has no meaningful position — instead, `radius` is treated as an
     * angular radius in degrees (like the sun's ~0.5° apparent size), and the direction is
     * perturbed within a small cone of that angle. Returns the exact direction unchanged
     * when radius is 0 (hard shadows).
     * @return Jittered direction for one shadow-ray sample
     */
    public Vector3D getJitteredDirection() {
        if (radius <= 0.0) return direction;

        // Build an orthonormal basis around 'direction' to define the cone's cross-section
        Vector3D arbitrary = Math.abs(direction.getY()) < 0.99 ? new Vector3D(0, 1, 0) : new Vector3D(1, 0, 0);
        Vector3D tangent = direction.cross(arbitrary).normalize();
        Vector3D bitangent = direction.cross(tangent).normalize();

        // Small-angle approximation: offset within a disk of radius tan(angle) in the plane
        // perpendicular to 'direction', then re-normalize. Fine for the few-degree cones
        // realistic light sizes need.
        double maxOffset = Math.tan(Math.toRadians(radius));

        double lx, ly;
        do {
            lx = 2 * Math.random() - 1;
            ly = 2 * Math.random() - 1;
        } while (lx * lx + ly * ly > 1.0);
        lx *= maxOffset;
        ly *= maxOffset;

        return direction.add(tangent.multiply(lx)).add(bitangent.multiply(ly)).normalize();
    }
}