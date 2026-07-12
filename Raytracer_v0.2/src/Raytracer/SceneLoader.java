package Raytracer;

import Raytracer.Objects.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class SceneLoader {

    public static class LoadedScene {
        public Scene scene;
        public Camera camera;
        public int width, height, aaSamples;
        public String outputFile;
    }

    public static LoadedScene load(String jsonPath) throws Exception {
        JSONObject root = new JSONObject(new JSONTokener(new FileReader(jsonPath)));
        LoadedScene result = new LoadedScene();
        result.scene = new Scene();

        // --- Camera ---
        JSONObject camJson = root.getJSONObject("camera");
        result.camera = new Camera(
                vec3(camJson.getJSONArray("position")),
                vec3(camJson.getJSONArray("lookAt")),
                vec3(camJson.getJSONArray("up")),
                camJson.getDouble("fov"),
                camJson.getDouble("aspectRatio"),
                camJson.getDouble("near"),
                camJson.getDouble("far")
        );

        // --- Materials (named presets, based on a Material.* base) ---
        Map<String, Material> materials = new HashMap<>();
        if (root.has("materials")) {
            JSONObject matsJson = root.getJSONObject("materials");
            for (String name : matsJson.keySet()) {
                JSONObject m = matsJson.getJSONObject(name);
                Material base = resolveBaseMaterial(m.optString("base", "PLASTIC")).copy();
                if (m.has("ambient")) base.setAmbient(m.getDouble("ambient"));
                if (m.has("diffuse")) base.setDiffuse(m.getDouble("diffuse"));
                if (m.has("specular")) base.setSpecular(m.getDouble("specular"));
                if (m.has("shininess")) base.setShininess(m.getDouble("shininess"));
                if (m.has("reflectivity")) base.setReflectivity(m.getDouble("reflectivity"));
                if (m.has("ior")) base.setIOR(m.getDouble("ior"));
                if (m.has("refractive")) base.setRefractive(m.getBoolean("refractive"));
                materials.put(name, base);
            }
        }

        // --- Models ---
        Map<String, BufferedImage> textureCache = new HashMap<>();
        if (root.has("models")) {
            for (Object o : root.getJSONArray("models")) {
                JSONObject mJson = (JSONObject) o;
                BufferedImage texture = null;
                if (mJson.has("texture")) {
                    String texPath = mJson.getString("texture");
                    texture = textureCache.computeIfAbsent(texPath, Raytracer::loadTexture);
                }
                Material mat = materials.get(mJson.getString("material"));
                Model3D model = ObjectReader.loadModel(
                        mJson.getString("path"),
                        vec3(mJson.getJSONArray("position")),
                        vec3(mJson.getJSONArray("scale")),
                        vec3(mJson.getJSONArray("rotation")),
                        color(mJson.getJSONArray("color")),
                        mat,
                        texture
                );
                result.scene.addObject(model);
            }
        }

        // --- Lights ---
        if (root.has("lights")) {
            for (Object o : root.getJSONArray("lights")) {
                JSONObject lJson = (JSONObject) o;
                result.scene.addLight(parseLight(lJson));
            }
        }

        // --- Render settings ---
        JSONObject renderJson = root.getJSONObject("render");
        result.width = renderJson.getInt("width");
        result.height = renderJson.getInt("height");
        result.aaSamples = renderJson.optInt("aaSamples", 1);
        result.outputFile = renderJson.optString("output", "output.png");

        result.scene.buildBVH();
        return result;
    }

    private static Light parseLight(JSONObject lJson) {
        String type = lJson.getString("type");
        Vector3D pos = vec3(lJson.getJSONArray("position"));
        Color col = color(lJson.getJSONArray("color"));
        double intensity = lJson.getDouble("intensity");

        return switch (type) {
            case "spot" -> new SpotLight(
                    pos,
                    vec3(lJson.getJSONArray("direction")),
                    col, intensity,
                    lJson.getDouble("innerAngle"),
                    lJson.getDouble("outerAngle")
            );
            case "point" -> new PointLight(pos, col, intensity);
            case "directional" -> new DirectionalLight(
                    vec3(lJson.getJSONArray("direction")), col, intensity
            );
            default -> throw new IllegalArgumentException("Unknown light type: " + type);
        };
    }

    private static Material resolveBaseMaterial(String name) {
        return switch (name) {
            case "GLASS" -> Material.GLASS;
            case "METAL" -> Material.METAL;
            case "PLASTIC" -> Material.PLASTIC;
            case "RUBBER" -> Material.RUBBER;
            case "SHINY" -> Material.SHINY;
            case "MATTE" -> Material.MATTE;
            case "WATER" -> Material.WATER;
            default -> throw new IllegalArgumentException("Unknown base material: " + name);
        };
    }

    private static Vector3D vec3(JSONArray a) {
        return new Vector3D(a.getDouble(0), a.getDouble(1), a.getDouble(2));
    }

    private static Color color(JSONArray a) {
        return new Color((float) a.getDouble(0), (float) a.getDouble(1), (float) a.getDouble(2));
    }
}