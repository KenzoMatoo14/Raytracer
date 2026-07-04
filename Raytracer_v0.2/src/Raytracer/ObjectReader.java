package Raytracer;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.util.List;

import Raytracer.Objects.Model3D;
import Raytracer.Objects.Triangle;

import static Raytracer.Vector3D.rotate;

/**
 * ObjectReader class handles loading and parsing of 3D model files.
 * Primarily supports Wavefront OBJ format with vertex positions, normals,
 * texture coordinates, and smoothing groups for realistic 3D models.
 */
public class ObjectReader {

    /**
     * Reads a Wavefront OBJ file and creates a 3D model with specified transformations.
     * Supports vertices, normals, texture coordinates, faces, and smoothing groups.
     * @param filename Path to the OBJ file to load
     * @param position World position offset for the loaded model
     * @param color Base color to apply to all triangles
     * @param scale Scaling factors for X, Y, Z dimensions
     * @param rotation Rotation angles in degrees for X, Y, Z axes
     * @return Model3D object containing all parsed triangles
     */
    public static Model3D readOBJ(String filename, Vector3D position, Color color, Vector3D scale, Vector3D rotation) {
        // Collections to store parsed OBJ data
        List<Vector3D> vertices = new ArrayList<>();           // Vertex positions (v)
        List<Vector3D> normals = new ArrayList<>();            // Vertex normals (vn)
        List<int[]> faceIndices = new ArrayList<>();           // Face vertex indices (f)
        List<int[]> normalIndices = new ArrayList<>();         // Face normal indices
        Map<Integer, List<Integer>> smoothingGroups = new HashMap<>();  // Smoothing group assignments
        List<Vector3D> texCoords = new ArrayList<>();          // Texture coordinates (vt)
        List<int[]> texCoordIndices = new ArrayList<>();       // Face texture coordinate indices

        // Current smoothing group (0 = no smoothing, >0 = smooth within group)
        int currentSmoothingGroup = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int triangleCounter = 0;

            // Parse each line of the OBJ file
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("v ")) {
                    // Parse vertex position line: "v x y z"
                    String[] tokens = line.split("\\s+");

                    // Apply scaling to vertex coordinates
                    double x = Double.parseDouble(tokens[1]) * scale.getX();
                    double y = Double.parseDouble(tokens[2]) * scale.getY();
                    double z = Double.parseDouble(tokens[3]) * scale.getZ();

                    Vector3D v = new Vector3D(x, y, z);

                    // Apply rotation transformation to vertex
                    v = rotate(v, rotation.getX(), rotation.getY(), rotation.getZ());
                    vertices.add(v);

                } else if (line.startsWith("vn ")) {
                    // Parse vertex normal line: "vn x y z"
                    String[] tokens = line.split("\\s+");

                    // Apply inverse scaling to normal (normals scale inversely)
                    double x = Double.parseDouble(tokens[1]) / scale.getX();
                    double y = Double.parseDouble(tokens[2]) / scale.getY();
                    double z = Double.parseDouble(tokens[3]) / scale.getZ();

                    Vector3D n = new Vector3D(x, y, z);

                    // Apply rotation transformation to normal
                    n = rotate(n, rotation.getX(), rotation.getY(), rotation.getZ());

                    // Store normalized normal vector
                    normals.add(n.normalize());

                } else if (line.startsWith("s ")) {
                    // Parse smoothing group line: "s group_number" or "s off"
                    String[] tokens = line.split("\\s+");

                    // Set smoothing group (0 for "off", positive integer for group)
                    currentSmoothingGroup = tokens[1].equalsIgnoreCase("off") ? 0 : Integer.parseInt(tokens[1]);

                } else if (line.startsWith("f ")) {
                    // Parse face line: "f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ..."
                    String[] params  = line.split("\\s+");

                    // Number of vertices in this face (subtract 1 for "f" token)
                    int vertexCount = params.length - 1;

                    // Arrays to store indices for this face
                    int[] vIndices = new int[vertexCount];  // Vertex indices
                    int[] nIndices = new int[vertexCount];  // Normal indices
                    int[] tIndices = new int[vertexCount];  // Texture coordinate indices

                    // Parse each vertex specification in the face
                    for (int i = 1; i < params.length; i++) {
                        String[] parts = params[i].split("/");

                        // Vertex index (required, OBJ uses 1-based indexing)
                        vIndices[i - 1] = Integer.parseInt(parts[0]) - 1;

                        // Texture coordinate index (optional)
                        tIndices[i - 1] = (parts.length >= 2 && !parts[1].isEmpty()) ? Integer.parseInt(parts[1]) - 1 : -1;

                        // Normal index (optional)
                        nIndices[i - 1] = (parts.length >= 3 && !parts[2].isEmpty()) ? Integer.parseInt(parts[2]) - 1 : -1;
                        if (parts.length >= 3 && !parts[2].isEmpty()) {
                            nIndices[i - 1] = Integer.parseInt(parts[2]) - 1;
                        } else {
                            nIndices[i - 1] = -1; // No normal specified
                        }
                    }

                    // Triangulate polygonal faces using fan method (assumes convex faces)
                    // Converts n-gons into (n-2) triangles by connecting vertex 0 to each edge
                    for (int i = 1; i < vertexCount - 1; i++) {
                        // Create triangle from vertices 0, i, i+1
                        int[] face = { vIndices[0], vIndices[i], vIndices[i + 1] };
                        int[] normalsPerVertex = { nIndices[0], nIndices[i], nIndices[i + 1] };

                        // Store triangle data
                        faceIndices.add(face);
                        normalIndices.add(normalsPerVertex);
                        texCoordIndices.add(new int[]{ tIndices[0], tIndices[i], tIndices[i + 1] });

                        // Assign triangle to current smoothing group
                        smoothingGroups.computeIfAbsent(currentSmoothingGroup, k -> new ArrayList<>())
                                .add(triangleCounter++);
                    }
                } else if (line.startsWith("vt ")) {
                    // Parse texture coordinate line: "vt u v [w]"
                    String[] tokens = line.split("\\s+");
                    double u = Double.parseDouble(tokens[1]);
                    double v = Double.parseDouble(tokens[2]);

                    // Store as Vector3D (w component defaults to 0)
                    texCoords.add(new Vector3D(u, v, 0));
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to read OBJ file: " + e.getMessage());
        }

