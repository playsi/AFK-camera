package net.playsi.Afkcam.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.playsi.Afkcam.client.Camera.FreeCamManager;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // Disables block outlines.
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCamManager.isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}