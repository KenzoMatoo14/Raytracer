package Raytracer;

import Raytracer.BVH.BVHNode;
import Raytracer.Objects.*;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.lang.Math.clamp;

/**
 * Main raytracer class that handles ray tracing calculations and image rendering.
 * Implements recursive ray tracing with reflection and refraction support.
 */
public class Raytracer {

    private Scene scene;   // The 3D scene containing objects and lights
    private Camera camera; // The camera from which rays are cast

    /**
     * Constructor to create a raytracer with a given scene and camera
     * @param scene The 3D scene to render
     * @param camera The camera viewpoint for rendering
     */
    public Raytracer(Scene scene, Camera camera) {
        this.scene = scene;
        this.camera = camera;
    }

    /**
     * Traces a single ray through the scene and returns the resulting color
     * Implements recursive ray tracing with reflection and refraction
     * @param ray The ray to trace through the scene
     * @param depth Current recursion depth to prevent infinite recursion
     * @return The color computed for this ray
     */
    public Color traceRay(Ray ray, int depth) {
        // Terminate recursion at maximum depth to prevent stack overflow
        if (depth >= 9) return Color.BLACK;

        // Find the closest intersection with scene objects
        Intersection intersection = scene.intersect(ray, camera.getNear(), camera.getFar());

        if (intersection != null) {
            // Extract intersection properties
            Object3D obj = intersection.getObject();
            Vector3D point = intersection.getPoint();
            Vector3D normal = intersection.getNormal().normalize();
            Vector3D incoming = ray.getDirection().normalize();
            Vector3D viewDir = ray.getDirection().negate().normalize(); // Direction towards viewer

            // Compute local illumination using Blinn-Phong shading model
            Color localColor = scene.computeBlinnPhongColor(intersection, viewDir, camera.getNear(), camera.getFar());

            // Convert colors to RGB components for blending calculations
            float[] localRGB = localColor.getRGBColorComponents(null);
            float[] reflectedRGB = new float[]{0, 0, 0};  // Initialize reflection contribution
            float[] refractedRGB = new float[]{0, 0, 0};  // Initialize refraction contribution

            double reflectivity = obj.getReflectivity();
            double fresnel = 0.0; // Fresnel coefficient for reflection/refraction mixing

            // Handle reflection for reflective objects
            if (reflectivity > 0.0) {
                // Compute reflection vector using: R = I - 2*(I·N)*N
                Vector3D reflectedDir = incoming.subtract(normal.multiply(2.0 * incoming.dot(normal))).normalize();
                // Offset origin slightly to prevent self-intersection artifacts (shadow acne)
                Vector3D reflectedOrigin = point.add(reflectedDir.multiply(1e-4));
                Ray reflectedRay = new Ray(reflectedOrigin, reflectedDir);
                // Recursively trace the reflection ray
                Color reflectedColor = traceRay(reflectedRay, depth + 1);

                reflectedRGB = reflectedColor.getRGBColorComponents(null);
            }

            // Handle refraction for transparent objects
            boolean refracted = false;
            if (obj.isRefractive()) {
                double n1 = 1.0; // Refractive index of air
                double n2 = obj.getIOR(); // Refractive index of the material
                boolean exiting = incoming.dot(normal) > 0; // Check if ray is exiting the object

                // Adjust indices and normal for exiting rays
                if (exiting) {
                    normal = normal.negate();
                    double temp = n1;
                    n1 = n2;
                    n2 = temp;
                }

                double eta = n1 / n2; // Ratio of refractive indices
                double cosi = -normal.dot(incoming); // Cosine of incident angle
                double sint2 = eta * eta * (1 - cosi * cosi); // Sin²(transmitted angle)

                // Compute Fresnel reflectance using Schlick's approximation
                double r0 = Math.pow((n1 - n2) / (n1 + n2), 2);
                fresnel = r0 + (1 - r0) * Math.pow(1 - Math.abs(cosi), 5);

                // Check for total internal reflection
                if (sint2 <= 1.0) { // No total internal reflection
                    double cost = Math.sqrt(1.0 - sint2); // Cosine of transmitted angle
                    // Compute refracted direction using Snell's law
                    Vector3D refractedDir = incoming.multiply(eta).add(normal.multiply(eta * cosi - cost)).normalize();
                    // Offset origin to prevent self-intersection
                    Vector3D refractedOrigin = point.add(refractedDir.multiply(1e-4));
                    Ray refractedRay = new Ray(refractedOrigin, refractedDir);
                    // Recursively trace the refracted ray
                    Color refractedColor = traceRay(refractedRay, depth + 1);

                    refractedRGB = refractedColor.getRGBColorComponents(null);
                    refracted = true;
                } else {
                    fresnel = 1.0; // Total internal reflection - all light is reflected
                }
            } else {
                fresnel = reflectivity; // Use object reflectivity as fallback
            }

            // Blend local shading, reflection, and refraction contributions
            // Final color = local * (1 - reflectivity) + reflection * fresnel + refraction * (1 - fresnel)
            float r = (float) clamp(
                    localRGB[0] * (1 - reflectivity) +
                            reflectedRGB[0] * fresnel +
                            refractedRGB[0] * (1.0 - fresnel) * (refracted ? 1.0 : 0.0),
                    0, 1);
            float g = (float) clamp(
                    localRGB[1] * (1 - reflectivity) +
                            reflectedRGB[1] * fresnel +
                            refractedRGB[1] * (1.0 - fresnel) * (refracted ? 1.0 : 0.0),
                    0, 1);
            float b = (float) clamp(
                    localRGB[2] * (1 - reflectivity) +
                            reflectedRGB[2] * fresnel +
                            refractedRGB[2] * (1.0 - fresnel) * (refracted ? 1.0 : 0.0),
                    0, 1);
            return new Color(r, g, b);
        }

        // No intersection found — sample background texture (skybox) or return black
        BufferedImage bg = scene.getBackgroundTexture();
        if (bg != null) {
            return sampleSkybox(bg, ray.getDirection());
        }
        return Color.BLACK;
    }

