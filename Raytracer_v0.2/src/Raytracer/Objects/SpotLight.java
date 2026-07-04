package Raytracer.Objects;

import Raytracer.Intersection;
import Raytracer.Vector3D;

import java.awt.*;

/**
 * SpotLight represents a cone-shaped light source that emits light in a focused direction.
 * Like a stage light or flashlight: full intensity inside the inner cone (hotspot),
 * smooth falloff between the inner and outer cone, and zero light outside the outer cone.
 *
 * Uses quadratic (inverse-square) distance attenuation for physically-based falloff.
 */
public class SpotLight extends Light {

    // Direction the spotlight is pointing (normalized)
    private Vector3D direction;

    // Inner cone half-angle in radians (full intensity inside this cone)
    private double innerCutoffCos;

    // Outer cone half-angle in radians (zero intensity outside this cone)
    private double outerCutoffCos;

    /**
     * Creates a SpotLight.
     * @param position       World position of the spotlight
     * @param direction      Direction the spotlight is pointing (will be normalized)
     * @param color          Color of emitted light
     * @param intensity      Brightness of the light
     * @param innerAngleDeg  Half-angle of the inner (full-intensity) cone in degrees
     * @param outerAngleDeg  Half-angle of the outer (zero-intensity) cone in degrees
     */
    public SpotLight(Vector3D position, Vector3D direction, Color color, double intensity,
                     double innerAngleDeg, double outerAngleDeg) {
        super(position, color, intensity);
        this.direction = direction.normalize();
        this.innerCutoffCos = Math.cos(Math.toRadians(innerAngleDeg));
        this.outerCutoffCos = Math.cos(Math.toRadians(outerAngleDeg));
    }

    /**
     * Gets the direction the spotlight is pointing.
     * @return Normalized direction vector
     */
    public Vector3D getDirection() {
        return direction;
    }

    /**
     * Sets the spotlight direction.
     * @param direction New direction vector (will be normalized)
     */
    public void setDirection(Vector3D direction) {
        this.direction = direction.normalize();
    }

    /**
     * Calculates the spotlight cone attenuation factor for a given light direction.
     * Returns 1.0 inside the inner cone, smooth falloff between inner and outer cones,
     * and 0.0 outside the outer cone.
     *
     * @param lightDirFromSurface Direction from the surface point TOWARD the light (normalized)
     * @return Spot attenuation factor [0.0, 1.0]
     */
    public double getSpotFactor(Vector3D lightDirFromSurface) {
        // Dot product between the spotlight's direction and the vector from light to surface.
        // We negate lightDirFromSurface because 'direction' points FROM the light,
        // while lightDirFromSurface points TOWARD the light.
        double cosAngle = direction.dot(lightDirFromSurface.negate());

        // Outside outer cone → no contribution
        if (cosAngle < outerCutoffCos) return 0.0;

        // Inside inner cone → full contribution
        if (cosAngle >= innerCutoffCos) return 1.0;

        // Smooth (cubic) falloff between outer and inner cones (Hermite interpolation)
        double t = (cosAngle - outerCutoffCos) / (innerCutoffCos - outerCutoffCos);
        return t * t * (3.0 - 2.0 * t); // Smoothstep
    }

    /**
     * Calculates N·L for the Phong/Blinn-Phong lighting model.
     * The light direction is from the surface point toward the light position.
     * @param intersection Surface intersection data
     * @return Clamped dot product between surface normal and light direction
     */
    @Override
    public double getNDotL(Intersection intersection) {
        Vector3D L = getPosition().subtract(intersection.getPoint()).normalize();
        return Math.max(0.0, intersection.getNormal().dot(L));
    }
}