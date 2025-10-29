package net.playsi.Afkcam.modmenu;

import com.terraformersmc.modmenu.api.*;

import net.fabricmc.loader.api.*;

import net.playsi.Afkcam.Afkcam;
import net.playsi.Afkcam.client.AFKcamResourceReloadListener;
import net.playsi.Afkcam.client.AfkcamClient;
import net.playsi.Afkcam.yacl.YACLConfigurationScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModMenuIntegration implements ModMenuApi {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(Afkcam.MOD_NAME
					+ AFKcamResourceReloadListener.class.getName());


	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		FabricLoader fabricLoader = FabricLoader.getInstance();
		if (fabricLoader.isModLoaded("yet_another_config_lib_v3")) {
			ModContainer modContainer = fabricLoader.getModContainer("yet_another_config_lib_v3").orElseThrow();
			Version version = modContainer.getMetadata().getVersion();
			try {
				Version requestsVersion = Version.parse(Afkcam.YACL_DEPEND_VERSION);
				if (version.compareTo(requestsVersion) >= 0) {
					return YACLConfigurationScreen::createScreen;
				}
			} catch (VersionParsingException e) {
				LOGGER.error("Failed to compare YACL version, tell mod author about this error: ", e);
			}
			return parent -> NoConfigLibraryScreen.createScreenAboutOldVersion(parent, version.getFriendlyString());
		}
		return NoConfigLibraryScreen::createScreen;
	}
}
