package net.playsi.Afkcam.mixin;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import net.playsi.Afkcam.client.AFKmodeState.AFKCamLoopState;
import net.playsi.Afkcam.client.AfkcamClient;
import net.playsi.Afkcam.client.Animations.Parser.BbModelAnim;
import net.playsi.Afkcam.client.Camera.FreeCamManager;
import net.playsi.Afkcam.utils.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.20.2
/*import net.minecraft.world.tick.TickManager;*/

//? if <= 1.21.1
import net.minecraft.client.render.entity.EntityRenderDispatcher;

//? if > 1.21.1 {
/*import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.ObjectAllocator;
*///?}

//? if > 1.21.5
/*import com.mojang.blaze3d.buffers.GpuBufferSlice;*/

//? if > 1.21.8 {
/*import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.WorldRenderState;
*///?}

import java.util.List;

import static org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILHARD;

@Slf4j
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    private static final LogUtils LOGGER = new LogUtils(WorldRendererMixin.class);

    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    //? if <= 1.21.1 {

        @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

        @Unique
        private static final MinecraftClient MC = MinecraftClient.getInstance();

        @Inject(method = "render", at = @At("TAIL"), locals = CAPTURE_FAILHARD)
        private void onRender(
                              //? if <= 1.20.4
                              MatrixStack matrices,

                              //? <= 1.20.6 {
                               float tickDeltaQ,
                               long limitTime,
                              //?}

                              //? if >= 1.21
                              /*RenderTickCounter tickCounter,*/
                              boolean renderBlockOutline,
                              Camera camera,
                              GameRenderer gameRenderer,
                              LightmapTextureManager lightmapTextureManager,
                              Matrix4f matrix4f,
                              //? if > 1.20.4
                              /*Matrix4f matrix4f2,*/
                              CallbackInfo ci) {

            if (!FreeCamManager.isEnabled() && MC.world != null) return;


            float tickDelta =
                    //? if >= 1.21
                    /*tickCounter.getTickDelta(false);*/
                    //? if <= 1.20.6
                    tickDeltaQ;

            //? if >= 1.21
            /*LOGGER.info("tickDelta:  " + tickDelta);*/

            AFKCamLoopState.onRender(tickDelta);

            //? if > 1.20.4 {
            /*MatrixStack matrices = new MatrixStack();
            matrices.multiplyPositionMatrix(matrix4f);
            *///?}

            VertexConsumerProvider.Immediate vertexConsumers =
                    MC.getBufferBuilders().getEntityVertexConsumers();


            double renderX = MC.player.lastRenderX + (MC.player.getX() - MC.player.lastRenderX) * tickDelta;
            double renderY = MC.player.lastRenderY + (MC.player.getY() - MC.player.lastRenderY) * tickDelta;
            double renderZ = MC.player.lastRenderZ + (MC.player.getZ() - MC.player.lastRenderZ) * tickDelta;

            entityRenderDispatcher.render(
                    MC.player,
                    renderX - camera.getPos().x,
                    renderY - camera.getPos().y,
                    renderZ - camera.getPos().z,
                    MC.player.getYaw(),
                    tickDelta,
                    matrices,
                    vertexConsumers,
                    entityRenderDispatcher.getLight(MC.player, tickDelta)
            );
            vertexConsumers.draw();
            //? if > 1.20.4
            /*matrices.pop();*/
        }
    }


    //?} else if <= 1.21.8 {
    /*@Shadow
    protected abstract void renderEntity(Entity entity, double camX, double camY, double camZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    // Makes the player render if showPlayer is enabled.


    @Inject(method = "renderEntities", at = @At("TAIL"), locals = CAPTURE_FAILHARD)
    private void onRender(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            Camera camera,
            RenderTickCounter tickCounter,
            List<Entity> entities,
            CallbackInfo ci) {
        if (FreeCamManager.isEnabled()) {
            float tickDelta = tickCounter.
                    //? if > 1.21.4 {
                    /^getTickProgress(MinecraftClient.getInstance().player.isFrozen());
                    ^///?} else {
                    getTickDelta(MinecraftClient.getInstance().player.isFrozen());
                    //?}
                AFKCamLoopState.onRender(tickDelta);

                Vec3d position = camera.getPos();
                renderEntity(MinecraftClient.getInstance().player, position.x, position.y, position.z, tickDelta, matrices, bufferBuilders.getEntityVertexConsumers());
            }
        }
    }

    *///?} else if > 1.21.8 {

    /*@Shadow public abstract void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky);

    @Shadow @Nullable public abstract Framebuffer getEntityFramebuffer();

    @Shadow protected abstract EntityRenderState getAndUpdateRenderState(Entity entity, float tickProgress);

    private static final MinecraftClient  MC = AfkcamClient.getMC();
    // Makes the player render if showPlayer is enabled.
        @Inject(
                method = "fillEntityRenderStates",
                at = @At("RETURN")
        )
        private void fillEntityRenderStates(
                Camera camera,
                Frustum frustum,
                RenderTickCounter tickCounter,
                WorldRenderState renderStates,
                CallbackInfo ci
        ) {
            if (FreeCamManager.isEnabled() && MC.world != null ) {

                ClientPlayerEntity player = MC.player;

                TickManager tickManager = MC.world.getTickManager();
                float tickDelta = tickCounter.getTickProgress(!tickManager.isFrozen());

                EntityRenderState playerRenderState = this.getAndUpdateRenderState(player, tickDelta);
                renderStates.entityRenderStates.add(playerRenderState);

                AFKCamLoopState.onRender(tickDelta);
                //TODO при афк игрок игнорирует события, камера не успевает за игроком, игрок дергаеться
            }
        }
}
*///?}