package org.playsi.afkcam.client;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import org.playsi.afkcam.client.Camera.CameraAnimationManager;
import org.playsi.afkcam.client.Camera.CameraKeyframe;
import org.playsi.afkcam.client.Camera.FreeCamManager;
import org.playsi.afkcam.client.RPsParser.CinematicCameraResourceReloadListener;
import org.playsi.afkcam.client.Utils.LogUtils;
import org.playsi.afkcam.client.Utils.ParsedAnimation;
import org.playsi.afkcam.client.config.Config;

import java.util.*;

/**
 * Основной менеджер AFK-камеры, который управляет автоматическим включением
 * и циклическим воспроизведением анимаций когда игрок неактивен
 */
public class AFKcameraManager {

    private static final MinecraftClient MC = AfkcamClient.getMC();
    private static final Config CONFIG = AfkcamClient.getConfig();
    private static final LogUtils LOGGER = new LogUtils(AFKcameraManager.class);

//    private static final long AFK_TIMEOUT_MS = 5 * 60 * 1000 ; // 5 минут в миллисекундах
//    private static final float FADE_IN_DURATION = 0.5f; // Длительность затухания в секундах
//    private static final float FADE_OUT_DURATION = 0.5f;
//    private static final float MIN_ANIMATION_DURATION = FADE_IN_DURATION + FADE_OUT_DURATION + 1.0f; // Минимальная длительность анимации (1с + 2*0.5с затухание)

    private static final float SCALE_FACTOR = 1.0f / 16.0f;

    @Getter
    private static boolean isAfkModeActive = false;

    @Setter
    private static boolean playerTakeDamage = false;

    private static long lastActivityTime = System.currentTimeMillis();
    private static boolean wasHudHidden = false;
    private static boolean wasInMenu = false;

    private static double prevMouseX;
    private static double prevMouseY;

    private static List<ParsedAnimation> availableAnimations = new ArrayList<>();
    private static int currentAnimationIndex = 0;
    private static Random animationRandom = new Random();

//    private static ScreenFadeManager fadeManager = new ScreenFadeManager();

//    private static AnimationValidator validator = new AnimationValidator();

