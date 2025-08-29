package org.playsi.afkcam.client.AFKmode;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.AnimationsLogic.AnimationService;
import org.playsi.afkcam.client.Camera.CameraAnimationManager;
import org.playsi.afkcam.client.Camera.CameraKeyframe;
import org.playsi.afkcam.client.Camera.FreeCamManager;
import org.playsi.afkcam.client.Utils.LogUtils;
import org.playsi.afkcam.client.AnimationsLogic.Parser.RawAnimation;
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

    private static final float SCALE_FACTOR = 1.0f / 16.0f;

    @Getter
    private static boolean isAfkModeActive = false;

    private static long lastActivityTime = System.currentTimeMillis();


    private static List<RawAnimation> availableAnimations = new ArrayList<>();
    private static int currentAnimationIndex = 0;
    private static Random animationRandom = new Random();

//    private static ScreenFadeManager fadeManager = new ScreenFadeManager();

//    private static AnimationValidator validator = new AnimationValidator();

    public static void tick() {
    if (CONFIG.isModEnabled() || !isInWorld()) {
            if (!isAfkModeActive && shouldActivateAfkMode()) {
                activateAfkMode();
            } else if (isAfkModeActive && !AFKConditions.hasAFKConditions()) {
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
        if (!AFKConditions.hasAFKConditions()){
            lastActivityTime = System.currentTimeMillis();
        }
        long timeSinceActivity = System.currentTimeMillis() - lastActivityTime;
        return timeSinceActivity >= (long) CONFIG.getActivationAfter() * 1000L;
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
            HudManager.hidePlayerHud();
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
        HudManager.restorePlayerHud();
        //fadeManager.startFadeOut(() -> {
            if (FreeCamManager.isEnabled()) {
                FreeCamManager.freecamToggle();
            }

            //fadeManager.startFadeIn(null);

            lastActivityTime = System.currentTimeMillis();
        //});
    }

    private static void tickAfkMode() {
        if (!CameraAnimationManager.isPlaying()) {
            startNextAnimation();
        }
    }

    private static void loadAvailableAnimations() {
        availableAnimations.clear();

        //List<RawAnimation> allAnimations = AFKcamResourceReloadListener.getCachedAnimations();
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

        availableAnimations = new ArrayList<>(AnimationService.getInstance().getAllAnimations());
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

        RawAnimation nextAnimation = availableAnimations.get(currentAnimationIndex);
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
    private static void convertAndLoadAnimation(RawAnimation animation) {
        CameraAnimationManager.clearKeyframes();

        List<RawAnimation.Keyframe> posFrames = animation.getPositionKeyframes();
        List<RawAnimation.Keyframe> rotFrames = animation.getRotationKeyframes();

        Map<Float, RawAnimation.Keyframe> rotationMap = new HashMap<>();
        for (RawAnimation.Keyframe rotFrame : rotFrames) {
            rotationMap.put(rotFrame.getTime(), rotFrame);
        }

        for (RawAnimation.Keyframe posFrame : posFrames) {
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

        for (RawAnimation.Keyframe rotFrame : rotFrames) {
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

    private static float[] findOrInterpolateRotation(List<RawAnimation.Keyframe> rotFrames, float time) {
        if (rotFrames.isEmpty()) return new float[]{0, 0};

        for (RawAnimation.Keyframe frame : rotFrames) {
            if (Math.abs(frame.getTime() - time) < 0.001f) {
                return frame.getValues();
            }
        }

        RawAnimation.Keyframe prevFrame = null;
        RawAnimation.Keyframe nextFrame = null;

        for (RawAnimation.Keyframe frame : rotFrames) {
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

    private static float[] findOrInterpolatePosition(List<RawAnimation.Keyframe> posFrames, float time) {
        if (posFrames.isEmpty()) return new float[]{0, 0, 0};

        for (RawAnimation.Keyframe frame : posFrames) {
            if (Math.abs(frame.getTime() - time) < 0.001f) {
                float[] values = frame.getValues();
                return new float[]{
                        values[0] * SCALE_FACTOR,
                        values[1] * SCALE_FACTOR,
                        values[2] * SCALE_FACTOR
                };
            }
        }

        RawAnimation.Keyframe prevFrame = null;
        RawAnimation.Keyframe nextFrame = null;

        for (RawAnimation.Keyframe frame : posFrames) {
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

    public static void onDisconnect() {
        deactivateAfkMode();
    }
}