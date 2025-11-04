package net.playsi.Afkcam.client.AFKmodeState;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.playsi.Afkcam.client.AfkcamClient;
import net.playsi.Afkcam.client.Animations.AnimationService;
import net.playsi.Afkcam.client.Animations.RawAnimation;
import net.playsi.Afkcam.client.Camera.CameraAnimationManager;
import net.playsi.Afkcam.client.Camera.CameraKeyframe;
import net.playsi.Afkcam.client.Camera.FreeCamManager;
import net.playsi.Afkcam.config.Config;
import net.playsi.Afkcam.utils.LogUtils;


import java.util.*;

/**
 * Основной менеджер AFK-камеры, который управляет автоматическим включением
 * и циклическим воспроизведением анимаций когда игрок неактивен
 */
public class AFKCamLoopState {

    private static final MinecraftClient MC = AfkcamClient.getMC();
    private static final Config CONFIG = Config.getInstance();
    private static final LogUtils LOGGER = new LogUtils(AFKCamLoopState.class);

    @Getter
    private static boolean isAfkModeActive = false;

    @Getter
    private static List<RawAnimation> loadedAnimations = new ArrayList<>();

    private static int currentAnimationIndex = 0;
    private static Random animationRandom = new Random();

//    private static ScreenFadeManager fadeManager = new ScreenFadeManager();

//    private static AnimationValidator validator = new AnimationValidator();

    public static void tick() {
        if (CONFIG.isModEnabled() || !isInWorld()) {
            if (!isAfkModeActive && AFKCondition.shouldActivateAfkMode()) {
                activateAfkMode();

            } else if (isAfkModeActive && !AFKCondition.hasAFKConditions()) {
                deactivateAfkMode();
            }
            if (isAfkModeActive) {
                tickAfkMode();
            }
//            if (CONFIG.isFade()) {
//              fadeManager.tick();
//            }
        }
    }


    public static void onRender(float tickDelta) {
//        fadeManager.render(tickDelta);
        if (HUDManager.isPlayerHudWhenAFK()) {
            MC.options.hudHidden = true;

            deactivateAfkMode();
            AFKCondition.resetLastActivityTime();
        }

        if (isAfkModeActive) {
            CameraAnimationManager.onRender(tickDelta);
        }
    }

    public static boolean isInWorld() {
        return MC.world != null && MC.player != null && !MC.isPaused();
    }

    /**
     * Активация AFK режима
     */
    private static void activateAfkMode() {
        LOGGER.infoDebug("Активация AFK режима камеры");

        loadAvailableAnimations(loadedAnimations);
        if (loadedAnimations.isEmpty()) {
            LOGGER.warn("No available animations!");
            return;
        }

        setAnimationsQueue();

        isAfkModeActive = true;

        if (!FreeCamManager.isEnabled()) {
            FreeCamManager.freecamToggle();
        }

//        if (CONFIG.isFade()){
//            hidePlayerHud();
//            startNextAnimation();
//        }else{
        //fadeManager.startFadeOut(() -> {
        HUDManager.hidePlayerHud();
        startNextAnimation();
        //});
//        }
    }

    /**
     * Деактивация AFK режима
     */
    private static void deactivateAfkMode() {
        LOGGER.infoDebug("Деактивация AFK режима камеры");

        isAfkModeActive = false;
        CameraAnimationManager.stopPlayback();
        HUDManager.restorePlayerHud();
        //fadeManager.startFadeOut(() -> {
        if (FreeCamManager.isEnabled()) {
            FreeCamManager.freecamToggle();
        }

        //fadeManager.startFadeIn(null);

        AFKCondition.resetLastActivityTime();
        //});
    }

    private static void tickAfkMode() {
        if (!CameraAnimationManager.isPlaying()) {
            startNextAnimation();
        }
    }

    private static void loadAvailableAnimations( List<RawAnimation> listToLoad) {
        listToLoad.clear();
        listToLoad.addAll(AnimationService.getInstance().getAllAnimations());
        LOGGER.infoDebug("Загружено " + listToLoad.size() + " доступных анимаций");
    }

    private static void setAnimationsQueue() {
        currentAnimationIndex = 0;
        Collections.shuffle(loadedAnimations, animationRandom);

    }

    /**
     * Запуск следующей анимации
     */
    private static void startNextAnimation() {
        if (loadedAnimations.isEmpty()) {
            LOGGER.warnDebug("No animations available for playback");
            deactivateAfkMode();
            return;
        }

        RawAnimation nextAnimation = loadedAnimations.get(currentAnimationIndex);
        currentAnimationIndex = (currentAnimationIndex + 1) % loadedAnimations.size();

        if (currentAnimationIndex == 0) {
            Collections.shuffle(loadedAnimations, animationRandom);
        }

        LOGGER.infoDebug("Start animation: " + nextAnimation.getName());

        convertAndLoadAnimation(nextAnimation);
        //TODO сначала кешировать потом проверять потом грузить, колбек при обновлении анимаций
        //TODO реализовать проверку анимаций

        // Начинаем проявление и запускаем анимацию
        //fadeManager.startFadeIn(() -> {
        CameraAnimationManager.startPlayback();
        //});
    }



    public static void onDisconnect() {
        deactivateAfkMode();
    }
}