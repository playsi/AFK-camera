package net.playsi.Afkcam.debug;

import net.playsi.Afkcam.client.AFKmodeState.AFKCamLoopState;
import net.playsi.Afkcam.client.Animations.RawAnimation;

import java.util.List;

public class DebugContent {
    public static void debug() {
        DebugInfoBuilder builder = new DebugInfoBuilder();

        List<RawAnimation> animations = AFKCamLoopState.getLoadedAnimations();

        builder.add("Total Animations", animations.size());

        for (int i = 0; i < animations.size(); i++) {
            RawAnimation anim = animations.get(i);
            builder.add("─────────────────", "");
            builder.add("Animation #" + (i + 1), anim.getName());

            // Show ALL position keyframes
            List<RawAnimation.Keyframe> posKeys = anim.getPositionKeyframes();
            for (int j = 0; j < posKeys.size(); j++) {
                RawAnimation.Keyframe kf = posKeys.get(j);
                builder.add("    Pos[" + j + "] t=" + kf.getTime(),
                        String.format("(%.2f, %.2f, %.2f) %s",
                                kf.getValues()[0], kf.getValues()[1], kf.getValues()[2],
                                kf.getInterpolation()));
            }

            // Show ALL rotation keyframes
            List<RawAnimation.Keyframe> rotKeys = anim.getRotationKeyframes();
            for (int j = 0; j < rotKeys.size(); j++) {
                RawAnimation.Keyframe kf = rotKeys.get(j);
                builder.add("    Rot[" + j + "] t=" + kf.getTime(),
                        String.format("(%.2f, %.2f, %.2f) %s",
                                kf.getValues()[0], kf.getValues()[1], kf.getValues()[2],
                                kf.getInterpolation()));
            }
        }

        DebugOverlayRenderer.update(builder);
    }
}
