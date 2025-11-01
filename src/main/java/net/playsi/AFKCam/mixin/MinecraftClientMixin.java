package net.playsi.Afkcam.mixin;


import net.minecraft.client.MinecraftClient;
import net.playsi.Afkcam.client.AFKmodeState.AFKCamLoopState;
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
        if (AFKCamLoopState.isAfkModeActive()) {
            cir.setReturnValue(false);
        }
    }

    // Prevents item pick when AFK mode is active.
    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        if (AFKCamLoopState.isAfkModeActive()) {
            ci.cancel();
        }
    }

    // Prevents block breaking when AFK mode is active.
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreaking(CallbackInfo ci) {
        if (AFKCamLoopState.isAfkModeActive()) {
            ci.cancel();
        }
    }

    // Disables AFKcam if the player disconnects.
    //? if >= 1.21.9 {
    /*@Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
    *///?} else
    @Inject(method = "disconnect", at = @At("HEAD"))

    private void onDisconnect(CallbackInfo ci) {
        AFKCamLoopState.onDisconnect();
    }
}
