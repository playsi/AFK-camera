package net.playsi.Afkcam.client.Camera;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CameraKeyframe {
    private double time;
    private double x, y, z;
    private double yaw, pitch;

    private InterpolationType positionInterpolation;
    private InterpolationType rotationInterpolation;

    public enum InterpolationType {
        STEP,
        LINEAR,
        CATMULLROM
    }

    public CameraKeyframe(float time, FreecamPosition position,
                          InterpolationType positionInterpolation,
                          InterpolationType rotationInterpolation) {
        this.time = time;
        this.x = position.getX();
        this.y = position.getY();
        this.z = position.getZ();
        this.yaw = position.getYaw();
        this.pitch = position.getPitch();
        this.positionInterpolation = positionInterpolation;
        this.rotationInterpolation = rotationInterpolation;
    }

    public FreecamPosition toFreecamPosition() {
        FreecamPosition pos = new FreecamPosition();
        pos.setX(x);
        pos.setY(y);
        pos.setZ(z);
        pos.setRotation((float) yaw, (float) pitch);
        return pos;
    }
}