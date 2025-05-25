package org.playsi.afkcam.client.Camera;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.Utils.LogUtils;

public class FreeCamManager {

    private static final double FIRST_PERSON_OFFSET = 0.4;
    private static final double THIRD_PERSON_OFFSET = -4.0;
    private static final double COLLISION_CHECK_INCREMENT = 0.1;
    private static final int FREECAM_ENTITY_ID = -420;

    private static final MinecraftClient MC = AfkcamClient.getMC();
    private static final LogUtils LOGGER = new LogUtils(FreeCamManager.class);

    @Getter
    private static FreeCamera freeCamera;
    private static boolean freecamEnabled = false;
    private static boolean disableNextTick = false;
    private static Perspective rememberedPerspective = Perspective.FIRST_PERSON;

    public static void toggleFreecam() {
        if (!canToggleFreecam()) {
            return;
        }

        freecamToggle();
    }

    public static void enableFreecam() {
        if (!freecamEnabled && canToggleFreecam()) {
            freecamToggle();
        }
    }

    public static void disableFreecam() {
        if (freecamEnabled) {
            freecamToggle();
        }
    }

    private static boolean canToggleFreecam() {
        return MC.player != null && MC.world != null && !MC.isPaused();
    }

    // Методы для внешнего управления камерой
    public static void teleportFreecam(double x, double y, double z) {
        if (freeCamera != null) {
            freeCamera.applyPosition(x, y, z, freeCamera.getYaw(), freeCamera.getPitch());
        }
    }

    public static void teleportFreecam(double x, double y, double z, float yaw, float pitch) {
        if (freeCamera != null) {
            freeCamera.applyPosition(x, y, z, yaw, pitch);
        }
    }

    public static void preTick(MinecraftClient mc) {
        if (disableNextTick && isEnabled()) {
            freecamToggle();
        }
        disableNextTick = false;

        if (isEnabled()) {
            mc.gameRenderer.setRenderHand(false);
        }
    }

    public static void onDisconnect() {
        if (isEnabled()) {
            // Принудительно отключаем камеру при отключении от сервера
            freecamEnabled = true; // Устанавливаем true чтобы freecamToggle() корректно отработал
            freecamToggle();
        }
    }

    public static void freecamToggle() {
        if (freecamEnabled) {
            onDisableFreecam();
        } else {
            onEnableFreecam();
        }
        freecamEnabled = !freecamEnabled;
    }

    public static void freecamToggle(boolean active) {
        if (active && !freecamEnabled) {
            onEnableFreecam();
            freecamEnabled = true;
        } else if (!active && freecamEnabled) {
            onDisableFreecam();
            freecamEnabled = false;
        }
    }

    private static void onEnableFreecam() {
        if (MC.player == null || MC.world == null) {
            LOGGER.infoDebug("Cannot enable freecam: player or world is null");
            return;
        }

        onEnable();

        // Создаем камеру с уникальным ID
        freeCamera = new FreeCamera(FREECAM_ENTITY_ID);
        moveToPlayer();
        freeCamera.spawn();

        // Устанавливаем камеру как активную
        MC.setCameraEntity(freeCamera);

        LOGGER.infoDebug("Freecam enabled");
    }

    private static void onDisableFreecam() {
        LOGGER.infoDebug("Disabling freecam");

        // Сначала возвращаем камеру на игрока
        if (MC.player != null) {
            MC.setCameraEntity(MC.player);
        }

        // Затем очищаем freecam
        if (freeCamera != null) {
            freeCamera.despawn();
            freeCamera = null;
        }

        onDisable();
        LOGGER.infoDebug("Freecam disabled");
    }

    private static void onEnable() {
        MC.chunkCullingEnabled = false;
        MC.gameRenderer.setRenderHand(false);

        rememberedPerspective = MC.options.getPerspective();
        MC.options.setPerspective(Perspective.FIRST_PERSON);
    }

    private static void onDisable() {
        MC.chunkCullingEnabled = true;
        MC.gameRenderer.setRenderHand(true);

        // Восстанавливаем сохраненную перспективу
        if (rememberedPerspective != null) {
            MC.options.setPerspective(rememberedPerspective);
        }
    }

    public static void moveToPlayer() {
        if (freeCamera == null || MC.player == null) {
            return;
        }

        freeCamera.copyPosition(MC.player);
        freeCamera.applyPerspective(Perspective.FIRST_PERSON, true);
    }

    public static void disableNextTick() {
        disableNextTick = true;
    }

    public static boolean isEnabled() {
        return freecamEnabled;
    }
}