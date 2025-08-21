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

import java.util.List;

import static org.playsi.afkcam.client.Camera.FreeCamManager.isEnabled;
import static org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILHARD;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Shadow protected abstract void renderEntity(Entity entity, double camX, double camY, double camZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    // Makes the player render if showPlayer is enabled.
    @Inject(method = "renderEntities", at = @At("TAIL"), locals = CAPTURE_FAILHARD)
    private void onRender(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            Camera camera,
            RenderTickCounter tickCounter,
            List<Entity> entities,
            CallbackInfo ci) {
        if (isEnabled()) {
            Vec3d position = camera.getPos();
            float tickDelta = tickCounter.getTickProgress(false);
            renderEntity(MinecraftClient.getInstance().player, position.x, position.y, position.z, tickDelta, matrices, bufferBuilders.getEntityVertexConsumers());
        }
    }
}
