package org.playsi.afkcam.client.Utils;

import com.google.gson.JsonElement;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds camera animation keyframes.
 */
@Getter
public class CameraAnimation {
    private final String name;
    private final List<Keyframe> positionKeyframes = new ArrayList<>();
    private final List<Keyframe> rotationKeyframes = new ArrayList<>();

    public CameraAnimation(String name) {
        this.name = name;
    }

    /**
     * Adds a position keyframe to the animation.
     *
     * @param time The time of the keyframe
     * @param x The x position
     * @param y The y position
     * @param z The z position
     * @param interp The interpolation method
     */
    public void addPositionKeyframe(float time, float x, float y, float z, String interp) {
        positionKeyframes.add(new Keyframe(time, new float[]{x, y, z}, interp));
    }

    /**
     * Adds a rotation keyframe to the animation.
     *
     * @param time The time of the keyframe
     * @param x The x rotation
     * @param y The y rotation
     * @param z The z rotation
     * @param interp The interpolation method
     */
    public void addRotationKeyframe(float time, float x, float y, float z, String interp) {
        rotationKeyframes.add(new Keyframe(time, new float[]{x, y, z}, interp));
    }

    /**
     * Represents a single keyframe in an animation.
     */
    @Getter
    public static class Keyframe {
        private final float time;
        private final float[] values;
        private final String interpolation;

        public Keyframe(float time, float[] values, String interpolation) {
            this.time = time;
            this.values = values;
            this.interpolation = interpolation;
        }
    }

    public static float parseFloatValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            try {
                if (element.getAsJsonPrimitive().isString()) {
                    return Float.parseFloat(element.getAsString());
                } else {
                    return element.getAsFloat();
                }
            } catch (NumberFormatException e) {
                return 0.0f;
            }
        }
        return 0.0f;
    }
}