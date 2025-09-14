package org.playsi.afkcam.client.Camera;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public class FreecamPosition {
    @Getter
    @Setter
    private double x, y, z;
    @Getter
    @Setter
    private float yaw, pitch;

    // Use Vec3f instead of JOML Vector3f for 1.19.2
    private Vec3f forward = new Vec3f(0.0f, 0.0f, 1.0f);
    private Vec3f up = new Vec3f(0.0f, 1.0f, 0.0f);
    private Vec3f right = new Vec3f(1.0f, 0.0f, 0.0f);

    public FreecamPosition(Entity entity) {
        this.x = entity.getX();
        this.y = calculateCameraY(entity);
        this.z = entity.getZ();
        setRotation(entity.getYaw(), entity.getPitch());
    }

    public FreecamPosition() {
    }

    public FreecamPosition(FreecamPosition other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        setRotation(other.yaw, other.pitch);
    }

    // Updated rotation calculation for 1.19.2 without JOML Quaternionf
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;

        // Convert angles to radians
        float yawRad = -yaw * ((float) Math.PI / 180);
        float pitchRad = pitch * ((float) Math.PI / 180);

        // Calculate direction vectors manually
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        // Forward vector
        forward = new Vec3f(
                sinYaw * cosPitch,
                -sinPitch,
                cosYaw * cosPitch
        );

        // Up vector (relative to rotation)
        up = new Vec3f(
                sinYaw * sinPitch,
                cosPitch,
                cosYaw * sinPitch
        );

        // Right vector (cross product of forward and world up)
        right = new Vec3f(cosYaw, 0.0f, -sinYaw);
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
        this.x += forward.getX() * forwardOffset + up.getX() * upOffset + right.getX() * rightOffset;
        this.y += forward.getY() * forwardOffset + up.getY() * upOffset + right.getY() * rightOffset;
        this.z += forward.getZ() * forwardOffset + up.getZ() * upOffset + right.getZ() * rightOffset;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos((int) (x / 16), (int) (z / 16));
    }

    public Vec3f getPositionVec3f() {
        return new Vec3f((float) x, (float) y, (float) z);
    }

    // Alternative method to get position as Vec3d (more common in 1.19.2)
    public Vec3d getPositionVec3d() {
        return new Vec3d(x, y, z);
    }

    public FreecamPosition copy() {
        return new FreecamPosition(this);
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Метод для проверки равенства позиций (для оптимизации)
    public boolean positionEquals(FreecamPosition other, double epsilon) {
        if (other == null) return false;
        return Math.abs(this.x - other.x) < epsilon &&
                Math.abs(this.y - other.y) < epsilon &&
                Math.abs(this.z - other.z) < epsilon &&
                Math.abs(this.yaw - other.yaw) < epsilon &&
                Math.abs(this.pitch - other.pitch) < epsilon;
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