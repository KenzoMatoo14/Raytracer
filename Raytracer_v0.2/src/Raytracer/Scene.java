package Raytracer;

import Raytracer.BVH.BVHNode;
import Raytracer.Objects.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.clamp;

/**
 * Scene class represents a 3D scene containing objects, lights, and optional background texture.
 * Handles ray-object intersection testing, lighting calculations, and BVH acceleration structure.
 */
public class Scene {
    // Number of shadow-ray samples used per light when that light has radius > 0 (soft shadows).
    // Lights with radius == 0 (the default) still use exactly one hard shadow ray — no cost
    // change for scenes that don't opt into soft shadows.
    private int softShadowSamples = 8;

    // === Ambient Occlusion settings ===
    // Number of AO rays per intersection point. 0 disables AO entirely (ambient term behaves
    // exactly as before) — no cost change for scenes that don't opt in.
    private int aoSamples = 0;
    // Max distance an AO ray searches for occluders. Should roughly match the scale of the
    // geometric detail you want AO to react to (gaps between scales, contact with the floor).
    private double aoRadius = 1.0;
    // How strongly occlusion darkens the ambient term, 0..1. 1.0 = full physical darkening,
    // lower values keep some ambient light even in fully-occluded spots (softer look).
    private double aoStrength = 1.0;

    private List<Object3D> objects; // List of objects in the scene
    private List<Light> lights; // List of light sources in the scene
    private BVHNode bvhRoot = null; // Optional BVH (Bounding Volume Hierarchy) root for acceleration
    private BufferedImage backgroundTexture = null; // Optional background texture/skybox

    /**
     * Constructor - initializes empty lists for objects and lights
     */
    public Scene() {
        this.objects = new ArrayList<>();
        this.lights = new ArrayList<>();
    }

    /**
     * Add an object to the scene
     * @param object The 3D object to add to the scene
     */
    public void addObject(Object3D object) {
        objects.add(object);
    }

    /**
     * Get all objects in the scene
     * @return List of all Object3D instances in the scene
     */
    public List<Object3D> getObjects() {
        return objects;
    }

    /**
     * Add a light source to the scene
     * @param light The light source to add
     */
    public void addLight(Light light) {
        lights.add(light);
    }

    /**
     * Get all lights in the scene
     * @return List of all Light instances in the scene
     */
    public List<Light> getLights() {
        return lights;
    }

    /**
     * Set the background texture for the scene
     * @param texture BufferedImage to use as background/skybox
     */
    public void setBackgroundTexture(BufferedImage texture) {
        this.backgroundTexture = texture;
    }

    /**
     * Get the background texture of the scene
     * @return The background texture as BufferedImage, or null if none set
     */
    public BufferedImage getBackgroundTexture() {
        return this.backgroundTexture;
    }

    /**
     * Sets the number of shadow ray samples used for soft shadows on lights with radius > 0.
     * @param n Sample count (minimum 1)
     */
    public void setSoftShadowSamples(int n) {
        this.softShadowSamples = Math.max(1, n);
    }

    /**
     * Sets the number of ambient occlusion rays per intersection point.
     * @param n Sample count; 0 disables AO
     */
    public void setAoSamples(int n) {
        this.aoSamples = Math.max(0, n);
    }

    /**
     * Sets the maximum distance an AO ray searches for occluders.
     * @param radius AO search radius
     */
    public void setAoRadius(double radius) {
        this.aoRadius = radius;
    }

    /**
     * Sets how strongly occlusion darkens the ambient term.
     * @param strength Strength in [0,1]; 1.0 = full physical darkening
     */
    public void setAoStrength(double strength) {
        this.aoStrength = clamp(strength, 0.0, 1.0);
    }

