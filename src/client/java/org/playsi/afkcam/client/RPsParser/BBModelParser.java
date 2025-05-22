package org.playsi.afkcam.client.RPsParser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.playsi.afkcam.Afkcam;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.Utils.CameraAnimation;
import org.playsi.afkcam.client.Utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.playsi.afkcam.client.Utils.CameraAnimation.parseFloatValue;

/**
 * Parser for BBModel files to extract camera animations.
 */
public class BBModelParser {
    private static final LogUtils LOGGER = new LogUtils(BBModelParser.class);
    private static final Set<String> ALLOWED_VALUES = Set.of("step", "linear", "catmullrom");
    /**
     * Parse camera animations from a JSON reader.
     *
     * @param reader The reader containing JSON data
     * @return List of parsed camera animations
     */
    public static List<CameraAnimation> parseAnimationsFromJson(BufferedReader reader) {
        List<CameraAnimation> animations = new ArrayList<>();
        JsonElement root = JsonParser.parseReader(reader);

        if (!root.isJsonObject()) {
            LOGGER.errorDebug("Root element is not a JSON object");
            return animations;
        }

        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("animations")) {
            LOGGER.errorDebug("JSON object doesn't have 'animations' property");
            return animations;
        }

        JsonElement animElement = obj.get("animations");
        if (!animElement.isJsonArray()) {
            LOGGER.errorDebug("'animations' is not a JSON array");
            return animations;
        }

        JsonArray animArray = animElement.getAsJsonArray();
        LOGGER.info("Found " + animArray.size() + " animation entries to process");

        // Check for any animation data first
        if (animArray.size() == 0) {
            LOGGER.warnDebug("Animations array is empty!");
            return animations;
        }

        // Try to get the structure of the first animation for debugging
        if (animArray.size() > 0 && animArray.get(0).isJsonObject()) {
            JsonObject firstAnim = animArray.get(0).getAsJsonObject();
            LOGGER.infoDebug("First animation properties: " + firstAnim.keySet());

            // Debug the animators structure if it exists
            if (firstAnim.has("animators")) {
                JsonElement animatorsElem = firstAnim.get("animators");
                if (animatorsElem.isJsonObject()) {
                    JsonObject animatorsObj = animatorsElem.getAsJsonObject();
                    LOGGER.infoDebug("Animators has " + animatorsObj.keySet().size() + " keys");
                    for (String key : animatorsObj.keySet()) {
                        LOGGER.infoDebug(" - Animator key: " + key);
                        JsonElement animator = animatorsObj.get(key);
                        if (animator.isJsonObject()) {
                            JsonObject animatorObj = animator.getAsJsonObject();
                            if (animatorObj.has("name")) {
                                LOGGER.infoDebug("   * Name: " + animatorObj.get("name").getAsString());
                            }
                        }
                    }
                }
            }
        }

