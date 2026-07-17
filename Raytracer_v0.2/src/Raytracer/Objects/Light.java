package Raytracer.Objects;

import Raytracer.BVH.BoundingBox;
import Raytracer.Intersection;
import Raytracer.Ray;
import Raytracer.Vector3D;

import java.awt.Color;

/**
 * Abstract base class for all light sources in the raytracer.
 * Extends Object3D but represents non-geometric objects that provide illumination.
 * Concrete light types (DirectionalLight, PointLight, etc.) must extend this class
 * and implement the abstract methods.
 */
public abstract class Light extends Object3D {
    // The brightness/strength of the light source
    private double intensity;

    // Physical size of the light, used for soft shadows. 0 = point light (hard shadows).
    // For PointLight/SpotLight this is a world-space radius (sphere around the position).
    // DirectionalLight interprets this as an angular radius in degrees instead (see its
    // getJitteredDirection()), since a directional light has no meaningful world-space position.
    protected double radius = 0.0;

    /**
     * Constructor for creating a light source.
     * @param Position The position of the light in 3D space (may not be used by all light types)
     * @param color The color of the light
     * @param intensity The brightness/intensity of the light
     */
    public Light(Vector3D Position, Color color, double intensity) {
        // Call parent Object3D constructor with position and color
        super(Position, color);
        // Set the light intensity using the setter for validation
        setIntensity(intensity);
    }

    /**
     * Gets the intensity (brightness) of the light.
     * @return The light's intensity value
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * Sets the intensity (brightness) of the light.
     * @param intensity The new intensity value for the light
     */
    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    /**
     * Gets the physical size of the light used for soft-shadow sampling.
     * @return Radius (0 = hard shadows / point light)
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Sets the physical size of the light used for soft-shadow sampling.
     * @param radius New radius; 0 disables soft shadows for this light
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * Returns a randomly jittered position for this light, sampled within a sphere of
     * `radius` around its actual position. Used by soft-shadow sampling to approximate
     * the light's physical size — each shadow ray aims at a slightly different point,
     * and averaging many of them produces a soft penumbra instead of a hard edge.
     * Returns the exact position unchanged when radius is 0 (hard shadows).
     * @return Jittered light position for one shadow-ray sample
     */
    public Vector3D getJitteredPosition() {
        if (radius <= 0.0) return getPosition();
        return getPosition().add(Vector3D.randomInSphere(radius));
    }

    /**
     * Abstract method that calculates the dot product between surface normal and light direction.
     * This is a key component of lighting calculations (Lambert's cosine law).
     * Different light types implement this differently based on their geometry.
     * @param intersection The intersection point containing surface normal and position
     * @return The N·L dot product value used for diffuse lighting calculations
     */
    public abstract double getNDotL(Intersection intersection);

    /**
     * Lights don't have physical geometry that can be intersected by rays.
     * This method returns a dummy intersection indicating no valid intersection.
     * @param ray The ray to test for intersection (ignored)
     * @return A dummy Intersection with negative t value indicating no intersection
     */
    @Override
    public Intersection intersect(Ray ray) {
        // Return invalid intersection (negative t) since lights aren't geometric objects
        return new Intersection(-1, Vector3D.ZERO(), Vector3D.ZERO(), null);
    }

    /**
     * Lights are not included in the BVH spatial acceleration structure.
     * This method returns a degenerate bounding box since lights don't need spatial partitioning.
     * @return A degenerate BoundingBox (both min and max at origin)
     */
    @Override
    public BoundingBox getBoundingBox() {
        // Lights are not part of the BVH — so this won't be used
        // Return degenerate bounding box since lights don't have physical bounds
        return new BoundingBox(Vector3D.ZERO(), Vector3D.ZERO());
    }
}