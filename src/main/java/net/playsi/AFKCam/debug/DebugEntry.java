package net.playsi.Afkcam.debug;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.playsi.Afkcam.client.AfkcamClient;

import java.awt.Color;

public class DebugEntry {
    private final String label;
    private String value;
    private boolean condition = true;

    public DebugEntry(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public DebugEntry setValue(String value) {
        this.value = value;
        return this;
    }

    public DebugEntry setCondition(boolean condition) {
        this.condition = condition;
        return this;
    }

    public void render(DrawContext context, int x, int y) {
        int color = condition ? Color.WHITE.getRGB() : Color.RED.getRGB();
        context.drawText(
                AfkcamClient.getMC().textRenderer,
                Text.literal(label + ": " + value),
                x, y, color, false
        );
    }
}
