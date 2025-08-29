package org.playsi.afkcam.client.Mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.playsi.afkcam.client.AFKmode.AFKcameraManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.MinecraftClient;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    // Prevents interacting with blocks when AFK mode is active.
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (AFKcameraManager.isAfkModeActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    // Prevents interacting with entities when AFK mode is active, and prevents interacting with self.
    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity.equals(mc.player) || AFKcameraManager.isAfkModeActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    // Prevents interacting with entities when AFK mode is active, and prevents interacting with self.
    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true)
    private void onInteractEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity.equals(mc.player) || AFKcameraManager.isAfkModeActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}