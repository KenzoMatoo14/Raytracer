package Raytracer.Objects;

import Raytracer.Intersection;
import Raytracer.Vector3D;

import java.awt.*;

/**
 * Represents a point light source in the raytracer.
 * Point lights emit light uniformly in all directions from a single point in space,
 * similar to a light bulb. The light direction varies depending on the surface point
 * being illuminated - it's always from the surface point toward the light position.
 */
public class PointLight extends Light {

    /**
     * Constructor for creating a point light.
     * @param position The 3D position of the light source in world space
     * @param color The color of the light emitted
     * @param intensity The brightness/intensity of the light
     */
    public PointLight(Vector3D position, Color color, double intensity) {
        // Call parent Light constructor with the specified parameters
        super(position, color, intensity);
    }

    /**
     * Calculates the dot product between the surface normal and light direction (N · L).
     * For point lights, the light direction varies for each surface point and is calculated
     * as the vector from the surface point to the light's position.
     * @param intersection The intersection point containing surface normal and position
     * @return The N·L dot product clamped to [0, 1] for diffuse lighting calculations
     */
    @Override
    public double getNDotL(Intersection intersection) {
        // Light direction is from surface point to light position
        // Calculate: L = lightPosition - surfacePosition, then normalize
        Vector3D L = getPosition().subtract(intersection.getPoint()).normalize(); // L = lightPos - surfacePos

        // Calculate dot product between surface normal and light direction
        // Clamp to 0 so surfaces facing away from light receive no illumination
        return Math.max(intersection.getNormal().dot(L), 0.0);
    }
}