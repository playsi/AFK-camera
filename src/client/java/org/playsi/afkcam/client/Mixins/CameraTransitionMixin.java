package org.playsi.afkcam.client.Mixins;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.playsi.afkcam.client.Camera.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraTransitionMixin {

    @Shadow private Entity focusedEntity;
    @Shadow private float lastCameraY;
    @Shadow private float cameraY;

    // When toggling freecam, update the camera's eye height instantly without any transition.
    @Inject(method = "update", at = @At("HEAD"))
    public void onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (focusedEntity == null || this.focusedEntity == null || focusedEntity.equals(this.focusedEntity)) {
            return;
        }

        if (focusedEntity instanceof FreeCamera || this.focusedEntity instanceof FreeCamera) {
            this.lastCameraY = this.cameraY = focusedEntity.getStandingEyeHeight();
        }
    }
}