    /**
     * Samples a spherical skybox texture using equirectangular (lat-long) projection.
     * Maps a direction vector to UV coordinates on the background image.
     * @param skybox The background equirectangular texture
     * @param dir    Normalized ray direction
     * @return Sampled sky color
     */
    private Color sampleSkybox(BufferedImage skybox, Vector3D dir) {
        // Convert direction to spherical coordinates
        double u = 0.5 + Math.atan2(dir.getZ(), dir.getX()) / (2 * Math.PI);
        double v = 0.5 - Math.asin(Math.max(-1, Math.min(1, dir.getY()))) / Math.PI;

        // Convert to pixel coordinates (clamp to valid range)
        int px = (int) Math.min(skybox.getWidth()  - 1, Math.max(0, u * skybox.getWidth()));
        int py = (int) Math.min(skybox.getHeight() - 1, Math.max(0, v * skybox.getHeight()));

        return new Color(skybox.getRGB(px, py));
    }

    /**
     * Traces a ray for a single sample (used by anti-aliasing render loop).
     * Identical to the public traceRay but kept internal for clarity.
     */
    private Color traceSample(double normX, double normY) {
        Ray ray = camera.generateRay(normX, normY);
        return traceRay(ray, 0);
    }

    /**
     * Renders the scene with optional supersampling anti-aliasing.
     * @param width    Output image width in pixels
     * @param height   Output image height in pixels
     * @param filename Output image filename
     * @param aaSamples Anti-aliasing samples per pixel (1 = no AA, 4 = 2x2 grid, 16 = 4x4 grid)
     */
    public void render(int width, int height, String filename, int aaSamples) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        System.out.println("Rendering scene at " + width + "x" + height +
                " with " + aaSamples + "x AA...");

        // Grid side length for stratified sampling (e.g. aaSamples=4 → 2x2 grid)
        int gridSize = (int) Math.sqrt(aaSamples);
        // Recompute aaSamples to be a perfect square
        aaSamples = gridSize * gridSize;

        long rayStart = System.nanoTime();
        int rayCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                double rAccum = 0, gAccum = 0, bAccum = 0;

                if (aaSamples <= 1) {
                    // Fast path — no anti-aliasing
                    double normX = (double) x / (width - 1);
                    double normY = (double) y / (height - 1);
                    Color c = traceSample(normX, normY);
                    rAccum = c.getRed();
                    gAccum = c.getGreen();
                    bAccum = c.getBlue();
                } else {
                    // Stratified (jittered) supersampling
                    for (int sy = 0; sy < gridSize; sy++) {
                        for (int sx = 0; sx < gridSize; sx++) {
                            // Jitter within sub-pixel cell for stratified sampling
                            double jx = (sx + Math.random()) / gridSize;
                            double jy = (sy + Math.random()) / gridSize;
                            double normX = (x + jx - 0.5) / (width  - 1);
                            double normY = (y + jy - 0.5) / (height - 1);
                            Color c = traceSample(normX, normY);
                            rAccum += c.getRed();
                            gAccum += c.getGreen();
                            bAccum += c.getBlue();
                        }
                    }
                    rAccum /= aaSamples;
                    gAccum /= aaSamples;
                    bAccum /= aaSamples;
                }

