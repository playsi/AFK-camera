package org.playsi.afkcam.client;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.playsi.afkcam.client.AFKmode.AFKcameraManager;
import org.playsi.afkcam.client.Camera.FreeCamManager;
import org.playsi.afkcam.client.RPsParser.AFKcamResourceReloadListener;
import org.playsi.afkcam.client.config.Config;

public class AfkcamClient implements ClientModInitializer {
    @Getter
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    @Getter
    private static Config config;

    @Override
    public void onInitializeClient() {


        config = Config.INSTANCE.getConfig();

        registerEvents();

        AFKcamResourceReloadListener.register();
        AFKcamCommands.register();

    }

    private void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AFKcameraManager.tick();
        });

        // Fix 2: Change getDynamicDeltaTicks() to getTickDelta() for 1.19.2
        WorldRenderEvents.BEFORE_ENTITIES.register((context) -> {
            AFKcameraManager.onRender(context.tickDelta()); // Changed from getDynamicDeltaTicks()
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FreeCamManager.onDisconnect();
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == MC.player && FreeCamManager.isEnabled()) {
                FreeCamManager.moveToPlayer();
            }
        });
    }
}