package Raytracer.Objects;

import Raytracer.BVH.BoundingBox;
import Raytracer.Intersection;
import Raytracer.Material;
import Raytracer.Ray;
import Raytracer.Vector3D;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Abstract base class for all 3D objects in the raytracer.
 * This class defines the common properties and methods that all renderable objects share,
 * including position, color, material properties, and texture support.
 * Concrete object types (Sphere, Triangle, Model3D, etc.) extend this class.
 */
public abstract class Object3D {
    // Base color of the object (used when no texture is applied)
    protected Color color;
    // 3D position of the object in world space
    private Vector3D position;
    // Reflectivity coefficient for mirror-like reflections (0.0 = no reflection, 1.0 = perfect mirror)
    protected double reflectivity = 0.0;
    // Index of Refraction for transparent materials (1.0 = air, 1.33 = water, 1.5 = glass)
    private double ior = 1.0; // default (air)
    // Whether this object refracts light (is transparent)
    private boolean refractive = false;

    // Optional texture image for surface detail
    protected BufferedImage texture = null;

    // Material properties for Phong lighting model
    // (ambient, diffuse, specular coefficients, shininess, reflectivity, IOR, refractive flag)
    protected Material material = new Material(0.1, 0.7, 0.2, 32, 0, 0, false);

    /**
     * Constructor for creating a 3D object with position and color.
     * @param position The 3D position of the object in world space
     * @param color The base color of the object
     */
    public Object3D(Vector3D position, Color color) {
        setPosition(position);
        setColor(color);
    }

    /**
     * Sets the texture image for this object.
     * @param texture BufferedImage to use as the object's surface texture
     */
    public void setTexture(BufferedImage texture) {
        this.texture = texture;
    }

    /**
     * Gets the texture image applied to this object.
     * @return The BufferedImage texture, or null if no texture is applied
     */
    public BufferedImage getTexture() {
        return this.texture;
    }

    /**
     * Samples the texture color at given UV coordinates.
     * UV coordinates are texture coordinates where (0,0) is one corner and (1,1) is opposite.
     * @param u Horizontal texture coordinate (0.0 to 1.0)
     * @param v Vertical texture coordinate (0.0 to 1.0)
     * @return Color sampled from texture at UV coordinates, or base color if no texture
     */
    public Color sampleTextureUV(double u, double v) {
        // If no texture is applied, return the base color
        if (texture == null) return color;

        // Get texture dimensions
        int width = texture.getWidth();
        int height = texture.getHeight();

        // Convert UV coordinates to pixel coordinates
        // Clamp to valid pixel range to avoid out-of-bounds access
        int x = Math.min(width - 1, Math.max(0, (int) (u * width)));
        int y = Math.min(height - 1, Math.max(0, (int) ((1.0 - v) * height))); // Y flipped for standard UV mapping

        // Sample the texture at the calculated pixel coordinates
        return new Color(texture.getRGB(x, y));
    }

    /**
     * Gets the base color of the object.
     * @return The Color object representing the base color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the base color of the object.
     * @param color The new Color to set as the base color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Gets the 3D position of the object in world space.
     * @return Vector3D representing the object's position
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Sets the 3D position of the object in world space.
     * @param position Vector3D representing the new position
     */
    public void setPosition(Vector3D position) {
        this.position = position;
    }

    /**
     * Gets the ambient lighting coefficient from the material.
     * Ambient lighting simulates indirect lighting that illuminates all surfaces equally.
     * @return Ambient coefficient (typically 0.0 to 1.0)
     */
    public double getAmbientCoeff() {
        return material != null ? material.ambientCoeff : 0.1;
    }

    /**
     * Gets the diffuse lighting coefficient from the material.
     * Diffuse lighting creates the main surface color based on light direction and surface normal.
     * @return Diffuse coefficient (typically 0.0 to 1.0)
     */
    public double getDiffuseCoeff() {
        return material != null ? material.diffuseCoeff : 0.7;
    }

    /**
     * Gets the specular lighting coefficient from the material.
     * Specular lighting creates bright highlights on shiny surfaces.
     * @return Specular coefficient (typically 0.0 to 1.0)
     */
    public double getSpecularCoeff() {
        return material != null ? material.specularCoeff : 0.2;
    }

    /**
     * Gets the shininess factor from the material.
     * Higher shininess values create smaller, more focused specular highlights.
     * @return Shininess factor (typically 1 to 200+)
     */
    public double getShininess() {
        return material != null ? material.shininess : 32;
    }

    /**
     * Sets the material properties for this object and updates related properties.
     * Material defines how the object responds to lighting.
     * @param material Material object containing lighting coefficients and optical properties
     */
    public void setMaterial(Material material) {
        this.material = material;
        // Update individual properties from the material
        this.setReflectivity(material.reflectivity);
        this.setIOR(material.ior);
        this.setRefractive(material.refractive);
    }

    /**
     * Gets the material object associated with this object.
     * @return Material object containing lighting and optical properties
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Sets the reflectivity coefficient for mirror-like reflections.
     * @param reflectivity Coefficient from 0.0 (no reflection) to 1.0 (perfect mirror)
     */
    public void setReflectivity(double reflectivity) {
        this.reflectivity = reflectivity;
    }

    /**
     * Gets the reflectivity coefficient of this object.
     * @return Reflectivity value (0.0 to 1.0)
     */
    public double getReflectivity() {
        return reflectivity;
    }

    /**
     * Sets whether this object is refractive (transparent).
     * Refractive objects bend light rays as they pass through.
     * @param r true if object should refract light, false otherwise
     */
    public void setRefractive(boolean r) {
        this.refractive = r;
    }

    /**
     * Checks if this object is refractive (transparent).
     * @return true if object refracts light, false otherwise
     */
    public boolean isRefractive() {
        return this.refractive;
    }

    /**
     * Sets the Index of Refraction (IOR) for this object.
     * IOR determines how much light bends when entering or exiting the material.
     * @param ior Index of Refraction (1.0 = vacuum/air, 1.33 = water, 1.5 = glass, 2.4 = diamond)
     */
    public void setIOR(double ior) {
        this.ior = ior;
    }

    /**
     * Gets the Index of Refraction (IOR) of this object.
     * @return The IOR value
     */
    public double getIOR() {
        return this.ior;
    }

    /**
     * Abstract method for ray-object intersection testing.
     * Each concrete object type must implement this method to define its geometry.
     * @param ray The ray to test for intersection with this object
     * @return Intersection object if ray hits the object, null otherwise
     */
    public abstract Intersection intersect(Ray ray);

    /**
     * Abstract method for computing the axis-aligned bounding box of this object.
     * Bounding boxes are used by the BVH for spatial acceleration.
     * @return BoundingBox that completely encloses this object
     */
    public abstract BoundingBox getBoundingBox();
}