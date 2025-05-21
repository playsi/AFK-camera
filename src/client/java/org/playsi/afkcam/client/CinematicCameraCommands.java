package org.playsi.afkcam.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.playsi.afkcam.client.RPsParser.CinematicCameraResourceReloadListener;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CinematicCameraCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("cinematiccamera")
                    .then(literal("start").executes(ctx -> {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("Анимация камеры запущена."), false);
                        // TODO: Реализовать запуск анимации
                        return 1;
                    }))
                    .then(literal("stop").executes(ctx -> {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("Анимация камеры остановлена."), false);
                        // TODO: Реализовать остановку анимации
                        return 1;
                    }))
            );

            dispatcher.register(literal("cinematicanimations").executes(ctx -> {
                List<CinematicCameraResourceReloadListener.CameraAnimation> animations = CinematicCameraResourceReloadListener.getCachedAnimations();
                if (animations.isEmpty()) {
                    ctx.getSource().sendFeedback(Text.literal("Анимации не найдены."));
                } else {
                    for (CinematicCameraResourceReloadListener.CameraAnimation anim : animations) {
                        ctx.getSource().sendFeedback(Text.literal("Анимация: " + anim.getName()));
                        for (CinematicCameraResourceReloadListener.CameraAnimation.Keyframe kf : anim.getPositionKeyframes()) {
                            ctx.getSource().sendFeedback(Text.literal(String.format("Время: %.2f, Позиция: [%.2f, %.2f, %.2f], Интерполяция: %s",
                                    kf.time, kf.values[0], kf.values[1], kf.values[2], kf.interpolation)));
                        }
                        for (CinematicCameraResourceReloadListener.CameraAnimation.Keyframe kf : anim.getRotationKeyframes()) {
                            ctx.getSource().sendFeedback(Text.literal(String.format("Время: %.2f, Поворот: [%.2f, %.2f, %.2f], Интерполяция: %s",
                                    kf.time, kf.values[0], kf.values[1], kf.values[2], kf.interpolation)));
                        }
                    }
                }
                return 1;
            }));
        });
    }
}