        parseAnimationsFromArray(animArray, animations);
        return animations;
    }

    /**
     * Parses animations from a JSON array.
     *
     * @param animArray The JSON array containing animation data
     * @param animations The list to add parsed animations to
     */
    private static void parseAnimationsFromArray(JsonArray animArray, List<CameraAnimation> animations) {
        for (JsonElement animEl : animArray) {
            if (!animEl.isJsonObject()) continue;

            JsonObject animObj = animEl.getAsJsonObject();
            String animationName = animObj.has("name") ? animObj.get("name").getAsString() : "unnamed";

            // Check if animation has animators property
            if (!animObj.has("animators")) {
                LOGGER.errorDebug("Animation missing 'animators' property: " + animationName);
                continue;
            }

            JsonElement animatorsElem = animObj.get("animators");
            if (!animatorsElem.isJsonObject()) {
                LOGGER.errorDebug("'animators' is not a JSON object in animation: " + animationName);
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
                    LOGGER.errorDebug("Animator missing 'name' property in animation: " + animationName);
                    continue;
                }

                String boneName = animatorObj.get("name").getAsString();

                // Only process camera bones
                if (!"camera".equalsIgnoreCase(boneName)) {
                    LOGGER.infoDebug("Skipping non-camera animator: " + boneName);
                    continue;
                }

                LOGGER.infoDebug("Found camera animator in animation: " + animationName);
                foundCameraAnimator = true;

                // Create camera animation object
                CameraAnimation camAnim = new CameraAnimation(animationName);

                // Check for keyframes
                if (!animatorObj.has("keyframes")) {
                    LOGGER.warnDebug("Camera animator missing keyframes in animation: " + animationName);
                    continue;
                }

                JsonElement keysElement = animatorObj.get("keyframes");
                if (!keysElement.isJsonArray()) {
                    LOGGER.warnDebug("Keyframes is not a JSON array in animation: " + animationName);
                    continue;
                }

                JsonArray keys = keysElement.getAsJsonArray();
                parseKeyframes(keys, camAnim, animationName);

                if (camAnim.getPositionKeyframes().isEmpty() && camAnim.getRotationKeyframes().isEmpty()) {
                    LOGGER.warnDebug("Camera animation has no usable keyframes: "+ animationName);
                } else {
                    LOGGER.infoDebug("Added camera animation: " + animationName +
                            " (pos keys: " + camAnim.getPositionKeyframes().size() +
                            ", rot keys: " + camAnim.getRotationKeyframes().size() + ")");
                    animations.add(camAnim);
                }
            }

            if (!foundCameraAnimator) {
                LOGGER.errorDebug("No camera animator found in animation: " + animationName);
            }
        }
    }

    /**
     * Parses keyframes from a JSON array and adds them to the camera animation.
     *
     * @param keys The JSON array containing keyframe data
     * @param camAnim The camera animation to add keyframes to
     * @param animationName The name of the animation (for logging)
     */
    private static void parseKeyframes(JsonArray keys, CameraAnimation camAnim, String animationName) {
        for (JsonElement keyEl : keys) {
            if (!keyEl.isJsonObject()) continue;

            JsonObject keyObj = keyEl.getAsJsonObject();
            float time = keyObj.has("time") ? keyObj.get("time").getAsFloat() : 0F;;
            String channel = keyObj.has("channel") ? keyObj.get("channel").getAsString() : "";
            String interp = parseInterpolation(keyObj);

            if ("position".equals(channel) && keyObj.has("data_points")) {
                JsonElement dataPointsElem = keyObj.get("data_points");
                if (dataPointsElem.isJsonArray() && dataPointsElem.getAsJsonArray().size() > 0) {
                    JsonObject dataPoint = dataPointsElem.getAsJsonArray().get(0).getAsJsonObject();

                    if (dataPoint.has("x") && dataPoint.has("y") && dataPoint.has("z")) {
                        float x = parseFloatValue(dataPoint.get("x"));
                        float y = parseFloatValue(dataPoint.get("y"));
                        float z = parseFloatValue(dataPoint.get("z"));

                        camAnim.addPositionKeyframe(time, x, y, z, interp);
                        LOGGER.infoDebug("Added position keyframe at t=" + time + " pos=(" + x + "," + y + "," + z + ")"+ interp);
                    }
                }
            }

            if ("rotation".equals(channel) && keyObj.has("data_points")) {
                JsonElement dataPointsElem = keyObj.get("data_points");
                if (dataPointsElem.isJsonArray() && dataPointsElem.getAsJsonArray().size() > 0) {
                    JsonObject dataPoint = dataPointsElem.getAsJsonArray().get(0).getAsJsonObject();

                    if (dataPoint.has("x") && dataPoint.has("y") && dataPoint.has("z")) {
                        float x = parseFloatValue(dataPoint.get("x"));
                        float y = parseFloatValue(dataPoint.get("y"));
                        float z = parseFloatValue(dataPoint.get("z"));

                        camAnim.addRotationKeyframe(time, x, y, z, interp);
                        LOGGER.infoDebug("Added rotation keyframe at t=" + time + " rot=(" + x + "," + y + "," + z + ")"+ interp);
                    }
                }
            }
        }
    }

    private static String parseInterpolation(JsonObject keyObj) {
        String defaultInterpolation = "catmullrom";
        String value = null;

        if (keyObj.has("interpolation")) {
            value = keyObj.get("interpolation").getAsString();
            if (ALLOWED_VALUES.contains(value)) {
                return value;
            }
        }

        LOGGER.warnDebug("Недопустимое значение interpolation: " + value + ". Используется значение по умолчанию: " + defaultInterpolation);

        return defaultInterpolation;
    }

}