    /**
     * Build BVH (Bounding Volume Hierarchy) from added objects for faster ray intersection
     * Should be called once after all objects are added to the scene
     * Expands Model3D objects into individual triangles and creates BVH structure
     */
    public void buildBVH() {
        // Create list to hold all primitive objects (triangles and basic shapes)
        List<Object3D> primitives = new ArrayList<>();

        // Iterate through all objects in the scene
        for (Object3D obj : objects) {
            // If object is a 3D model, extract individual triangles
            if (obj instanceof Model3D) {
                Model3D model = (Model3D) obj;
                for (Triangle tri : model.getTriangles()) {
                    tri.setMaterial(model.getMaterial()); // Inherit material from parent model
                    // Also inherit texture — without this, textures are lost when BVH is used
                    if (tri.getTexture() == null && model.getTexture() != null) {
                        tri.setTexture(model.getTexture());
                    }
                    primitives.add(tri);
                }
            } else {
                // For basic shapes, add directly as primitive
                primitives.add(obj);
            }
        }

        // Build BVH tree from all primitives
        this.bvhRoot = new BVHNode(primitives);
    }

    /**
     * Get the root node of the BVH tree
     * @return BVHNode root, or null if BVH hasn't been built
     */
    public BVHNode getBVHRoot() {
        return bvhRoot;
    }

    /**
     * Compute color using basic Phong lighting model
     * @param intersection The intersection point containing object and surface information
     * @return Color calculated using Phong lighting
     */
    public Color computePhongColor(Intersection intersection) {
        // Get the intersected object and its base color
        Object3D obj = intersection.getObject();
        Color objColor = obj.getColor();

        // Convert object color from [0,255] to [0,1] range for calculations
        double[] objColors = new double[]{
                objColor.getRed() / 255.0,
                objColor.getGreen() / 255.0,
                objColor.getBlue() / 255.0
        };

        // Initialize final color accumulator
        double[] finalColor = new double[3];

        // Calculate lighting contribution from each light source
        for (Light light : lights) {
            // Calculate dot product between surface normal and light direction
            double nDotL = light.getNDotL(intersection);
            // Calculate light intensity based on angle
            double intensity = light.getIntensity() * nDotL;

            // Convert light color from [0,255] to [0,1] range
            Color lightColor = light.getColor();
            double[] lightColors = new double[]{
                    lightColor.getRed() / 255.0,
                    lightColor.getGreen() / 255.0,
                    lightColor.getBlue() / 255.0
            };

            // Accumulate color for each RGB component (object color + light color * intensity)
            for (int i = 0; i < 3; i++) {
                finalColor[i] += objColors[i] + lightColors[i] * intensity;
            }
        }

        // Clamp final color values to [0,1] range and return as Color object
        return new Color(
                (float) clamp(finalColor[0], 0.0, 1.0),
                (float) clamp(finalColor[1], 0.0, 1.0),
                (float) clamp(finalColor[2], 0.0, 1.0)
        );
    }

    /**
     * Computes an ambient occlusion factor in [0,1] for a surface point by casting cosine-
     * weighted rays into the hemisphere around its normal and checking how many are blocked
     * by nearby geometry within aoRadius. 1.0 means fully exposed (no darkening), lower values
     * mean the point is nestled against other geometry (crevices, contact points) and should
     * receive less ambient light.
     * @param point Surface point being shaded
     * @param normal Surface normal at that point (normalized)
     * @return Occlusion factor to multiply into the ambient term
     */
    private double computeAO(Vector3D point, Vector3D normal) {
        if (aoSamples <= 0) return 1.0;

        // Same scale-relative bias reasoning as shadow rays — keeps AO rays from
        // self-intersecting the surface they're cast from.
        double bias = Math.max(1e-3, point.magnitude() * 1e-4);
        Vector3D origin = point.add(normal.multiply(bias));

        int occluded = 0;
        for (int i = 0; i < aoSamples; i++) {
            Vector3D dir = Vector3D.randomCosineHemisphere(normal);
            Ray aoRay = new Ray(origin, dir);
            if (intersectAny(aoRay, 0.0, aoRadius)) occluded++;
        }

        double occlusion = (double) occluded / aoSamples;
        return 1.0 - occlusion * aoStrength;
    }

