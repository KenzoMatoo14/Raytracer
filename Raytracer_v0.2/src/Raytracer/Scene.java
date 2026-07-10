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
     * Compute color using Blinn-Phong lighting model with ambient, diffuse, and specular components
     * Includes shadow ray testing for realistic shadows
     * @param intersection The intersection point containing surface information
     * @param viewDir Direction vector from intersection point to camera
     * @param near Near clipping plane distance
     * @param far Far clipping plane distance
     * @return Color calculated using Blinn-Phong lighting with shadows
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

        // Initialize color accumulator
        double[] finalColor = new double[3];

        // Calculate lighting contribution from each light source
        for (Light light : lights) {
            Vector3D lightDir;
            Vector3D lightVec = null;
            double distance = Double.POSITIVE_INFINITY;

            // Calculate light direction based on light type
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

            // === Shadow Ray Logic ===
            // Shadow ray offset along the surface normal (not lightDir) for robustness.
            // Using normal avoids self-intersection on thin/small triangles regardless of
            // the angle between the normal and the light. Offset 1e-3 safely clears
            // Triangle.EPSILON (1e-6) even on nearly-degenerate geometry.
            double shadowBias = 1e-3;
            Vector3D shadowOrigin = point.add(normal.multiply(shadowBias));
            Ray shadowRay = new Ray(shadowOrigin, lightDir);
            // Point/spot lights only care about occluders closer than the light itself;
            // directional lights have no far bound beyond the camera's far clip plane.
            // intersectAny stops at the first occluder found instead of searching for the
            // globally closest hit — shadow tests only need "does anything block the path".
            double shadowFar = (light instanceof DirectionalLight) ? far : Math.min(far, distance - 1e-3);
            boolean inShadow = intersectAny(shadowRay, near, shadowFar);

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
                // Ambient component (always present, independent of lighting)
                double ambient = ka * objRGB[i];
                double diffuse = 0;
                double specular = 0;

                // Only calculate diffuse and specular if not in shadow
                if (!inShadow) {
                    // Diffuse component (Lambertian reflection)
                    diffuse = kd * objRGB[i] * lightRGB[i] * nDotL * intensity * attenuation;
                    // Specular component (Blinn-Phong highlights)
                    specular = ks * lightRGB[i] * Math.pow(nDotH, shininess) * intensity * attenuation;
                }

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