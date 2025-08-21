package org.playsi.afkcam.client.modMenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.gui.controllers.BooleanController;
import dev.isxander.yacl3.gui.controllers.string.number.DoubleFieldController;
import dev.isxander.yacl3.gui.controllers.string.number.FloatFieldController;
import dev.isxander.yacl3.gui.controllers.string.number.IntegerFieldController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.config.Config;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> Config().generateScreen(parent);
    }

    public static YetAnotherConfigLib Config() {
        return YetAnotherConfigLib.create(Config.GSON, (def, config, builder) -> builder

                .title(Text.translatable("afkcam.modmenu.title"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("afkcam.category.general"))

                        .group(OptionGroup.createBuilder()

                                .option(
                                        Option.<Boolean>createBuilder()
                                                .name(Text.translatable("afkcam.option.modEnabled"))
                                                .description(OptionDescription.of(Text.translatable("afkcam.option.modEnabled.desc")))
                                                .stateManager(StateManager.createSimple(def.isModEnabled(), config::isModEnabled, config::setModEnabled))
                                                .customController(BooleanController::new)
                                                .build()
                                )

                                .option(
                                        Option.<Boolean>createBuilder()
                                                .name(Text.translatable("afkcam.option.debugLogEnabled"))
                                                .description(OptionDescription.of(Text.translatable("afkcam.option.debugLogEnabled.desc")))
                                                .stateManager(StateManager.createSimple(def.isDebugLogEnabled(), config::isDebugLogEnabled, config::setDebugLogEnabled))
                                                .customController(BooleanController::new)
                                                .build()
                                )

                                .option(
                                        Option.<Float>createBuilder()
                                                .name(Text.translatable("afkcam.option.activationAfter"))
                                                .description(OptionDescription.of(Text.translatable("afkcam.option.activationAfter.desc")))
                                                .stateManager(StateManager.createSimple(def.getActivationAfter(), config::getActivationAfter, config::setActivationAfter))
                                                .customController(FloatFieldController::new)
                                                .build()
                                )

//                                .option(
//                                        Option.<Double>createBuilder()
//                                                .name(Text.translatable("afkcam.option.cameraSpeed"))
//                                                .description(OptionDescription.of(Text.translatable("afkcam.option.cameraSpeed.desc")))
//                                                .stateManager(StateManager.createSimple(def.getCameraSpeed(), config::getCameraSpeed, config::setCameraSpeed))
//                                                .customController(DoubleFieldController::new)
//                                                .build()
//                                )

//                                .option(
//                                        Option.<Boolean>createBuilder()
//                                                .name(Text.translatable("afkcam.option.cameraFollow"))
//                                                .description(OptionDescription.of(Text.translatable("afkcam.option.cameraFollow.desc")))
//                                                .stateManager(StateManager.createSimple(def.isCameraFollow(), config::isCameraFollow, config::setCameraFollow))
//                                                .customController(BooleanController::new)
//                                                .build()
//                                )

                                .option(
                                        Option.<Boolean>createBuilder()
                                                .name(Text.translatable("afkcam.option.disableOnDamage"))
                                                .description(OptionDescription.of(Text.translatable("afkcam.option.disableOnDamage.desc")))
                                                .stateManager(StateManager.createSimple(def.isDisableOnDamage(), config::isDisableOnDamage, config::setDisableOnDamage))
                                                .customController(BooleanController::new)
                                                .build()
                                )
                                .option(
                                        Option.<Boolean>createBuilder()
                                                .name(Text.translatable("afkcam.option.disableOnDeath"))
                                                .description(OptionDescription.of(Text.translatable("afkcam.option.disableOnDeath.desc")))
                                                .stateManager(StateManager.createSimple(def.isDisableOnDeath(), config::isDisableOnDeath, config::setDisableOnDeath))
                                                .customController(BooleanController::new)
                                                .build()
                                )
                                .option(
                                        Option.<Boolean>createBuilder()
                                                .name(Text.translatable("afkcam.option.loadDefaultAnimation"))
                                                .description(OptionDescription.of(Text.translatable("afkcam.option.loadDefaultAnimation.desc")))
                                                .stateManager(StateManager.createSimple(
                                                        def.isLoadDefaultAnimation(),
                                                        config::isLoadDefaultAnimation,
                                                        value -> {
                                                            config.setLoadDefaultAnimation(value);

                                                            MinecraftClient client = AfkcamClient.getMC();
                                                            if (client != null) {
                                                                client.reloadResources();
                                                            }
                                                        }
                                                ))
                                                .customController(BooleanController::new)
                                                .build()
                                )
                                .build())


//                        .group(OptionGroup.createBuilder()
//
//                                .option(
//                                        Option.<Boolean>createBuilder()
//                                                .name(Text.translatable("afkcam.option.fade"))
//                                                .description(OptionDescription.of(Text.translatable("afkcam.option.fade.desc")))
//                                                .stateManager(StateManager.createSimple(def.isFade(), config::isFade, config::setFade))
//                                                .customController(BooleanController::new)
//                                                .build()
//                                )
//
//                                .option(
//                                        Option.<Integer>createBuilder()
//                                                .name(Text.translatable("afkcam.option.fadeIn"))
//                                                .description(OptionDescription.of(Text.translatable("afkcam.option.fadeIn.desc")))
//                                                .stateManager(StateManager.createSimple(def.getFadeIn(), config::getFadeIn, config::setFadeIn))
//                                                .customController(IntegerFieldController::new)
//                                                .build()
//                                )
//
//                                .option(
//                                        Option.<Integer>createBuilder()
//                                                .name(Text.translatable("afkcam.option.fadeOut"))
//                                                .description(OptionDescription.of(Text.translatable("afkcam.option.fadeOut.desc")))
//                                                .stateManager(StateManager.createSimple(def.getFadeOut(), config::getFadeOut, config::setFadeOut))
//                                                .customController(IntegerFieldController::new)
//                                                .build()
//                                )
//                                .build())
                        .build()
                )

        );
    }
}