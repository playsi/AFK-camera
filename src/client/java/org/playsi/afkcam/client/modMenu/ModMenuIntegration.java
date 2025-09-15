package org.playsi.afkcam.client.modMenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.gui.controllers.BooleanController;
import dev.isxander.yacl3.gui.controllers.slider.FloatSliderController;
import dev.isxander.yacl3.impl.controller.FloatSliderControllerBuilderImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.config.Config;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> createConfigScreen().generateScreen(parent);
    }

    public static YetAnotherConfigLib createConfigScreen() {
        Config config = Config.INSTANCE.getConfig();
        Config defaults = new Config();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("afkcam.modmenu.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("afkcam.category.general"))

                        .option(Option.createBuilder(boolean.class)
                                .name(Text.translatable("afkcam.option.modEnabled"))
                                .description(OptionDescription.of(Text.translatable("afkcam.option.modEnabled.desc")))
                                .binding(
                                        defaults.isModEnabled(),
                                        () -> config.isModEnabled(),
                                        value -> config.setModEnabled(value)
                                )
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.createBuilder(boolean.class)
                                .name(Text.translatable("afkcam.option.debugLogEnabled"))
                                .description(OptionDescription.of(Text.translatable("afkcam.option.debugLogEnabled.desc")))
                                .binding(
                                        defaults.isDebugLogEnabled(),
                                        () -> config.isDebugLogEnabled(),
                                        value -> config.setDebugLogEnabled(value)
                                )
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.createBuilder(float.class)
                                .name(Text.translatable("afkcam.option.activationAfter"))
                                .description(OptionDescription.of(Text.translatable("afkcam.option.activationAfter.desc")))
                                .binding(
                                        defaults.getActivationAfter(),
                                        () -> config.getActivationAfter(),
                                        value -> config.setActivationAfter(value)
                                )
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(0.1f, 60.0f)  // Задайте подходящий диапазон
                                        .step(0.1f)           // Шаг изменения значения
                                )
                                .build())

//                        .option(Option.createBuilder(double.class)
//                                .name(Text.translatable("afkcam.option.cameraSpeed"))
//                                .tooltip(Text.translatable("afkcam.option.cameraSpeed.desc"))
//                                .binding(
//                                        defaults.getCameraSpeed(),
//                                        () -> config.getCameraSpeed(),
//                                        value -> config.setCameraSpeed(value)
//                                )
//                                .controller(opt -> new DoubleSliderController(opt, 0.1, 5.0, 0.1))
//                                .build())

//                        .option(Option.createBuilder(boolean.class)
//                                .name(Text.translatable("afkcam.option.cameraFollow"))
//                                .tooltip(Text.translatable("afkcam.option.cameraFollow.desc"))
//                                .binding(
//                                        defaults.isCameraFollow(),
//                                        () -> config.isCameraFollow(),
//                                        value -> config.setCameraFollow(value)
//                                )
//                                .controller(BooleanController::new)
//                                .build())

                        .option(Option.createBuilder(boolean.class)
                                .name(Text.translatable("afkcam.option.disableOnDamage"))
                                .description(OptionDescription.of(Text.translatable("afkcam.option.disableOnDamage.desc")))
                                .binding(
                                        defaults.isDisableOnDamage(),
                                        () -> config.isDisableOnDamage(),
                                        value -> config.setDisableOnDamage(value)
                                )
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.createBuilder(boolean.class)
                                .name(Text.translatable("afkcam.option.disableOnDeath"))
                                .description(OptionDescription.of(Text.translatable("afkcam.option.disableOnDeath.desc")))
                                .binding(
                                        defaults.isDisableOnDeath(),
                                        () -> config.isDisableOnDeath(),
                                        value -> config.setDisableOnDeath(value)
                                )
                                .controller(BooleanControllerBuilder::create)
                                .build())

                        .option(Option.createBuilder(boolean.class)
                                .name(Text.translatable("afkcam.option.loadDefaultAnimation"))
                                .description(OptionDescription.of(Text.translatable("afkcam.option.loadDefaultAnimation.desc")))
                                .binding(
                                        defaults.isLoadDefaultAnimation(),
                                        () -> config.isLoadDefaultAnimation(),
                                        value -> {
                                            config.setLoadDefaultAnimation(value);

                                            MinecraftClient client = AfkcamClient.getMC();
                                            if (client != null) {
                                                client.reloadResources();
                                            }
                                        }
                                )
                                .controller(BooleanControllerBuilder::create)
                                .build())

//                        .option(Option.createBuilder(boolean.class)
//                                .name(Text.translatable("afkcam.option.fade"))
//                                .tooltip(Text.translatable("afkcam.option.fade.desc"))
//                                .binding(
//                                        defaults.isFade(),
//                                        () -> config.isFade(),
//                                        value -> config.setFade(value)
//                                )
//                                .controller(BooleanController::new)
//                                .build())

//                        .option(Option.createBuilder(int.class)
//                                .name(Text.translatable("afkcam.option.fadeIn"))
//                                .tooltip(Text.translatable("afkcam.option.fadeIn.desc"))
//                                .binding(
//                                        defaults.getFadeIn(),
//                                        () -> config.getFadeIn(),
//                                        value -> config.setFadeIn(value)
//                                )
//                                .controller(opt -> new IntegerSliderController(opt, 1, 10, 1))
//                                .build())

//                        .option(Option.createBuilder(int.class)
//                                .name(Text.translatable("afkcam.option.fadeOut"))
//                                .tooltip(Text.translatable("afkcam.option.fadeOut.desc"))
//                                .binding(
//                                        defaults.getFadeOut(),
//                                        () -> config.getFadeOut(),
//                                        value -> config.setFadeOut(value)
//                                )
//                                .controller(opt -> new IntegerSliderController(opt, 1, 10, 1))
//                                .build())

                        .build())
                .save(() -> Config.INSTANCE.save())
                .build();
    }
}