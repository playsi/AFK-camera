package net.playsi.Afkcam.client.AFKmodeState;

import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.playsi.Afkcam.client.AfkcamClient;
import net.playsi.Afkcam.config.Config;
import org.lwjgl.glfw.GLFW;

public class AFKCondition {
    private static final MinecraftClient MC = AfkcamClient.getMC();
    private static final Config CONFIG = Config.getInstance();

    private static double prevMouseX;
    private static double prevMouseY;

    @Setter
    private static float playerLastHp = 0.0f;

    public static boolean hasAFKConditions() {
        if (MC.player == null) return false;

        if (playerTakeDamage(playerLastHp)){
            playerLastHp = MC.player.getHealth();
            return false;
        }

        playerLastHp = MC.player.getHealth();

        if (CONFIG.isDisableOnDeath() && MC.player.isDead()) {
            return false;
        }
        return !hasPlayerActivity();
    }

    public static boolean playerTakeDamage(float playerLastHp){
        return MC.player.getHealth() < playerLastHp;
    }

    public static boolean hasPlayerActivity() {
        if (prevMouseX != MC.mouse.getX() || prevMouseY != MC.mouse.getY()){
            prevMouseX = MC.mouse.getX();
            prevMouseY = MC.mouse.getY();
            return true;
        }

        return  MC.options.forwardKey.isPressed()           ||
                MC.options.backKey.isPressed()              ||
                MC.options.rightKey.isPressed()             ||
                MC.options.leftKey.isPressed()              ||
                MC.options.inventoryKey.isPressed()         ||
                MC.options.sprintKey.isPressed()            ||
                MC.options.jumpKey.isPressed()              ||
                MC.options.sneakKey.isPressed()             ||
                MC.options.useKey.isPressed()               ||
                MC.options.attackKey.isPressed()            ||
                MC.options.playerListKey.isPressed()        ||
                MC.options.togglePerspectiveKey.isPressed() ;
    }

}