        // Calculate smoothed normals based on smoothing groups
        Vector3D[] smoothedNormals = calculateSmoothedNormals(vertices, faceIndices, normals, normalIndices, smoothingGroups);

        // Create triangle objects from parsed face data
        List<Triangle> triangles = new ArrayList<>();
        for (int i = 0; i < faceIndices.size(); i++) {
            int[] face = faceIndices.get(i);

            // Get vertex positions for this triangle
            Vector3D v0 = vertices.get(face[0]);
            Vector3D v1 = vertices.get(face[1]);
            Vector3D v2 = vertices.get(face[2]);

            // Get smoothed normals for this triangle
            Vector3D n0 = smoothedNormals[face[0]];
            Vector3D n1 = smoothedNormals[face[1]];
            Vector3D n2 = smoothedNormals[face[2]];

            // Create triangle with vertices, normals, and color
            Triangle tri = new Triangle(v0, v1, v2, n0, n1, n2, color);

            // Add texture coordinates if available
            if (texCoordIndices.size() > i) {
                int[] ti = texCoordIndices.get(i);
                Vector3D[] triangleUVs = new Vector3D[3];

                // Set texture coordinates for each vertex (with bounds checking)
                for (int j = 0; j < 3; j++) {
                    triangleUVs[j] = (ti[j] >= 0 && ti[j] < texCoords.size()) ? texCoords.get(ti[j]) : new Vector3D(0, 0, 0);
                }
                tri.setTextureCoords(triangleUVs);
            }

            triangles.add(tri);
        }

