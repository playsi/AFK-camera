package net.playsi.Afkcam.client.AFKmodeState.cameraCollide;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.playsi.Afkcam.client.AfkcamClient;

public class AnimationCollideManager {
    @Setter
    @Getter
    private static boolean animationListInvalid = true;

    private static boolean playerPositionInvalid = true;
    private static final MinecraftClient MC = AfkcamClient.getMC();

    public static boolean isPlayerPositionSame(){
        return( Math.abs(MC.player.lastX - MC.player.getX()) <= 0.5 ||
                Math.abs(MC.player.lastY - MC.player.getY()) <= 0.5 ||
                Math.abs(MC.player.lastZ - MC.player.getZ()) <= 0.5);
    }

}
