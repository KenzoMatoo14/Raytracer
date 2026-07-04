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
}