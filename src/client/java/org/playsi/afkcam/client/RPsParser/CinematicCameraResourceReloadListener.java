package org.playsi.afkcam.client.RPsParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.playsi.afkcam.Afkcam;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Reload listener that scans cinematic_camera .bbmodel files and parses camera animations.
 */
@Environment(EnvType.CLIENT)
public class CinematicCameraResourceReloadListener implements SimpleResourceReloadListener<List<CinematicCameraResourceReloadListener.CameraAnimation>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Afkcam.MOD_NAME + " RPsParser");
    private static final Identifier ID = Identifier.of("cinematic:camera_reload");
    private static final String BBE_PATH = "cinematic_camera";
    private static final Config config = AfkcamClient.getConfig();

    @Getter
    private static List<CameraAnimation> cachedAnimations = new ArrayList<>();

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new CinematicCameraResourceReloadListener());
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public CompletableFuture<List<CameraAnimation>> load(ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            List<CameraAnimation> animations = new ArrayList<>();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            LOGGER.info("Checking path: " + BBE_PATH);
            Map<Identifier, Resource> resources = manager.findResources(BBE_PATH, id -> true);
            if (resources.isEmpty()) {
                LOGGER.error(" - NO resources found in " + BBE_PATH);
            } else {
                LOGGER.info(" - Found " + resources.size() + " resources in " + BBE_PATH);
                for (Identifier id : resources.keySet()) {
                    LOGGER.info("   * " + id);
                }
            }

            // Check for .bbmodel files specifically
            LOGGER.info("Looking for .bbmodel files in " + BBE_PATH + ":");
            Map<Identifier, Resource> bbmodelResources = manager.findResources(BBE_PATH, id -> id.getPath().endsWith(".bbmodel"));
            if (bbmodelResources.isEmpty()) {
                LOGGER.error(" - NO .bbmodel files found! This is the main issue.");
                LOGGER.error(" - Make sure .bbmodel files are in the correct location:");
                LOGGER.error("   * assets/<namespace>/cinematic_camera/*.bbmodel");
            } else {
                LOGGER.info(" - Found " + bbmodelResources.size() + " .bbmodel files");

                // Process each .bbmodel file
                bbmodelResources.forEach((id, resource) -> {
                    LOGGER.info("Processing .bbmodel file: " + id);
                    try (InputStream is = resource.getInputStream();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(is, StandardCharsets.UTF_8))) {

                        // Parse the JSON content
                        JsonElement root = JsonParser.parseReader(reader);

                        // Check the animations structure
                        if (root.isJsonObject()) {
                            JsonObject rootObj = root.getAsJsonObject();
                            if (rootObj.has("animations")) {
                                JsonElement animElement = rootObj.get("animations");
                                if (animElement.isJsonArray()) {
                                    JsonArray animArray = animElement.getAsJsonArray();

                                    // Check each animation for "bone": "camera"
                                    boolean foundCameraAnim = false;
                                    for (int i = 0; i < animArray.size(); i++) {
                                        if (!animArray.get(i).isJsonObject()) continue;

                                    }
                                } else {
                                    LOGGER.error("ERROR: 'animations' is not a JSON array!");
                                }
                            } else {
                                LOGGER.error("ERROR: File does not have an 'animations' property!");
                            }
                        } else {
                            LOGGER.error("ERROR: File is not a valid JSON object!");
                        }

                        List<CameraAnimation> parsedAnimations = new ArrayList<>();
                        parseAnimations(root, parsedAnimations);

                        if (parsedAnimations.isEmpty()) {
                            LOGGER.error("No camera animations extracted from file: " + id);
                        } else {
                            LOGGER.info("Successfully found " + parsedAnimations.size() + " camera animations in: " + id);
                            for (CameraAnimation anim : parsedAnimations) {
                                LOGGER.info(" - Animation: " + anim.getName() +
                                        " (pos keys: " + anim.getPositionKeyframes().size() +
                                        ", rot keys: " + anim.getRotationKeyframes().size() + ")");
                            }
                            animations.addAll(parsedAnimations);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to load camera file: " + id, e);
                    }
                });
            }

            LOGGER.info("Total camera animations loaded: " + animations.size());
            return animations;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(List<CameraAnimation> animations, ResourceManager manager, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            cachedAnimations = animations;
            LOGGER.info("Loaded camera animations: " + animations.size());
            if (animations.isEmpty()) {
                LOGGER.warn("No camera animations were loaded! Check your resource files.");
            } else {
                // Print loaded animation names for debugging
                animations.forEach(anim -> LOGGER.info("Loaded animation: " + anim.getName()));
            }
        }, executor);
    }

    private void parseAnimations(JsonElement root, List<CameraAnimation> animations) {
        if (!root.isJsonObject()) {
            LOGGER.error("Root element is not a JSON object");
            return;
        }

        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("animations")) {
            LOGGER.error("JSON object doesn't have 'animations' property");
            return;
        }

        JsonElement animElement = obj.get("animations");
        if (!animElement.isJsonArray()) {
            LOGGER.error("'animations' is not a JSON array");
            return;
        }

        JsonArray animArray = animElement.getAsJsonArray();
        LOGGER.info("Found " + animArray.size() + " animation entries to process");

        // Check for any animation data first
        if (animArray.size() == 0) {
            LOGGER.warn("Animations array is empty!");
            return;
        }

        // Try to get the structure of the first animation for debugging
        if (animArray.size() > 0 && animArray.get(0).isJsonObject()) {
            JsonObject firstAnim = animArray.get(0).getAsJsonObject();
            LOGGER.info("First animation properties: " + firstAnim.keySet());

            // Debug the animators structure if it exists
            if (firstAnim.has("animators")) {
                JsonElement animatorsElem = firstAnim.get("animators");
                if (animatorsElem.isJsonObject()) {
                    JsonObject animatorsObj = animatorsElem.getAsJsonObject();
                    LOGGER.info("Animators has " + animatorsObj.keySet().size() + " keys");
                    for (String key : animatorsObj.keySet()) {
                        LOGGER.info(" - Animator key: " + key);
                        JsonElement animator = animatorsObj.get(key);
                        if (animator.isJsonObject()) {
                            JsonObject animatorObj = animator.getAsJsonObject();
                            if (animatorObj.has("name")) {
                                LOGGER.info("   * Name: " + animatorObj.get("name").getAsString());
                            }
                        }
                    }
                }
            }
        }

        for (JsonElement animEl : animArray) {
            if (!animEl.isJsonObject()) continue;

            JsonObject animObj = animEl.getAsJsonObject();
            String animationName = animObj.has("name") ? animObj.get("name").getAsString() : "unnamed";

            // Check if animation has animators property
            if (!animObj.has("animators")) {
                LOGGER.error("Animation missing 'animators' property: " + animationName);
                continue;
            }

            JsonElement animatorsElem = animObj.get("animators");
            if (!animatorsElem.isJsonObject()) {
                LOGGER.error("'animators' is not a JSON object in animation: " + animationName);
                continue;
            }

            JsonObject animatorsObj = animatorsElem.getAsJsonObject();

            // Flag to track if we found a camera animator
            boolean foundCameraAnimator = false;

            // Process each animator in the animators object
            for (String key : animatorsObj.keySet()) {
                JsonElement animatorElem = animatorsObj.get(key);
                if (!animatorElem.isJsonObject()) continue;

                JsonObject animatorObj = animatorElem.getAsJsonObject();

                if (!animatorObj.has("name")) {
                    LOGGER.error("Animator missing 'name' property in animation: " + animationName);
                    continue;
                }

                String boneName = animatorObj.get("name").getAsString();

                // Only process camera bones
                if (!"camera".equalsIgnoreCase(boneName)) {
                    LOGGER.info("Skipping non-camera animator: " + boneName);
                    continue;
                }

                LOGGER.info("Found camera animator in animation: " + animationName);
                foundCameraAnimator = true;

                // Create camera animation object
                CameraAnimation camAnim = new CameraAnimation(animationName);

                // Check for keyframes
                if (!animatorObj.has("keyframes")) {
                    LOGGER.warn("Camera animator missing keyframes in animation: " + animationName);
                    continue;
                }

                JsonElement keysElement = animatorObj.get("keyframes");
                if (!keysElement.isJsonArray()) {
                    LOGGER.warn("Keyframes is not a JSON array in animation: " + animationName);
                    continue;
                }

                JsonArray keys = keysElement.getAsJsonArray();
                for (JsonElement keyEl : keys) {
                    if (!keyEl.isJsonObject()) continue;

                    JsonObject keyObj = keyEl.getAsJsonObject();
                    float time = keyObj.has("time") ? keyObj.get("time").getAsFloat() : 0F;
                    String interp = keyObj.has("interpolation") ? keyObj.get("interpolation").getAsString() : "linear";
                    String channel = keyObj.has("channel") ? keyObj.get("channel").getAsString() : "";

                    // Position keyframes
                    if ("position".equals(channel) && keyObj.has("data_points")) {
                        JsonElement dataPointsElem = keyObj.get("data_points");
                        if (dataPointsElem.isJsonArray() && dataPointsElem.getAsJsonArray().size() > 0) {
                            JsonObject dataPoint = dataPointsElem.getAsJsonArray().get(0).getAsJsonObject();

                            if (dataPoint.has("x") && dataPoint.has("y") && dataPoint.has("z")) {
                                float x = parseFloatValue(dataPoint.get("x"));
                                float y = parseFloatValue(dataPoint.get("y"));
                                float z = parseFloatValue(dataPoint.get("z"));

                                camAnim.addPositionKeyframe(time, x, y, z, interp);
                                LOGGER.info("Added position keyframe at t=" + time + " pos=(" + x + "," + y + "," + z + ")");
                            }
                        }
                    }

                    // Rotation keyframes
                    if ("rotation".equals(channel) && keyObj.has("data_points")) {
                        JsonElement dataPointsElem = keyObj.get("data_points");
                        if (dataPointsElem.isJsonArray() && dataPointsElem.getAsJsonArray().size() > 0) {
                            JsonObject dataPoint = dataPointsElem.getAsJsonArray().get(0).getAsJsonObject();

                            if (dataPoint.has("x") && dataPoint.has("y") && dataPoint.has("z")) {
                                float x = parseFloatValue(dataPoint.get("x"));
                                float y = parseFloatValue(dataPoint.get("y"));
                                float z = parseFloatValue(dataPoint.get("z"));

                                camAnim.addRotationKeyframe(time, x, y, z, interp);
                                LOGGER.info("Added rotation keyframe at t=" + time + " rot=(" + x + "," + y + "," + z + ")");
                            }
                        }
                    }
                }

                if (camAnim.getPositionKeyframes().isEmpty() && camAnim.getRotationKeyframes().isEmpty()) {
                    LOGGER.warn("Camera animation has no usable keyframes: {}", animationName);
                } else {
                    LOGGER.info("Added camera animation: " + animationName +
                            " (pos keys: " + camAnim.getPositionKeyframes().size() +
                            ", rot keys: " + camAnim.getRotationKeyframes().size() + ")");
                    animations.add(camAnim);
                }
            }

            if (!foundCameraAnimator) {
                LOGGER.error("No camera animator found in animation: {}", animationName);
            }
        }
    }

    // Helper method to parse numeric values (could be strings or numbers in the JSON)
    private float parseFloatValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            try {
                if (element.getAsJsonPrimitive().isString()) {
                    return Float.parseFloat(element.getAsString());
                } else {
                    return element.getAsFloat();
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Failed to parse float value: " + element);
                return 0.0f;
            }
        }
        return 0.0f;
    }

    /**
     * Holds camera animation keyframes.
     */
    @Getter
    public static class CameraAnimation {
        private final String name;
        private final List<Keyframe> positionKeyframes = new ArrayList<>();
        private final List<Keyframe> rotationKeyframes = new ArrayList<>();

        public CameraAnimation(String name) {
            this.name = name;
        }

        public void addPositionKeyframe(float time, float x, float y, float z, String interp) {
            positionKeyframes.add(new Keyframe(time, new float[]{x, y, z}, interp));
        }

        public void addRotationKeyframe(float time, float x, float y, float z, String interp) {
            rotationKeyframes.add(new Keyframe(time, new float[]{x, y, z}, interp));
        }

        public static class Keyframe {
            public final float time;
            public final float[] values;
            public final String interpolation;

            public Keyframe(float time, float[] values, String interpolation) {
                this.time = time;
                this.values = values;
                this.interpolation = interpolation;
            }
        }
    }
}