                // Clamp and set final pixel color
                int r = Math.min(255, Math.max(0, (int) Math.round(rAccum)));
                int g = Math.min(255, Math.max(0, (int) Math.round(gAccum)));
                int b = Math.min(255, Math.max(0, (int) Math.round(bAccum)));
                image.setRGB(x, y, new Color(r, g, b).getRGB());
                rayCount += aaSamples;
            }

            // Progress indicator every 10% of rows
            if (y % Math.max(1, height / 10) == 0) {
                System.out.printf("  Progress: %d%%\n", (int)(100.0 * y / height));
            }
        }

        long rayEnd = System.nanoTime();
        System.out.printf("Render time: %.3f s | Rays traced: %,d\n",
                (rayEnd - rayStart) / 1e9, rayCount);
        System.out.printf("Triangle tests: %,d | Avg per ray: %.1f\n",
                Triangle.triangleTests, (double) Triangle.triangleTests / rayCount);

        try {
            File output = new File(filename);
            ImageIO.write(image, "png", output);
            System.out.println("Saved: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

    /**
     * Convenience overload — renders without anti-aliasing (1 sample per pixel).
     */
    public void render(int width, int height, String filename) {
        render(width, height, filename, 1);
    }

    /**
     * Utility method to load texture images from file
     * @param path File path to the texture image
     * @return BufferedImage containing the texture, or null if loading fails
     */
    public static BufferedImage loadTexture(String path) {
        if (path == null) return null;
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Failed to load texture from " + path + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Main method to run the raytracer application
     * Sets up the scene, camera, objects, lights, and renders the final image
     */
    public static void main(String[] args) {
        long startTotal = System.nanoTime();

        // === SCENE SETUP ===
        long startSceneSetup = System.nanoTime();
        Scene scene = new Scene(); // Create empty scene

        // Define camera parameters for the viewpoint
        Vector3D cameraPos = new Vector3D(0, 0, 0);        // Camera position at origin
        Vector3D lookAt = new Vector3D(0, 0, -1);          // Look down negative Z-axis
        Vector3D up = new Vector3D(0, 1, 0);               // Y-axis as up direction
        double fov = 60;          // Field of view in degrees
        double aspectRatio = 0.5273;   // Width/height ratio (adjust for image dimensions)
        double near = 0.1;        // Near clipping plane (prevents extreme closeness)
        double far = 400;         // Far clipping plane

        // Create camera with specified parameters
        Camera camera = new Camera(cameraPos, lookAt, up, fov, aspectRatio, near, far);

        long endSceneSetup = System.nanoTime();
        System.out.printf("Scene setup time: %.3f s\n", (endSceneSetup - startSceneSetup) / 1e9);

        // === OBJECT LOADING ===
        long startOBJ = System.nanoTime();

        BufferedImage SpiderlilyTexture = Raytracer.loadTexture("src/Raytracer/Textures/Spiderlily.png");
        Model3D Spiderlily = ObjectReader.loadModel(
                "src/Raytracer/Models/Spiderlily.obj", // 3D model file path
                new Vector3D(0, -2, -2),      // Position in world space
                new Vector3D(2, 2, 2),     // Uniform scaling factor
                new Vector3D(0, 0, 0),         // Rotation (X, Y, Z degrees)
                Color.WHITE,  // Base color
                Material.MATTE.copy().setAmbient(0.09).setDiffuse(0.60).setSpecular(0.06).setShininess(8).setReflectivity(0),
                SpiderlilyTexture  // Spiderlily Texture
        );
        scene.addObject(Spiderlily);

        long endOBJ = System.nanoTime();
        System.out.printf("OBJ Loading time: %.3f s\n", (endOBJ - startOBJ) / 1e9);

        // === LIGHTING SETUP ===

        // Luz principal cálida desde arriba-izquierda
        scene.addLight(new SpotLight(
                new Vector3D(-1, 4, -0.5),
                new Vector3D(0.2, -1, -0.4),
                new Color(0.95f, 0.78f, 0.55f),   // ámbar cálido
                8,
                55, 75
        ));

        // Relleno dorado desde abajo muy suave
        scene.addLight(new PointLight(
                new Vector3D(0.5, -2.5, -1.5),
                new Color(0.8f, 0.55f, 0.25f),  // dorado oscuro
                1.2
        ));

        // Rim cálido desde atrás para separar del fondo
        scene.addLight(new PointLight(
                new Vector3D(-1.5, 1, -4),
                new Color(0.9f, 0.65f, 0.3f),   // dorado suave
                1.5
        ));

        // === BVH ACCELERATION STRUCTURE ===
        long startBVH = System.nanoTime();

        // Build Bounding Volume Hierarchy for fast ray-object intersection tests
        scene.buildBVH();

        long endBVH = System.nanoTime();
        System.out.printf("BVH build time: %.3f s\n", (endBVH - startBVH) / 1e9);

        // Display BVH statistics for performance analysis
        if (scene.getBVHRoot() != null) {
            BVHNode root = scene.getBVHRoot();
            System.out.println("BVH depth: " + root.getDepth());
            System.out.println("Number of leaf nodes: " + root.countLeaves());
            System.out.println("Total triangles in BVH: " + root.countTotalObjects());
        }

        // === RENDERING ===
        Raytracer raytracer = new Raytracer(scene, camera);

        // (Optional) Load a skybox equirectangular texture for background:
        // BufferedImage skybox = loadTexture("src/Raytracer/Textures/skybox.hdr");
        // scene.setBackgroundTexture(skybox);

        long startRender = System.nanoTime();
        // aaSamples: 1 = no AA, 4 = 2×2 grid (good quality), 16 = 4×4 grid (best quality)
        raytracer.render(2160, 4096, "output.png", 1);
        long endRender = System.nanoTime();
        System.out.printf("Rendering time: %.3f s\n", (endRender - startRender) / 1e9);

        // Display total execution time
        long endTotal = System.nanoTime();
        System.out.printf("Total execution time: %.3f s\n", (endTotal - startTotal) / 1e9);
    }
}