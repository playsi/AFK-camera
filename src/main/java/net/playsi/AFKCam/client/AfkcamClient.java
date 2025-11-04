package net.playsi.Afkcam.client;

import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.MinecraftClient;
import net.playsi.Afkcam.client.AFKmodeState.AFKCamLoopState;
import net.playsi.Afkcam.client.Camera.FreeCamManager;
import net.playsi.Afkcam.client.command.AFKcamCommands;
import net.playsi.Afkcam.config.Config;

 //DEBUG
import net.playsi.Afkcam.debug.DebugOverlayRenderer;


public class AfkcamClient implements ClientModInitializer {
	@Getter
	private static final MinecraftClient MC = MinecraftClient.getInstance();

	@Override
	public void onInitializeClient() {
		registerEvents();
		AFKcamResourceReloadListener.register();
		AFKcamCommands.register();
		//DEBUG
		DebugOverlayRenderer.register();
	}

	private void registerEvents() {

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			AFKCamLoopState.tick();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			AFKCamLoopState.onDisconnect();
		});

		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity == MC.player && Config.getInstance().isModEnabled()) {
				FreeCamManager.moveToPlayer();
		}
		});
	}
}