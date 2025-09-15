package org.playsi.afkcam.client.config;

import dev.isxander.yacl3.config.ConfigEntry;
import dev.isxander.yacl3.config.ConfigInstance;
import dev.isxander.yacl3.config.GsonConfigInstance;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;
import org.playsi.afkcam.Afkcam;

@Setter
@Getter
public class Config {
    public static final ConfigInstance<Config> INSTANCE = new GsonConfigInstance<>(
            Config.class,
            FabricLoader.getInstance().getConfigDir().resolve(Afkcam.MOD_ID + ".json")
    );

    @ConfigEntry
    private boolean modEnabled = true;

    @ConfigEntry
    private boolean debugLogEnabled = false;

    @ConfigEntry
    private float activationAfter = 30f;

//    @ConfigEntry
//    private double cameraSpeed = 1.0;

    @ConfigEntry
    private boolean disableOnDamage = true;

    @ConfigEntry
    private boolean disableOnDeath = true;

    @ConfigEntry
    private boolean loadDefaultAnimation = true;

    @ConfigEntry
    private boolean cameraFollow = false;

//    @ConfigEntry
//    private boolean fade = true;
//
//    @ConfigEntry
//    private int fadeIn = 1;
//
//    @ConfigEntry
//    private int fadeOut = 1;
}