package net.playsi.Afkcam.client.Camera;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class CameraAnimation {
    private String name;
    private List<PositionKeyframe> positionKeyframes;
    private List<RotationKeyframe> rotationKeyframes;

    public CameraAnimation(String name) {
        this.name = name;
        this.positionKeyframes = new ArrayList<>();
        this.rotationKeyframes = new ArrayList<>();
    }

    public void addPositionKeyframe(double time, double x, double y, double z, InterpolationType interpolation) {
        PositionKeyframe keyframe = new PositionKeyframe(time, x, y, z, interpolation);
        insertSorted(positionKeyframes, keyframe);
    }

    public void addRotationKeyframe(double time, double yaw, double pitch, InterpolationType interpolation) {
        RotationKeyframe keyframe = new RotationKeyframe(time, yaw, pitch, interpolation);
        insertSorted(rotationKeyframes, keyframe);
    }

    private <T extends BaseKeyframe> void insertSorted(List<T> list, T keyframe) {
        int insertIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getTime() > keyframe.getTime()) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }
        list.add(insertIndex, keyframe);
    }

    public boolean removePositionKeyframe(int index) {
        if (index >= 0 && index < positionKeyframes.size()) {
            positionKeyframes.remove(index);
            return true;
        }
        return false;
    }

    public boolean removeRotationKeyframe(int index) {
        if (index >= 0 && index < rotationKeyframes.size()) {
            rotationKeyframes.remove(index);
            return true;
        }
        return false;
    }

    public boolean removePositionKeyframeAtTime(double time) {
        for (int i = 0; i < positionKeyframes.size(); i++) {
            if (Math.abs(positionKeyframes.get(i).getTime() - time) < 0.001) {
                return removePositionKeyframe(i);
            }
        }
        return false;
    }

    public boolean removeRotationKeyframeAtTime(double time) {
        for (int i = 0; i < rotationKeyframes.size(); i++) {
            if (Math.abs(rotationKeyframes.get(i).getTime() - time) < 0.001) {
                return removeRotationKeyframe(i);
            }
        }
        return false;
    }

    public void clear() {
        positionKeyframes.clear();
        rotationKeyframes.clear();
    }

    public double getDuration() {
        double maxTime = 0.0;
        if (!positionKeyframes.isEmpty()) {
            maxTime = Math.max(maxTime, positionKeyframes.get(positionKeyframes.size() - 1).getTime());
        }
        if (!rotationKeyframes.isEmpty()) {
            maxTime = Math.max(maxTime, rotationKeyframes.get(rotationKeyframes.size() - 1).getTime());
        }
        return maxTime;
    }

    public boolean isEmpty() {
        return positionKeyframes.isEmpty() && rotationKeyframes.isEmpty();
    }

    public enum InterpolationType {
        STEP,
        LINEAR,
        CATMULLROM
    }

    @Data
    @NoArgsConstructor
    public static abstract class BaseKeyframe {
        protected double time;
        protected InterpolationType interpolation;

        public BaseKeyframe(double time, InterpolationType interpolation) {
            this.time = time;
            this.interpolation = interpolation;
        }
    }

    @Getter
    @Setter
    public static class PositionKeyframe extends BaseKeyframe {
        private double x, y, z;

        public PositionKeyframe(double time, double x, double y, double z, InterpolationType interpolation) {
            super(time, interpolation);
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    @Getter
    @Setter
    public static class RotationKeyframe extends BaseKeyframe {
        private double yaw, pitch;

        public RotationKeyframe(double time, double yaw, double pitch, InterpolationType interpolation) {
            super(time, interpolation);
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}