    public static void tick() {
    if (CONFIG.isModEnabled() || !isInWorld()) {
            if (!isAfkModeActive && shouldActivateAfkMode()) {
                activateAfkMode();
            } else if (isAfkModeActive && hasPlayerActivity()) {
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

    /**
     * Рендер метод - вызывать каждый кадр
     */
    public static void onRender(float tickDelta) {
//        fadeManager.render(tickDelta);

        if (isAfkModeActive) {
            CameraAnimationManager.onRender(tickDelta);
        }
    }

    /**
     * Проверка, нужно ли активировать AFK режим
     */
    private static boolean shouldActivateAfkMode() {
        if (!isInWorld()) return false;
        if (isAfkModeActive) return false;
        if (hasPlayerActivity()){
            lastActivityTime = System.currentTimeMillis();
        }
        long timeSinceActivity = System.currentTimeMillis() - lastActivityTime;
        return timeSinceActivity >= (long) CONFIG.getActivationAfter() * 1000L;
    }

    private static boolean hasPlayerActivity() {
        if (playerTakeDamage) {
            playerTakeDamage = false;
            return true;
        }

        if (CONFIG.isDisableOnDeath() && MC.player.isDead()) {
            return true;
        }

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

    private static boolean isInWorld() {
        return MC.world != null && MC.player != null && !MC.isPaused();
    }

    /**
     * Активация AFK режима
     */
    private static void activateAfkMode() {
        LOGGER.infoDebug("Активация AFK режима камеры");

        loadAvailableAnimations();
        if (availableAnimations.isEmpty()) {
            LOGGER.warn("Нет доступных анимаций для AFK режима");
            return;
        }

        isAfkModeActive = true;

        if (!FreeCamManager.isEnabled()) {
            FreeCamManager.freecamToggle();
        }

//        if (CONFIG.isFade()){
//            hidePlayerHud();
//            startNextAnimation();
//        }else{
            //fadeManager.startFadeOut(() -> {
            hidePlayerHud();
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
        restorePlayerHud();
        //fadeManager.startFadeOut(() -> {
            if (FreeCamManager.isEnabled()) {
                FreeCamManager.freecamToggle();
            }

            //fadeManager.startFadeIn(null);

            lastActivityTime = System.currentTimeMillis();
        //});
    }

    /**
     * Тик AFK режима
     */
    private static void tickAfkMode() {
        if (!CameraAnimationManager.isPlaying()) {
            startNextAnimation();
        }
    }

    private static void loadAvailableAnimations() {
        availableAnimations.clear();

        List<ParsedAnimation> allAnimations = CinematicCameraResourceReloadListener.getCachedAnimations();
        currentAnimationIndex = 0;
// for feature
//        for (ParsedAnimation animation : allAnimations) {
//            if (validator.isAnimationValid(animation)) {
//                ParsedAnimation trimmedAnimation = validator.trimAnimationForCollisions(animation);
//                if (trimmedAnimation != null) {
//                    availableAnimations.add(trimmedAnimation);
//                }
//            }
//        }

        availableAnimations = new ArrayList<>(allAnimations);
        LOGGER.infoDebug("Загружено " + availableAnimations.size() + " доступных анимаций");

        Collections.shuffle(availableAnimations, animationRandom);
    }

    /**
     * Запуск следующей анимации
     */
    private static void startNextAnimation() {
        if (availableAnimations.isEmpty()) {
            LOGGER.warnDebug("No animations available for playback");
            deactivateAfkMode();
            return;
        }

        ParsedAnimation nextAnimation = availableAnimations.get(currentAnimationIndex);
        currentAnimationIndex = (currentAnimationIndex + 1) % availableAnimations.size();

        if (currentAnimationIndex == 0) {
            Collections.shuffle(availableAnimations, animationRandom);
        }

        LOGGER.infoDebug("Start animation: " + nextAnimation.getName());

        convertAndLoadAnimation(nextAnimation);

        // Начинаем проявление и запускаем анимацию
        //fadeManager.startFadeIn(() -> {
        CameraAnimationManager.startPlayback();
        //});
    }


    /**
     * Конвертация ParsedAnimation в формат CameraAnimationManager
     */
    private static void convertAndLoadAnimation(ParsedAnimation animation) {
        CameraAnimationManager.clearKeyframes();

        List<ParsedAnimation.Keyframe> posFrames = animation.getPositionKeyframes();
        List<ParsedAnimation.Keyframe> rotFrames = animation.getRotationKeyframes();

        Map<Float, ParsedAnimation.Keyframe> rotationMap = new HashMap<>();
        for (ParsedAnimation.Keyframe rotFrame : rotFrames) {
            rotationMap.put(rotFrame.getTime(), rotFrame);
        }

        for (ParsedAnimation.Keyframe posFrame : posFrames) {
            float time = posFrame.getTime();
            float[] position = posFrame.getValues();

            float[] rotation = findOrInterpolateRotation(rotFrames, time);

            CameraKeyframe.InterpolationType interpType = CameraKeyframe.InterpolationType.LINEAR;

            CameraAnimationManager.addKeyframe(
                    time,
                    position[0] * SCALE_FACTOR, position[1] * SCALE_FACTOR, position[2] * SCALE_FACTOR,
                    rotation[1], rotation[0],
                    interpType
            );
        }

        for (ParsedAnimation.Keyframe rotFrame : rotFrames) {
            float time = rotFrame.getTime();
            if (posFrames.stream().noneMatch(pf -> Math.abs(pf.getTime() - time) < 0.001f)) {
                float[] position = findOrInterpolatePosition(posFrames, time);
                float[] rotation = rotFrame.getValues();

                CameraAnimationManager.addKeyframe(
                        time,
                        position[0] * SCALE_FACTOR, position[1] * SCALE_FACTOR, position[2] * SCALE_FACTOR,
                        rotation[1], rotation[0], // yaw, pitch
                        CameraKeyframe.InterpolationType.LINEAR
                );
            }
        }
    }

    private static float[] findOrInterpolateRotation(List<ParsedAnimation.Keyframe> rotFrames, float time) {
        if (rotFrames.isEmpty()) return new float[]{0, 0};

        for (ParsedAnimation.Keyframe frame : rotFrames) {
            if (Math.abs(frame.getTime() - time) < 0.001f) {
                return frame.getValues();
            }
        }

        ParsedAnimation.Keyframe prevFrame = null;
        ParsedAnimation.Keyframe nextFrame = null;

        for (ParsedAnimation.Keyframe frame : rotFrames) {
            if (frame.getTime() <= time) {
                prevFrame = frame;
            } else if (nextFrame == null) {
                nextFrame = frame;
                break;
            }
        }

        if (prevFrame == null) return rotFrames.get(0).getValues();
        if (nextFrame == null) return rotFrames.get(rotFrames.size() - 1).getValues();

        float t = (time - prevFrame.getTime()) / (nextFrame.getTime() - prevFrame.getTime());
        float[] prevRot = prevFrame.getValues();
        float[] nextRot = nextFrame.getValues();

        return new float[]{
                prevRot[0] + (nextRot[0] - prevRot[0]) * t, // pitch
                prevRot[1] + (nextRot[1] - prevRot[1]) * t  // yaw
        };
    }

    private static float[] findOrInterpolatePosition(List<ParsedAnimation.Keyframe> posFrames, float time) {
        if (posFrames.isEmpty()) return new float[]{0, 0, 0};

        for (ParsedAnimation.Keyframe frame : posFrames) {
            if (Math.abs(frame.getTime() - time) < 0.001f) {
                float[] values = frame.getValues();
                return new float[]{
                        values[0] * SCALE_FACTOR,
                        values[1] * SCALE_FACTOR,
                        values[2] * SCALE_FACTOR
                };
            }
        }

        ParsedAnimation.Keyframe prevFrame = null;
        ParsedAnimation.Keyframe nextFrame = null;

        for (ParsedAnimation.Keyframe frame : posFrames) {
            if (frame.getTime() <= time) {
                prevFrame = frame;
            } else if (nextFrame == null) {
                nextFrame = frame;
                break;
            }
        }

        if (prevFrame == null) {
            float[] values = posFrames.get(0).getValues();
            return new float[]{
                    values[0] * SCALE_FACTOR,
                    values[1] * SCALE_FACTOR,
                    values[2] * SCALE_FACTOR
            };
        }
        if (nextFrame == null) {
            float[] values = posFrames.get(posFrames.size() - 1).getValues();
            return new float[]{
                    values[0] * SCALE_FACTOR,
                    values[1] * SCALE_FACTOR,
                    values[2] * SCALE_FACTOR
            };
        }

        float t = (time - prevFrame.getTime()) / (nextFrame.getTime() - prevFrame.getTime());
        float[] prevPos = prevFrame.getValues();
        float[] nextPos = nextFrame.getValues();

        // Интерполируем и масштабируем
        return new float[]{
                (prevPos[0] + (nextPos[0] - prevPos[0]) * t) * SCALE_FACTOR,
                (prevPos[1] + (nextPos[1] - prevPos[1]) * t) * SCALE_FACTOR,
                (prevPos[2] + (nextPos[2] - prevPos[2]) * t) * SCALE_FACTOR
        };
    }

    private static void hidePlayerHud() {
        if (MC.options != null) {
            wasHudHidden = MC.options.hudHidden;
            MC.options.hudHidden = true;
        }
    }
    private static void restorePlayerHud() {
        if (MC.options != null) {
            MC.options.hudHidden = wasHudHidden;
        }
    }

    public static void onDisconnect() {
        deactivateAfkMode();
    }
}