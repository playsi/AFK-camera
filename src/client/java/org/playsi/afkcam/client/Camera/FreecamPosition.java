package org.playsi.afkcam.client.Camera;


import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.ChunkPos;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FreecamPosition {
    @Getter
    @Setter
    private double x, y, z;
    @Getter
    @Setter
    private float yaw, pitch;

    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f forward = new Vector3f(0.0f, 0.0f, 1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);

    public FreecamPosition(Entity entity) {
        this.x = entity.getX();
        this.y = calculateCameraY(entity);
        this.z = entity.getZ();
        setRotation(entity.getYaw(), entity.getPitch());
    }
    public FreecamPosition(){
    }

    // From net.minecraft.client.render.Camera.setRotation
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;

        rotation.rotationYXZ(
                -yaw * ((float) Math.PI / 180),
                pitch * ((float) Math.PI / 180),
                0.0f
        );

        forward.set(0.0f, 0.0f, 1.0f).rotate(rotation);
        up.set(0.0f, 1.0f, 0.0f).rotate(rotation);
        right.set(1.0f, 0.0f, 0.0f).rotate(rotation);
    }


    // Invert the rotation so that it is mirrored
    // As-per net.minecraft.client.render.Camera.update
    public void mirrorRotation() {
        setRotation(yaw + 180.0f, -pitch);
    }

    // Move forward/backward relative to the current rotation
    public void moveForward(double distance) {
        move(distance, 0.0, 0.0);
    }

    // Move relative to current rotation
    // From net.minecraft.client.render.Camera.moveBy
    public void move(double forwardOffset, double upOffset, double rightOffset) {
        this.x += forward.x() * forwardOffset + up.x() * upOffset + right.x() * rightOffset;
        this.y += forward.y() * forwardOffset + up.y() * upOffset + right.y() * rightOffset;
        this.z += forward.z() * forwardOffset + up.z() * upOffset + right.z() * rightOffset;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos((int) (x / 16), (int) (z / 16));
    }

    public Vector3f getPositionVec3f() {
        return new Vector3f((float) x, (float) y, (float) z);
    }

    @Override
    public String toString() {
        return String.format("FreecamPosition[x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f]", x, y, z, yaw, pitch);
    }

    private static double calculateCameraY(Entity entity) {
        if (entity.getPose() == EntityPose.SWIMMING) {
            return entity.getY();
        }
        return entity.getY() - entity.getEyeHeight(EntityPose.SWIMMING) + entity.getEyeHeight(entity.getPose());
    }

}