    /**
     * Compute color using Blinn-Phong lighting model with ambient, diffuse, and specular components
     * Includes soft shadow ray testing and ambient occlusion for realistic contact shadowing.
     * @param intersection The intersection point containing surface information
     * @param viewDir Direction vector from intersection point to camera
     * @param near Near clipping plane distance
     * @param far Far clipping plane distance
     * @return Color calculated using Blinn-Phong lighting with soft shadows and AO
     */
    public Color computeBlinnPhongColor(Intersection intersection, Vector3D viewDir, double near, double far) {
        // Extract surface properties from intersection
        Object3D obj = intersection.getObject();
        Vector3D point = intersection.getPoint();
        Vector3D normal = intersection.getNormal(); // already normalized in Intersection's constructor
        double attenuation = 1.0; // Light attenuation factor

        // Get object color and convert to float array
        Color objColor = intersection.getColor();
        float[] objRGB = objColor.getRGBColorComponents(null);

        // Get material properties from object
        double ka = obj.getAmbientCoeff();    // Ambient reflection coefficient
        double kd = obj.getDiffuseCoeff();    // Diffuse reflection coefficient
        double ks = obj.getSpecularCoeff();   // Specular reflection coefficient
        double shininess = obj.getShininess(); // Specular shininess exponent

        // Ambient occlusion is a property of the point's geometric surroundings, independent
        // of any single light, so it's computed once per intersection rather than per light.
        double aoFactor = computeAO(point, normal);

        // Initialize color accumulator
        double[] finalColor = new double[3];

        // Calculate lighting contribution from each light source
        for (Light light : lights) {
            Vector3D lightDir;
            Vector3D lightVec = null;
            double distance = Double.POSITIVE_INFINITY;

            // Calculate light direction based on light type
            // NOTE: this uses the light's exact (non-jittered) position/direction — only the
            // shadow test below samples jittered positions. Keeping shading direction fixed
            // stops the specular highlight from jumping around between soft-shadow samples.
            if (light instanceof DirectionalLight) {
                // For directional lights, use negated direction (light rays point towards surface)
                lightDir = ((DirectionalLight) light).getDirection().negate().normalize();
            } else {
                // For point lights, calculate direction from surface to light
                lightVec = light.getPosition().subtract(point);
                distance = lightVec.magnitude();
                lightDir = lightVec.normalize();
                // Apply distance-based attenuation
                attenuation = 1.0 / distance;
            }

            // === Shadow Ray Logic (soft shadows via light-radius sampling) ===
            // Shadow ray offset along the surface normal (not lightDir) for robustness.
            // Using normal avoids self-intersection on thin/small triangles regardless of
            // the angle between the normal and the light. Bias is scale-relative so it holds
            // up on models with sub-unit vertex coordinates.
            double shadowBias = Math.max(1e-3, point.magnitude() * 1e-4);
            Vector3D shadowOrigin = point.add(normal.multiply(shadowBias));

            // radius == 0 (the default) collapses this to exactly one hard shadow ray at the
            // light's true position — identical cost and result to the pre-soft-shadow code.
            int shadowSamples = (light.getRadius() > 0) ? softShadowSamples : 1;
            int occludedCount = 0;

            for (int s = 0; s < shadowSamples; s++) {
                Vector3D sampleDir;
                double sampleFar;

                if (light instanceof DirectionalLight) {
                    // DirectionalLight interprets radius as an angular radius (degrees) and
                    // jitters the incoming direction within a small cone instead of a position.
                    Vector3D jitteredDir = ((DirectionalLight) light).getJitteredDirection();
                    sampleDir = jitteredDir.negate().normalize();
                    sampleFar = far;
                } else {
                    // PointLight/SpotLight: jitter within a sphere of world-space radius around
                    // the light's actual position.
                    Vector3D jitteredPos = light.getJitteredPosition();
                    Vector3D lv = jitteredPos.subtract(point);
                    double dist = lv.magnitude();
                    sampleDir = lv.normalize();
                    sampleFar = Math.min(far, dist - 1e-3);
                }

                Ray shadowRay = new Ray(shadowOrigin, sampleDir);
                // intersectAny stops at the first occluder found instead of searching for the
                // globally closest hit — shadow tests only need "does anything block the path".
                if (intersectAny(shadowRay, near, sampleFar)) occludedCount++;
            }

            // Continuous factor instead of a binary inShadow: 1.0 = fully lit, 0.0 = fully
            // occluded, anything in between = penumbra. With shadowSamples == 1 this is still
            // exactly 0.0 or 1.0 — hard shadows, unchanged from before.
            double shadowFactor = 1.0 - (double) occludedCount / shadowSamples;

            // Calculate half-vector for Blinn-Phong specular reflection
            Vector3D halfVector = lightDir.add(viewDir).normalize();

            // Calculate dot products for lighting equations
            // Double-sided lighting: use abs(nDotL) so back-facing triangles (petals,
            // leaves, thin geometry) still receive light instead of going black.
            // Standard for foliage/flower renderers.
            double nDotL = Math.abs(normal.dot(lightDir));           // Double-sided diffuse
            double nDotH = Math.max(0, normal.dot(halfVector));      // Specular stays one-sided
            double intensity = light.getIntensity();

            // Get light color components
            float[] lightRGB = light.getColor().getRGBColorComponents(null);

            // Calculate lighting components for each RGB channel
            for (int i = 0; i < 3; i++) {
                // Ambient component, darkened by ambient occlusion (contact points, crevices)
                double ambient = ka * objRGB[i] * aoFactor;

                // Diffuse component (Lambertian reflection), scaled by the soft shadow factor
                double diffuse = kd * objRGB[i] * lightRGB[i] * nDotL * intensity * attenuation * shadowFactor;
                // Specular component (Blinn-Phong highlights), scaled by the soft shadow factor
                double specular = ks * lightRGB[i] * Math.pow(nDotH, shininess) * intensity * attenuation * shadowFactor;

                // Accumulate all lighting components
                finalColor[i] += ambient + diffuse + specular;
            }
        }

        // Clamp final color values to [0,1] range and return as Color object
        return new Color(
                (float) clamp(finalColor[0], 0.0, 1.0),
                (float) clamp(finalColor[1], 0.0, 1.0),
                (float) clamp(finalColor[2], 0.0, 1.0)
        );
    }

