package net.playsi.Afkcam.debug;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

//? if < 1.21.8
import net.minecraft.client.render.RenderTickCounter;

import java.util.Map;

public class DebugOverlayRenderer implements HudRenderCallback {

    private static Map<String, DebugEntry> currentEntries = Map.of();

    public static void update(DebugInfoBuilder builder) {
        currentEntries = builder.build();
    }

    @Override
    //? if < 1.21.8 {
    //public void onHudRender(DrawContext context, float tickDelta) {
        //? }else
        public void onHudRender(DrawContext context, RenderTickCounter renderTickCounter) {
        int x = 5;
        int y = 5;
        for (DebugEntry entry : currentEntries.values()) {
            entry.render(context, x, y);
            y += 10;
        }
    }

    public static void register() {
        HudRenderCallback.EVENT.register(new DebugOverlayRenderer());
    }
}
