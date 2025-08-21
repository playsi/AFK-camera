package org.playsi.afkcam.client.Mixins;

import net.minecraft.client.render.GameRenderer;
import org.playsi.afkcam.client.Camera.FreeCamManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



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