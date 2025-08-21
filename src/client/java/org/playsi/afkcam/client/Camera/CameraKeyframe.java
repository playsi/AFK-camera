package org.playsi.afkcam.client.Camera;

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
    private InterpolationType interpolation;

    public enum InterpolationType {
        STEP,
        LINEAR,
        CATMULLROM
    }

    public CameraKeyframe(float time, FreecamPosition position, InterpolationType interpolation) {
        this.time = time;
        this.x = position.getX();
        this.y = position.getY();
        this.z = position.getZ();
        this.yaw = position.getYaw();
        this.pitch = position.getPitch();
        this.interpolation = interpolation;
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