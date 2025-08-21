package org.playsi.afkcam.client.Camera;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.UUID;

public class FreeCamera extends ClientPlayerEntity {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private final int customId;

    public FreeCamera(int id) {
        super(
                MC,
                MC.world != null ? MC.world : null,
                Objects.requireNonNull(MC.getNetworkHandler()),
                MC.player != null ? MC.player.getStatHandler() : null,
                MC.player != null ? MC.player.getRecipeBook() : null,
                false,
                false
        );

        this.customId = id;
        setId(id);

        setUuid(UUID.randomUUID());

        setPose(EntityPose.SWIMMING);

        setNoGravity(true);
        noClip = true;

        setInvisible(true);
        setInvulnerable(true);
    }

    public void copyPosition(Entity entity) {
        if (entity != null) {
            applyPosition(new FreecamPosition(entity));
        }
    }

    public void applyPosition(FreecamPosition position) {
        if (position != null) {
            setPosition(position.getX(), position.getY(), position.getZ());
            setAngles(position.getYaw(), position.getPitch());
        }
    }

    public void applyPosition(double x, double y, double z, float yaw, float pitch) {
        setPosition(x, y, z);
        setAngles(yaw, pitch);
    }

    // Mutate the position and rotation based on perspective
    // If checkCollision is true, move as far as possible without colliding
    public void applyPerspective(Perspective perspective, boolean checkCollision) {
        FreecamPosition position = new FreecamPosition(this);

        switch (perspective) {
            case FIRST_PERSON:
                // Move just in front of the player's eyes
                moveForwardUntilCollision(position, 0.4, checkCollision);
                break;
            case THIRD_PERSON_FRONT:
                // Invert the rotation and fallthrough into the THIRD_PERSON case
                position.mirrorRotation();
            case THIRD_PERSON_BACK:
                // Move back as per F5 mode
                moveForwardUntilCollision(position, -4.0, checkCollision);
                break;
        }
    }

    private boolean moveForwardUntilCollision(FreecamPosition position, double distance, boolean checkCollision) {
        if (!checkCollision) {
            position.moveForward(distance);
            applyPosition(position);
            return true;
        }
        return moveForwardUntilCollision(position, distance);
    }

    private boolean moveForwardUntilCollision(FreecamPosition position, double maxDistance) {
        boolean negative = maxDistance < 0;
        maxDistance = negative ? -1 * maxDistance : maxDistance;
        double increment = 0.1;

        for (double distance = 0.0; distance < maxDistance; distance += increment) {
            FreecamPosition oldPosition = new FreecamPosition(this);

            position.moveForward(negative ? -1 * increment : increment);
            applyPosition(position);
            if (!wouldNotSuffocateInPose(getPose())) {
                applyPosition(oldPosition);
                return distance > 0;
            }
        }

        return true;
    }

    public void spawn() {
        if (clientWorld != null && !clientWorld.hasEntity(this)) {
            clientWorld.addEntity(this);
        }
    }

    public void despawn() {
        if (clientWorld != null) {
            Entity existingEntity = clientWorld.getEntityById(customId);
            if (existingEntity != null) {
                clientWorld.removeEntity(customId, RemovalReason.DISCARDED);
            }

            Entity entityByUuid = clientWorld.getEntity(getUuid());
            if (entityByUuid != null && entityByUuid != existingEntity) {
                clientWorld.removeEntity(entityByUuid.getId(), RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public boolean isUsingItem() {
        return MC.player != null ? MC.player.isUsingItem() : false;
    }

    @Override
    public void setPose(EntityPose pose) {
        super.setPose(EntityPose.SWIMMING);
    }

    @Override
    public void tick() {
        baseTick();
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if (movementType == MovementType.SELF) {
            super.move(movementType, movement);
        }
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return true;
    }

    @Override
    public void updatePositionAndAngles(double x, double y, double z, float yaw, float pitch) {
    }
}