    /**
     * Find the closest intersection between a ray and objects in the scene
     * Uses BVH acceleration if available, otherwise brute force tests all objects
     * @param ray The ray to test for intersections
     * @param near Near clipping plane distance - intersections closer than this are ignored
     * @param far Far clipping plane distance - intersections farther than this are ignored
     * @return Closest valid intersection, or null if no intersection found
     */
    public Intersection intersect(Ray ray, double near, double far) {
        // Use BVH acceleration if available
        if (bvhRoot != null) {
            return bvhRoot.intersect(ray, near, far);
        }

        // Brute force intersection testing
        Intersection closestIntersection = null;
        double minDistance = Double.POSITIVE_INFINITY;

        // Test ray against each object in the scene
        for (Object3D object : objects) {
            Intersection intersection = object.intersect(ray);
            if (intersection != null) {
                double t = intersection.getT(); // Distance along ray to intersection
                // Check if intersection is within clipping planes and closer than current best
                if (t >= near && t <= far && t < minDistance) {
                    minDistance = t;
                    closestIntersection = intersection;
                }
            }
        }

        return closestIntersection; // Returns null if no intersection is found
    }

    /**
     * Tests whether the scene occludes a ray within [near, far], without computing the closest
     * hit. Used for shadow rays.
     */
    public boolean intersectAny(Ray ray, double near, double far) {
        if (bvhRoot != null) {
            return bvhRoot.intersectAny(ray, near, far);
        }

        for (Object3D object : objects) {
            Intersection intersection = object.intersect(ray);
            if (intersection != null) {
                double t = intersection.getT();
                if (t >= near && t <= far) return true;
            }
        }
        return false;
    }

}