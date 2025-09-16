package org.playsi.afkcam.client.Mixins;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.playsi.afkcam.client.AFKmode.AFKcameraManager;
import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    // Prevents attacks when allowInteract is disabled.
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (AFKcameraManager.isAfkModeActive()) {
            cir.setReturnValue(false);
        }
    }

    // Prevents item pick when AFK mode is active.
    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        if (AFKcameraManager.isAfkModeActive()) {
            ci.cancel();
        }
    }

    // Prevents block breaking when AFK mode is active.
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreaking(CallbackInfo ci) {
        if (AFKcameraManager.isAfkModeActive()) {
            ci.cancel();
        }
    }

    // Disables AFKcam if the player disconnects.
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(Screen disconnectionScreen, boolean transferring, CallbackInfo ci) {
        AFKcameraManager.onDisconnect();
    }
}
