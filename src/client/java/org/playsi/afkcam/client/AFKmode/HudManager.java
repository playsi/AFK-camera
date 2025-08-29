package org.playsi.afkcam.client.AFKmode;

import net.minecraft.client.MinecraftClient;
import org.playsi.afkcam.client.AfkcamClient;

public class HudManager {
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
}
