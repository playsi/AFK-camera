package org.playsi.afkcam.client;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import org.playsi.afkcam.client.RPsParser.CinematicCameraResourceReloadListener;
import org.playsi.afkcam.client.config.Config;

public class AfkcamClient implements ClientModInitializer {

    @Getter
    private static Config config;

    @Override
    public void onInitializeClient() {

        if (Config.GSON.load()) {
            config = Config.GSON.instance();
        }
        CinematicCameraResourceReloadListener.register();
        CinematicCameraCommands.register();

    }
}