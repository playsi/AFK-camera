package org.playsi.afkcam.client.Mixins;

import org.playsi.afkcam.client.AFKcameraManager;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.Camera.FreeCamManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract float getHealth();

    // Disables freecam upon receiving damage if disableOnDamage is enabled.
    @Inject(method = "setHealth", at = @At("HEAD"))
    private void onSetHealth(float health, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Early exit for non-player entities or null player
        if (mc.player == null || !this.equals(mc.player)) return;

        if (FreeCamManager.isEnabled() &&
                AfkcamClient.getConfig().isDisableOnDamage() &&
                !mc.player.getAbilities().creativeMode &&
                getHealth() > health) {
            AFKcameraManager.setPlayerTakeDamage(true);
        }
    }
}