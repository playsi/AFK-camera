package net.playsi.Afkcam.client.AFKmodeState;

import net.minecraft.client.MinecraftClient;
import net.playsi.Afkcam.client.AfkcamClient;

public class HUDManager {
    private static final MinecraftClient MC = AfkcamClient.getMC();

    private static boolean wasHudHidden = false;

    public static void hidePlayerHud() {
        if (MC.options != null) {
            wasHudHidden = MC.options.hudHidden;
            MC.options.hudHidden = true;
        }
    }
    public static void restorePlayerHud() {
        if (MC.options != null) {
            MC.options.hudHidden = wasHudHidden;
        }
    }
    public static boolean isPlayerHudWhenAFK(){
        return AFKCamLoopState.isAfkModeActive() && !MC.options.hudHidden;
    }
}
