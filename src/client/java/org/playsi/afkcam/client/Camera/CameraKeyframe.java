package org.playsi.afkcam.client.Camera;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CameraKeyframe {
    private float time; // время в секундах
    private double x, y, z; // позиция
    private float yaw, pitch; // вращение в градусах
    private InterpolationType interpolation; // тип интерполяции

    public enum InterpolationType {
        STEP,      // без интерполяции
        LINEAR,    // линейная интерполяция
        CATMULLROM // плавная кривая
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
        pos.setRotation(yaw, pitch);
        return pos;
    }
}