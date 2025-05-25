package org.playsi.afkcam.client;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.playsi.afkcam.client.Camera.FreeCamManager;
import org.playsi.afkcam.client.RPsParser.CinematicCameraResourceReloadListener;
import org.playsi.afkcam.client.config.Config;


public class AfkcamClient implements ClientModInitializer {
    @Getter
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    @Getter
    private static Config config;

    @Override
    public void onInitializeClient() {

        if (Config.GSON.load()) {
            config = Config.GSON.instance();
        }

        registerKeybinds();
        registerEvents();

        CinematicCameraResourceReloadListener.register();
        CinematicCameraCommands.register();

    }
    private void registerEvents() {
        // Обработка тиков
        ClientTickEvents.START_CLIENT_TICK.register(FreeCamManager::preTick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AFKcameraManager.tick();
        });
        WorldRenderEvents.BEFORE_ENTITIES.register((context) -> {
           AFKcameraManager.onRender(context.tickCounter().getDynamicDeltaTicks());
        });

        // Обработка отключения от сервера
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FreeCamManager.onDisconnect();
        });

        // Обработка смерти игрока (опционально)
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity == MC.player && FreeCamManager.isEnabled()) {
                FreeCamManager.moveToPlayer();
            }
        });
    }


//уф за это бы мне ******, но пусть будет
    private void registerKeybinds() {
        KeyBinding freecamToggle = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.afkcam.toggle",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_F8, // F8 для переключения
                        "category.afkcam"
                )
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (freecamToggle.wasPressed()) {
                FreeCamManager.freecamToggle();
            }
        });
    }
}