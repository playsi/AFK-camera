package net.playsi.Afkcam.config;

import lombok.*;
import net.playsi.Afkcam.utils.CodecUtils;
import net.playsi.Afkcam.utils.ConfigUtils;
import org.slf4j.*;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;

import net.playsi.Afkcam.Afkcam;

import java.io.*;
import java.util.concurrent.CompletableFuture;

import static net.playsi.Afkcam.utils.CodecUtils.option;

@Getter
@Setter
@AllArgsConstructor
public class Config {

	public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			option("modEnabled", true, Codec.BOOL, Config::isModEnabled),
			option("debugLogEnabled", false, Codec.BOOL, Config::isDebugLogEnabled),
			option("activationAfter", 30.0F, Codec.FLOAT, Config::getActivationAfter),
			option("disableOnDamage", true, Codec.BOOL, Config::isDisableOnDamage),
			option("disableOnDeath", true, Codec.BOOL, Config::isDisableOnDeath),
			option("loadDefaultAnimation", true, Codec.BOOL, Config::isLoadDefaultAnimation),
			option("cameraFollow", false, Codec.BOOL, Config::isCameraFollow)
	).apply(instance, Config::new));

	private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(Afkcam.MOD_ID + ".json5").toFile();
	private static final Logger LOGGER = LoggerFactory.getLogger(Afkcam.MOD_NAME + "/Config");
	private static Config INSTANCE;

	private boolean modEnabled;
	private boolean debugLogEnabled;
	private float activationAfter;
	private boolean disableOnDamage;
	private boolean disableOnDeath;
	private boolean loadDefaultAnimation;
	private boolean cameraFollow;

	private Config() {
		throw new IllegalArgumentException();
	}

	public static Config getInstance() {
		return INSTANCE == null ? reload() : INSTANCE;
	}

	public static Config reload() {
		return INSTANCE = Config.read();
	}

	public static Config getNewInstance() {
		return CodecUtils.parseNewInstanceHacky(CODEC);
	}

	private static Config read() {
		return ConfigUtils.readConfig(CODEC, CONFIG_FILE, LOGGER);
	}

	public void saveAsync() {
		CompletableFuture.runAsync(this::save);
	}

	public void save() {
		ConfigUtils.saveConfig(this, CODEC, CONFIG_FILE, LOGGER);
	}
}