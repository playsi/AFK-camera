package net.playsi.Afkcam.client.Animations;

import net.playsi.Afkcam.client.Camera.CameraAnimationManager;
import net.playsi.Afkcam.client.Camera.CameraKeyframe;

import java.util.*;

public class RawToCamConverter {
    private static final float SCALE_FACTOR = 1.0f / 16.0f;

    /**
     * Конвертация ParsedAnimation в формат CameraAnimationManager
     */
    private static List<CameraKeyframe> convertRawToCamAnim(RawAnimation animation) {
        List<CameraKeyframe> convertedResult = new ArrayList<>();

        List<RawAnimation.Keyframe> posFrames = animation.getPositionKeyframes();
        List<RawAnimation.Keyframe> rotFrames = animation.getRotationKeyframes();

        Map<Float, RawAnimation.Keyframe> rotationMap = new HashMap<>();
        for (RawAnimation.Keyframe rotFrame : rotFrames) {
            rotationMap.put(rotFrame.getTime(), rotFrame);
        }

        for (RawAnimation.Keyframe posFrame : posFrames) {
            float time = posFrame.getTime();
            float[] position = posFrame.getValues();
            float[] rotation = findOrInterpolateRotation(rotFrames, time);

            CameraAnimationManager.addKeyframe(
                    time,
                    position[0] * SCALE_FACTOR, position[1] * SCALE_FACTOR, position[2] * SCALE_FACTOR,
                    rotation[1], rotation[0],
                    mapInterpolation(posFrame.getInterpolation())
            );
        }

        for (RawAnimation.Keyframe rotFrame : rotFrames) {
            float time = rotFrame.getTime();
            if (posFrames.stream().noneMatch(pf -> Math.abs(pf.getTime() - time) < 0.001f)) {
                float[] position = findOrInterpolatePosition(posFrames, time);
                float[] rotation = rotFrame.getValues();

                CameraAnimationManager.addKeyframe(
                        time,
                        position[0] * SCALE_FACTOR, position[1] * SCALE_FACTOR, position[2] * SCALE_FACTOR,
                        rotation[1], rotation[0], // yaw, pitch
                        mapInterpolation(rotFrame.getInterpolation())
                );
            }
        }
    }

    private static CameraKeyframe.InterpolationType mapInterpolation(String interp) {
        if (interp == null) return CameraKeyframe.InterpolationType.LINEAR;
        return switch (interp.toLowerCase(Locale.ROOT)) {
            case "step" -> CameraKeyframe.InterpolationType.STEP;
            case "catmullrom" -> CameraKeyframe.InterpolationType.CATMULLROM;
            default -> CameraKeyframe.InterpolationType.LINEAR;
        };
    }

    private static float[] findOrInterpolateRotation(List<RawAnimation.Keyframe> rotFrames, float time) {
        if (rotFrames.isEmpty()) return new float[]{0, 0};

        for (RawAnimation.Keyframe frame : rotFrames) {
            if (Math.abs(frame.getTime() - time) < 0.001f) {
                return frame.getValues();
            }
        }

        RawAnimation.Keyframe prevFrame = null;
        RawAnimation.Keyframe nextFrame = null;

        for (RawAnimation.Keyframe frame : rotFrames) {
            if (frame.getTime() <= time) {
                prevFrame = frame;
            } else if (nextFrame == null) {
                nextFrame = frame;
                break;
            }
        }

        if (prevFrame == null) return rotFrames.get(0).getValues();
        if (nextFrame == null) return rotFrames.get(rotFrames.size() - 1).getValues();

        float t = (time - prevFrame.getTime()) / (nextFrame.getTime() - prevFrame.getTime());
        float[] prevRot = prevFrame.getValues();
        float[] nextRot = nextFrame.getValues();

        return new float[]{
                prevRot[0] + (nextRot[0] - prevRot[0]) * t, // pitch
                prevRot[1] + (nextRot[1] - prevRot[1]) * t  // yaw
        };
    }

    private static float[] findOrInterpolatePosition(List<RawAnimation.Keyframe> posFrames, float time) {
        if (posFrames.isEmpty()) return new float[]{0, 0, 0};

        for (RawAnimation.Keyframe frame : posFrames) {
            if (Math.abs(frame.getTime() - time) < 0.001f) {
                float[] values = frame.getValues();
                return new float[]{
                        values[0] * SCALE_FACTOR,
                        values[1] * SCALE_FACTOR,
                        values[2] * SCALE_FACTOR
                };
            }
        }

        RawAnimation.Keyframe prevFrame = null;
        RawAnimation.Keyframe nextFrame = null;

        for (RawAnimation.Keyframe frame : posFrames) {
            if (frame.getTime() <= time) {
                prevFrame = frame;
            } else if (nextFrame == null) {
                nextFrame = frame;
                break;
            }
        }

        if (prevFrame == null) {
            float[] values = posFrames.get(0).getValues();
            return new float[]{
                    values[0] * SCALE_FACTOR,
                    values[1] * SCALE_FACTOR,
                    values[2] * SCALE_FACTOR
            };
        }
        if (nextFrame == null) {
            float[] values = posFrames.get(posFrames.size() - 1).getValues();
            return new float[]{
                    values[0] * SCALE_FACTOR,
                    values[1] * SCALE_FACTOR,
                    values[2] * SCALE_FACTOR
            };
        }

        float t = (time - prevFrame.getTime()) / (nextFrame.getTime() - prevFrame.getTime());
        float[] prevPos = prevFrame.getValues();
        float[] nextPos = nextFrame.getValues();

        // Интерполируем и масштабируем
        return new float[]{
                (prevPos[0] + (nextPos[0] - prevPos[0]) * t) * SCALE_FACTOR,
                (prevPos[1] + (nextPos[1] - prevPos[1]) * t) * SCALE_FACTOR,
                (prevPos[2] + (nextPos[2] - prevPos[2]) * t) * SCALE_FACTOR
        };
    }

}
