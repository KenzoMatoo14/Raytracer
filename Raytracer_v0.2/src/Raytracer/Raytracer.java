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
            Vector3D normal = intersection.getNormal(); // already normalized in Intersection's constructor
            Vector3D incoming = ray.getDirection(); // Ray's constructor already normalizes this
            Vector3D viewDir = ray.getDirection().negate(); // negating a unit vector is still unit length

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
     * Parallelized at the pixel level (not row level) so the work-stealing scheduler can balance
     * uneven per-pixel cost (e.g. rows that cross a lot of reflective/refractive geometry cost far
     * more than empty background rows) across threads more evenly than row-granularity chunks.
     * Writes directly into the BufferedImage's backing int[] array instead of calling setRGB per
     * pixel, avoiding per-call color-model conversion overhead.
     * @param width    Output image width in pixels
     * @param height   Output image height in pixels
     * @param filename Output image filename
     * @param aaSamples Anti-aliasing samples per pixel (1 = no AA, 4 = 2x2 grid, 16 = 4x4 grid)
     */
    public void render(int width, int height, String filename, int aaSamples) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Direct handle to the image's backing pixel array (TYPE_INT_RGB → int[] of packed RGB).
        // Writing here avoids the per-pixel overhead of BufferedImage.setRGB's color model lookup.
        int[] imgData = ((java.awt.image.DataBufferInt) image.getRaster().getDataBuffer()).getData();

        System.out.println("Rendering scene at " + width + "x" + height +
                " with " + aaSamples + "x AA...");

        // Grid side length for stratified sampling (e.g. aaSamples=4 → 2x2 grid)
        int gridSize = (int) Math.sqrt(aaSamples);
        // Recompute aaSamples to be a perfect square
        aaSamples = gridSize * gridSize;
        final int finalAaSamples = aaSamples;
        final int finalGridSize = gridSize;

        long rayStart = System.nanoTime();

        // Progress reporting needs a thread-safe counter since pixels complete out of order
        // under parallel execution.
        java.util.concurrent.atomic.AtomicInteger pixelsDone = new java.util.concurrent.atomic.AtomicInteger(0);
        long totalPixels = (long) width * height;
        long progressStep = Math.max(1, totalPixels / 10);

        // Flatten to a single pixel-index range instead of one task per row. With ~8.8M
        // individual units of work instead of `height` row-sized chunks, the parallel scheduler
        // has far more granularity to balance uneven per-pixel cost across threads.
        java.util.stream.IntStream.range(0, (int) totalPixels).parallel().forEach(idx -> {
            int x = idx % width;
            int y = idx / width;

            double rAccum = 0, gAccum = 0, bAccum = 0;

            if (finalAaSamples <= 1) {
                double normX = (double) x / (width - 1);
                double normY = (double) y / (height - 1);
                Color c = traceSample(normX, normY);
                rAccum = c.getRed();
                gAccum = c.getGreen();
                bAccum = c.getBlue();
            } else {
                for (int sy = 0; sy < finalGridSize; sy++) {
                    for (int sx = 0; sx < finalGridSize; sx++) {
                        double jx = (sx + Math.random()) / finalGridSize;
                        double jy = (sy + Math.random()) / finalGridSize;
                        double normX = (x + jx - 0.5) / (width - 1);
                        double normY = (y + jy - 0.5) / (height - 1);
                        Color c = traceSample(normX, normY);
                        rAccum += c.getRed();
                        gAccum += c.getGreen();
                        bAccum += c.getBlue();
                    }
                }
                rAccum /= finalAaSamples;
                gAccum /= finalAaSamples;
                bAccum /= finalAaSamples;
            }

            int r = Math.min(255, Math.max(0, (int) Math.round(rAccum)));
            int g = Math.min(255, Math.max(0, (int) Math.round(gAccum)));
            int b = Math.min(255, Math.max(0, (int) Math.round(bAccum)));

            // Pack into 0xRRGGBB and write directly to the backing array. Each thread only ever
            // writes to its own pixel index, so this is safe without synchronization.
            imgData[idx] = (r << 16) | (g << 8) | b;

            int done = pixelsDone.incrementAndGet();
            if (done % progressStep == 0) {
                System.out.printf("  Progress: %d%%\n", (int) (100.0 * done / totalPixels));
            }
        });

        long rayEnd = System.nanoTime();
        long rayCount = totalPixels * finalAaSamples;
        System.out.printf("Render time: %.3f s | Rays traced: %,d\n",
                (rayEnd - rayStart) / 1e9, rayCount);
        System.out.printf("Triangle tests: %,d | Avg per ray: %.1f\n",
                Triangle.triangleTests.sum(), (double) Triangle.triangleTests.sum() / rayCount);
        System.out.printf("Box tests: %,d | Avg box tests per ray: %.2f\n",
                BVHNode.boxTests.sum(), (double) BVHNode.boxTests.sum() / rayCount);

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

        BufferedImage OrchidTexture = Raytracer.loadTexture("src/Raytracer/Textures/Orchid.jpg");
        Model3D Orchid = ObjectReader.loadModel("src/Raytracer/Models/Orchid.obj", // 3D model file path
                new Vector3D(-0.4, -1.3, -2),      // Position in world space
                new Vector3D(0.1, 0.1, 0.1),     // Uniform scaling factor
                new Vector3D(-30, -45, 0),         // Rotation (X, Y, Z degrees)
                new Color(0.92f, 0.9f, 0.85f),  // Base color
                Material.METAL.copy()
                        .setAmbient(0.03)
                        .setDiffuse(0.4)
                        .setSpecular(0.75)
                        .setShininess(70)
                        .setReflectivity(0.15),
                OrchidTexture  // Orchid Texture
        );
        scene.addObject(Orchid);
        Model3D Orchid2 = ObjectReader.loadModel("src/Raytracer/Models/Orchid.obj", // 3D model file path
                new Vector3D(0, -0.7, -2),
                new Vector3D(0.1, 0.1, 0.1),
                new Vector3D(-30, 5, -25),
                new Color(0.92f, 0.9f, 0.85f),  // Base color
                Material.METAL.copy()
                        .setAmbient(0.03)
                        .setDiffuse(0.4)
                        .setSpecular(0.75)
                        .setShininess(70)
                        .setReflectivity(0.15),
                OrchidTexture
        );
        scene.addObject(Orchid2);
        Model3D Orchid3 = ObjectReader.loadModel(
                "src/Raytracer/Models/Orchid.obj",
                new Vector3D(-0.3, -0.25, -2.05),      // arriba, centrada tirando a izquierda
                new Vector3D(0.1, 0.1, 0.1),
                new Vector3D(-20, 25, -40),       // continúa la torsión de Orchid2 gradualmente
                new Color(0.92f, 0.9f, 0.85f),  // Base color
                Material.METAL.copy()
                        .setAmbient(0.03)
                        .setDiffuse(0.4)
                        .setSpecular(0.75)
                        .setShininess(70)
                        .setReflectivity(0.15),
                OrchidTexture
        );
        scene.addObject(Orchid3);
        Model3D Orchid4 = ObjectReader.loadModel("src/Raytracer/Models/Orchid.obj",
                new Vector3D(0, 0.35, -2.2),
                new Vector3D(0.1, 0.1, 0.1),
                new Vector3D(-45, 15, 60),
                new Color(0.92f, 0.9f, 0.85f),  // Base color
                Material.METAL.copy()
                        .setAmbient(0.03)
                        .setDiffuse(0.4)
                        .setSpecular(0.75)
                        .setShininess(70)
                        .setReflectivity(0.15),
                OrchidTexture
        );
        scene.addObject(Orchid4);
        Model3D Orchid5 = ObjectReader.loadModel("src/Raytracer/Models/Orchid.obj",
                new Vector3D(0, 0.4, -2.2),
                new Vector3D(0.085, 0.085, 0.085),
                new Vector3D(-10, -20, -25),
                new Color(0.92f, 0.9f, 0.85f),  // Base color
                Material.METAL.copy()
                        .setAmbient(0.03)
                        .setDiffuse(0.4)
                        .setSpecular(0.75)
                        .setShininess(70)
                        .setReflectivity(0.15),
                OrchidTexture
        );
        scene.addObject(Orchid5);

        long endOBJ = System.nanoTime();
        System.out.printf("OBJ Loading time: %.3f s\n", (endOBJ - startOBJ) / 1e9);

        // === LIGHTING SETUP ===

        // Azul dominante — más intensidad, cono más abierto, ligeramente desaturado
        scene.addLight(new SpotLight(
                new Vector3D(-1, 5.5, -0.5),
                new Vector3D(0.3, -1, -0.3),
                new Color(0.3f, 0.25f, 0.9f),
                48,
                22, 38
        ));

        // Rojo desde abajo — más intensidad, cono más abierto, menos saturado
        scene.addLight(new SpotLight(
                new Vector3D(0, -4.5, -0.5),
                new Vector3D(0, 1, -0.5),
                new Color(0.85f, 0.1f, 0.1f),
                14,
                20, 35
        ));

        // Rim rojo desde la derecha — un poco más de intensidad
        scene.addLight(new PointLight(
                new Vector3D(3.5, 1, -2),
                new Color(0.7f, 0.1f, 0.1f),
                4
        ));

        // Fill neutro tenue — levanta las sombras sin quitar el mood bicolor
        scene.addLight(new PointLight(
                new Vector3D(0, 0, 2),
                new Color(0.25f, 0.23f, 0.25f),
                0.5
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
        raytracer.render(2160, 4096, "output.png", 4);
        long endRender = System.nanoTime();
        System.out.printf("Rendering time: %.3f s\n", (endRender - startRender) / 1e9);

        // Display total execution time
        long endTotal = System.nanoTime();
        System.out.printf("Total execution time: %.3f s\n", (endTotal - startTotal) / 1e9);

    }
}