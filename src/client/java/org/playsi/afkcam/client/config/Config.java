package org.playsi.afkcam.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import dev.isxander.yacl3.platform.YACLPlatform;
import lombok.Getter;
import lombok.Setter;
import org.playsi.afkcam.Afkcam;
import org.playsi.afkcam.client.AfkcamClient;

@Setter
@Getter
public class Config {
    public static final ConfigClassHandler<Config> GSON = ConfigClassHandler.createBuilder(Config.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(YACLPlatform.getConfigDir().resolve(Afkcam.MOD_ID + ".json"))
                    .build())
            .build();

    @SerialEntry
    private boolean modEnabled = true;

    @SerialEntry
    private boolean debugLogEnabled = false;

    @SerialEntry
    private int activationAfter = 1;

    @SerialEntry
    private double cameraSpeed = 1.0;

    @SerialEntry
    private boolean cameraFollow = false;

    @SerialEntry
    private boolean fade = true;

    @SerialEntry
    private int fadeIn = 1;

    @SerialEntry
    private int fadeOut = 1;
}