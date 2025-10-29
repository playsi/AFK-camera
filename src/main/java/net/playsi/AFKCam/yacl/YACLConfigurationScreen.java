package net.playsi.Afkcam.yacl;

import dev.isxander.yacl3.api.*;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import net.playsi.Afkcam.Afkcam;
import net.playsi.Afkcam.client.AfkcamClient;
import net.playsi.Afkcam.config.Config;
import net.playsi.Afkcam.utils.ModMenuUtils;
import net.playsi.Afkcam.yacl.base.SimpleCategory;
import net.playsi.Afkcam.yacl.base.SimpleGroup;
import net.playsi.Afkcam.yacl.base.SimpleOption;
import net.playsi.Afkcam.yacl.extension.SimpleOptionExtension;
import net.playsi.Afkcam.yacl.screen.SimpleYACLScreen;

import java.util.function.Function;

@ExtensionMethod(SimpleOptionExtension.class)
public class YACLConfigurationScreen {

	private static final Function<Boolean, Text> ENABLED_OR_DISABLE_FORMATTER = ModMenuUtils.getEnabledOrDisabledFormatter();

	private YACLConfigurationScreen() {
		throw new IllegalStateException("Screen class");
	}

	public static Screen createScreen(Screen parent) {
		Config defConfig = Config.getNewInstance();
		Config config = Config.getInstance();

		return SimpleYACLScreen.startBuilder(parent, config::saveAsync)
				.categories(getGeneralCategory(defConfig, config))
				.build();
	}

	private static ConfigCategory getGeneralCategory(Config defConfig, Config config) {
		return SimpleCategory.startBuilder("general")
				.groups(
						get_mod_enabled_Group(defConfig, config),
						get_additional_settings_Group(defConfig, config)
				)
				.build();
	}

	private static OptionGroup get_mod_enabled_Group(Config defConfig, Config config) {
		return SimpleGroup.startBuilder("mod_enabled_group").options(
				SimpleOption.<Boolean>startBuilder("mod_enabled_option")
						.withBinding(defConfig.isModEnabled(), config::isModEnabled, config::setModEnabled, false)
						.withController(ENABLED_OR_DISABLE_FORMATTER)
						.build()
		).build();
	}

	private static OptionGroup get_additional_settings_Group(Config defConfig, Config config) {
		return SimpleGroup.startBuilder("additional_settings_group").options(
				SimpleOption.<Boolean>startBuilder("debug_log_enabled")
						.withBinding(defConfig.isDebugLogEnabled(), config::isDebugLogEnabled, config::setDebugLogEnabled, false)
						.withController(ENABLED_OR_DISABLE_FORMATTER)
						.build(),

				SimpleOption.<Float>startBuilder("activation_after")
						.withBinding(defConfig.getActivationAfter(), config::getActivationAfter, config::setActivationAfter, false)
						.withController(1F, 600F, 1.0F)
						.build(),


				SimpleOption.<Boolean>startBuilder("disable_on_damage")
						.withBinding(defConfig.isDisableOnDamage(), config::isDisableOnDamage, config::setDisableOnDamage, false)
						.withController(ENABLED_OR_DISABLE_FORMATTER)
						.build(),

				SimpleOption.<Boolean>startBuilder("disable_on_death")
						.withBinding(defConfig.isDisableOnDeath(), config::isDisableOnDeath, config::setDisableOnDeath, false)
						.withController(ENABLED_OR_DISABLE_FORMATTER)
						.build(),

				SimpleOption.<Boolean>startBuilder("load_default_animation")
						.withBinding(defConfig.isLoadDefaultAnimation(), config::isLoadDefaultAnimation, config::setLoadDefaultAnimation, false)
						.withController(ENABLED_OR_DISABLE_FORMATTER)
						.build(),

				SimpleOption.<Boolean>startBuilder("camera_follow")
						.withBinding(defConfig.isCameraFollow(), config::isCameraFollow, config::setCameraFollow, false)
						.withController(ENABLED_OR_DISABLE_FORMATTER)
						.build()
		).build();
	}
}