        // Create and return final 3D model
        return new Model3D(position, triangles.toArray(new Triangle[0]), color);
    }

    /**
     * Calculates smoothed vertex normals based on smoothing groups.
     * Vertices in the same smoothing group share averaged normals for smooth shading.
     * @param vertices List of vertex positions
     * @param faceIndices List of face vertex indices
     * @param normals List of explicit vertex normals from OBJ file
     * @param normalIndices List of face normal indices
     * @param smoothingGroups Map of smoothing group IDs to triangle lists
     * @return Array of smoothed normal vectors for each vertex
     */
    private static Vector3D[] calculateSmoothedNormals(List<Vector3D> vertices, List<int[]> faceIndices,
                                                       List<Vector3D> normals, List<int[]> normalIndices,
                                                       Map<Integer, List<Integer>> smoothingGroups) {
        // Initialize result array with zero vectors
        Vector3D[] result = new Vector3D[vertices.size()];
        Arrays.fill(result, new Vector3D(0, 0, 0));
        boolean[] assigned = new boolean[vertices.size()];

        // Process each smoothing group
        for (Map.Entry<Integer, List<Integer>> group : smoothingGroups.entrySet()) {
            // Map to accumulate normals for each vertex in this smoothing group
            Map<Integer, Vector3D> normalSums = new HashMap<>();

            // Process each triangle in this smoothing group
            for (Integer faceIndex : group.getValue()) {
                int[] face = faceIndices.get(faceIndex);
                int[] nIdx = normalIndices.get(faceIndex);
                Vector3D faceNormal = null;

                // Process each vertex of the triangle
                for (int i = 0; i < 3; i++) {
                    int vi = face[i];  // Vertex index
                    Vector3D normal = null;

                    // Use explicit normal if available
                    if (nIdx[i] >= 0 && nIdx[i] < normals.size()) {
                        normal = normals.get(nIdx[i]);
                    } else {
                        // Calculate face normal if no explicit normal available
                        if (faceNormal == null) {
                            Vector3D a = vertices.get(face[0]);
                            Vector3D b = vertices.get(face[1]);
                            Vector3D c = vertices.get(face[2]);

                            // Face normal = (b-a) × (c-a)
                            faceNormal = b.subtract(a).cross(c.subtract(a)).normalizeSafe();
                        }
                        normal = faceNormal;
                    }

                    // Accumulate normal for this vertex (merge with existing)
                    normalSums.merge(vi, normal, Vector3D::add);
                }
            }

            // Normalize and assign accumulated normals for this smoothing group
            for (Map.Entry<Integer, Vector3D> entry : normalSums.entrySet()) {
                Vector3D n = entry.getValue().normalizeSafe();
                result[entry.getKey()] = n;
                assigned[entry.getKey()] = true;
            }
        }

        // Provide default normal for vertices not assigned to any smoothing group
        for (int i = 0; i < result.length; i++) {
            if (!assigned[i]) {
                result[i] = new Vector3D(0, 1, 0);  // Default up vector
            }
        }

        return result;
    }

    /**
     * Convenience method to load a complete 3D model with material and texture.
     * Combines OBJ loading with material assignment and texture application.
     * @param path File path to the OBJ model
     * @param position World position for the model
     * @param scale Scaling factors for model dimensions
     * @param rotation Rotation angles in degrees
     * @param color Base color for the model
     * @param material Material properties for lighting
     * @param texture Optional texture image (null if no texture)
     * @return Complete Model3D with material and texture applied
     */
    public static Model3D loadModel(String path, Vector3D position, Vector3D scale, Vector3D rotation, Color color, Material material, BufferedImage texture) {
        // Load the basic model from OBJ file
        Model3D model = readOBJ(path, position, color, scale, rotation);

        // Apply material properties to the model
        model.setMaterial(material);
        model.setReflectivity(material.reflectivity);
        model.setIOR(material.ior);
        model.setRefractive(material.refractive);

        // Apply texture to all triangles if provided
        if (texture != null) {
            for (Triangle tri : model.getTriangles()) {
                tri.setTexture(texture);
            }
        }

        return model;
    }
}