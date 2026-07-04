package Raytracer;

import java.awt.Color;

/**
 * Material class defines surface properties for 3D objects in ray tracing.
 * Contains coefficients for lighting models (Phong/Blinn-Phong) and
 * optical properties for reflection and refraction effects.
 */
public class Material {
    // Ambient reflection coefficient (0.0 to 1.0) - base illumination level
    public double ambientCoeff;

    // Diffuse reflection coefficient (0.0 to 1.0) - matte surface scattering
    public double diffuseCoeff;

    // Specular reflection coefficient (0.0 to 1.0) - shiny highlight strength
    public double specularCoeff;

    // Shininess exponent - controls tightness of specular highlights (higher = tighter)
    public double shininess;

    // Reflectivity coefficient (0.0 to 1.0) - strength of mirror-like reflections
    public double reflectivity;

    // Index of refraction - ratio of light speed in vacuum to speed in material
    public double ior;

    // Whether the material allows light transmission (glass, water, etc.)
    public boolean refractive;

    /**
     * Constructor creates a material with all lighting and optical properties.
     * @param ambient Ambient reflection coefficient (base lighting)
     * @param diffuse Diffuse reflection coefficient (matte scattering)
     * @param specular Specular reflection coefficient (shiny highlights)
     * @param shininess Shininess exponent (highlight tightness)
     * @param reflectivity Mirror reflection strength
     * @param ior Index of refraction for transparent materials
     * @param refractive Whether material transmits light
     */
    public Material(double ambient, double diffuse, double specular, double shininess, double reflectivity, double ior, boolean refractive) {
        this.ambientCoeff = ambient;
        this.diffuseCoeff = diffuse;
        this.specularCoeff = specular;
        this.shininess = shininess;
        this.reflectivity = reflectivity;
        this.ior = ior;
        this.refractive = refractive;
    }

    /**
     * Creates a deep copy of this material with identical properties.
     * Useful for creating material variations without affecting the original.
     * @return New Material instance with same properties
     */
    public Material copy() {
        return new Material(
                ambientCoeff, diffuseCoeff, specularCoeff, shininess,
                reflectivity, ior, refractive
        );
    }

    /**
     * Sets the reflectivity coefficient and returns this material for chaining.
     * @param r New reflectivity value (0.0 to 1.0)
     * @return This material instance for method chaining
     */
    public Material setReflectivity(double r) {
        this.reflectivity = r;
        return this;
    }

    /**
     * Sets the index of refraction and returns this material for chaining.
     * @param ior New index of refraction value (typically 1.0 to 2.5)
     * @return This material instance for method chaining
     */
    public Material setIOR(double ior) {
        this.ior = ior;
        return this;
    }

    /**
     * Sets whether the material is refractive and returns this material for chaining.
     * @param b True if material transmits light, false if opaque
     * @return This material instance for method chaining
     */
    public Material setRefractive(boolean b) {
        this.refractive = b;
        return this;
    }

    /**
     * Sets the ambient reflection coefficient and returns this material for chaining.
     * @param a New ambient coefficient (0.0 to 1.0)
     * @return This material instance for method chaining
     */
    public Material setAmbient(double a) {
        this.ambientCoeff = a;
        return this;
    }

    /**
     * Sets the diffuse reflection coefficient and returns this material for chaining.
     * @param d New diffuse coefficient (0.0 to 1.0)
     * @return This material instance for method chaining
     */
    public Material setDiffuse(double d) {
        this.diffuseCoeff = d;
        return this;
    }

    /**
     * Sets the specular reflection coefficient and returns this material for chaining.
     * @param s New specular coefficient (0.0 to 1.0)
     * @return This material instance for method chaining
     */
    public Material setSpecular(double s) {
        this.specularCoeff = s;
        return this;
    }

    /**
     * Sets the shininess exponent and returns this material for chaining.
     * @param s New shininess value (typically 1 to 1000+)
     * @return This material instance for method chaining
     */
    public Material setShininess(double s) {
        this.shininess = s;
        return this;
    }

    // Predefined material constants for common surface types

    // Glass material - high transmission, medium reflection, high IOR
    public static final Material GLASS  = new Material(0.2, 0.8, 0.75, 128, 0.6, 1.52, true);
    // Metal material - low diffuse, high specular, some reflection
    public static final Material METAL  = new Material(0.1, 0.3, 0.6, 128, 0.1, 1.0, false);
    // Plastic material - medium diffuse, low specular, minimal reflection
    public static final Material PLASTIC = new Material(0.1, 0.6, 0.3, 32, 0.1, 1.0, false);
    // Rubber material - high diffuse, very low specular, minimal reflection
    public static final Material RUBBER  = new Material(0.2, 0.7, 0.1, 10, 0.05, 1.0, false);
    // Shiny material - medium diffuse, high specular, good reflection
    public static final Material SHINY   = new Material(0.2, 0.3, 0.7, 64, 0.5, 1.0, false);
    // Matte material - high diffuse, very low specular, no reflection
    public static final Material MATTE   = new Material(0.2, 0.7, 0.1, 8, 0.0, 1.0, false);
    // Water material - high transmission, some reflection, water IOR
    public static final Material WATER   = new Material(0.1, 0.8, 0.5, 64, 0.05, 1.33, true);
}