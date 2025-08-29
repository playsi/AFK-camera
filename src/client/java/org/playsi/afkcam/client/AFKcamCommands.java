package org.playsi.afkcam.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import org.playsi.afkcam.client.AnimationsLogic.AnimationService;
import org.playsi.afkcam.client.RPsParser.AFKcamResourceReloadListener;
import org.playsi.afkcam.client.AnimationsLogic.Parser.RawAnimation;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class AFKcamCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("cinematicanimations")
                    .then(argument("animation", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                List<RawAnimation> animations = AnimationService.getInstance().getAllAnimations();
                                for (RawAnimation anim : animations) {
                                    builder.suggest(anim.getName());
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "animation");
                                List<RawAnimation> animations = AnimationService.getInstance().getAllAnimations();
                                for (RawAnimation anim : animations) {
                                    if (anim.getName().equals(name)) {
                                        ctx.getSource().sendFeedback(Text.literal("Animation: " + anim.getName()));
                                        for (RawAnimation.Keyframe kf : anim.getPositionKeyframes()) {
                                            ctx.getSource().sendFeedback(Text.literal(String.format("Time: %.2f, Position: [%.2f, %.2f, %.2f], Interpolation: %s",
                                                    kf.getTime(), kf.getValues()[0], kf.getValues()[1], kf.getValues()[2], kf.getInterpolation())));
                                        }
                                        for (RawAnimation.Keyframe kf : anim.getRotationKeyframes()) {
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
                        List<RawAnimation> animations = AnimationService.getInstance().getAllAnimations();
                        if (animations.isEmpty()) {
                            ctx.getSource().sendFeedback(Text.literal("No animations found."));
                        } else {
                            for (RawAnimation anim : animations) {
                                ctx.getSource().sendFeedback(Text.literal("Animation: " + anim.getName()));
                                for (RawAnimation.Keyframe kf : anim.getPositionKeyframes()) {
                                    ctx.getSource().sendFeedback(Text.literal(String.format("Time: %.2f, Position: [%.2f, %.2f, %.2f], Interpolation: %s",
                                            kf.getTime(), kf.getValues()[0], kf.getValues()[1], kf.getValues()[2], kf.getInterpolation())));
                                }
                                for (RawAnimation.Keyframe kf : anim.getRotationKeyframes()) {
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
