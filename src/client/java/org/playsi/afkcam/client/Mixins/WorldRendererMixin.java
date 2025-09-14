package org.playsi.afkcam.client.Mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.playsi.afkcam.client.Camera.FreeCamManager.isEnabled;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Shadow protected abstract void renderEntity(Entity entity, double camX, double camY, double camZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    // Makes the player render if showPlayer is enabled.
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            MatrixStack matrices,
            float tickDelta,
            long limitTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            net.minecraft.util.math.Matrix4f matrix4f,
            CallbackInfo ci) {
        if (isEnabled()) {
            Vec3d position = camera.getPos();
            renderEntity(MinecraftClient.getInstance().player, position.x, position.y, position.z, tickDelta, matrices, bufferBuilders.getEntityVertexConsumers());
        }
    }
}