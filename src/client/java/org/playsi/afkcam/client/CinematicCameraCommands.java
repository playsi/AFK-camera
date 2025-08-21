package org.playsi.afkcam.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.playsi.afkcam.client.RPsParser.CinematicCameraResourceReloadListener;
import org.playsi.afkcam.client.Utils.ParsedAnimation;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static org.playsi.afkcam.client.Camera.FreeCamManager.freecamToggle;

public class CinematicCameraCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("cinematiccamera")
                    .then(literal("start").executes(ctx -> {
                        MinecraftClient.getInstance().player.sendMessage(Text.literal("toggle"), false);
                        freecamToggle();
                        return 1;
                    }))
            );

            dispatcher.register(literal("cinematicanimations")
                    .then(argument("animation", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                List<ParsedAnimation> animations = CinematicCameraResourceReloadListener.getCachedAnimations();
                                for (ParsedAnimation anim : animations) {
                                    builder.suggest(anim.getName());
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "animation");
                                List<ParsedAnimation> animations = CinematicCameraResourceReloadListener.getCachedAnimations();
                                for (ParsedAnimation anim : animations) {
                                    if (anim.getName().equals(name)) {
                                        ctx.getSource().sendFeedback(Text.literal("Animation: " + anim.getName()));
                                        for (ParsedAnimation.Keyframe kf : anim.getPositionKeyframes()) {
                                            ctx.getSource().sendFeedback(Text.literal(String.format("Time: %.2f, Position: [%.2f, %.2f, %.2f], Interpolation: %s",
                                                    kf.getTime(), kf.getValues()[0], kf.getValues()[1], kf.getValues()[2], kf.getInterpolation())));
                                        }
                                        for (ParsedAnimation.Keyframe kf : anim.getRotationKeyframes()) {
                                            ctx.getSource().sendFeedback(Text.literal(String.format("Time: %.2f, Rotation: [%.2f, %.2f, %.2f], Interpolation: %s",
                                                    kf.getTime(), kf.getValues()[0], kf.getValues()[1], kf.getValues()[2], kf.getInterpolation())));
                                        }
                                        return 1;
                                    }
                                }
                                ctx.getSource().sendFeedback(Text.literal("Animation not found: " + name));
                                return 0;
                            })
                    )
                    .executes(ctx -> {
                        List<ParsedAnimation> animations = CinematicCameraResourceReloadListener.getCachedAnimations();
                        if (animations.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("No animations found."));
                        } else {
                            for (ParsedAnimation anim : animations) {
                                ctx.getSource().sendFeedback(Text.literal("Animation: " + anim.getName()));
                                for (ParsedAnimation.Keyframe kf : anim.getPositionKeyframes()) {
                                    ctx.getSource().sendFeedback(Text.literal(String.format("Time: %.2f, Position: [%.2f, %.2f, %.2f], Interpolation: %s",
                                            kf.getTime(), kf.getValues()[0], kf.getValues()[1], kf.getValues()[2], kf.getInterpolation())));
                                }
                                for (ParsedAnimation.Keyframe kf : anim.getRotationKeyframes()) {
                                    ctx.getSource().sendFeedback(Text.literal(String.format("Time: %.2f, Rotation: [%.2f, %.2f, %.2f], Interpolation: %s",
                                            kf.getTime(), kf.getValues()[0], kf.getValues()[1], kf.getValues()[2], kf.getInterpolation())));
                                }
                            }
                        }
                        return 1;
                    })
            );
        });
    }
}
