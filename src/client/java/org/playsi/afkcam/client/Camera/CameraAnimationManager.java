package org.playsi.afkcam.client.Camera;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.Utils.LogUtils;

import java.util.*;

public class CameraAnimationManager {

    private static final MinecraftClient MC = AfkcamClient.getMC();
    private static final LogUtils LOGGER = new LogUtils(CameraAnimationManager.class);

    @Getter
    private static final List<CameraKeyframe> keyframes = new ArrayList<>();

    private static boolean isPlaying = false;
    private static double currentTime = 0.0;
    private static long lastUpdateTime = 0L; // Для точного расчета времени

    private static double basePlayerX = 0.0;
    private static double basePlayerY = 0.0;
    private static double basePlayerZ = 0.0;
    private static double basePlayerYaw = 0.0;

    // Кэш для уменьшения вычислений
    private static double lastPlayerYaw = Double.NaN;
    private static double cachedCos = 0.0;
    private static double cachedSin = 0.0;

    // Сглаживание позиции
    private static FreecamPosition lastPosition = null;
    private static final double SMOOTHING_FACTOR = 0.15; // Коэффициент сглаживания

    public static void addKeyframe(double time, double x, double y, double z, double yaw, double pitch,
                                   CameraKeyframe.InterpolationType interpolation) {
        CameraKeyframe keyframe = new CameraKeyframe(time, x, y, z, yaw, pitch, interpolation);

        int insertIndex = 0;
        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i).getTime() > time) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }

        keyframes.add(insertIndex, keyframe);

        if (AfkcamClient.getConfig().isDebugLogEnabled()) {
            LOGGER.infoDebug("Keyframe added at " + time + "seconds: " + keyframe);
        }
    }

    public static boolean removeKeyframe(int index) {
        if (index >= 0 && index < keyframes.size()) {
            CameraKeyframe removed = keyframes.remove(index);
            if (AfkcamClient.getConfig().isDebugLogEnabled()) {
                LOGGER.infoDebug("Keyframe deleted: " + removed);
            }
            return true;
        }
        return false;
    }

    public static boolean removeKeyframeAtTime(double time) {
        for (int i = 0; i < keyframes.size(); i++) {
            if (Math.abs(keyframes.get(i).getTime() - time) < 0.001) {
                return removeKeyframe(i);
            }
        }
        return false;
    }

    public static void clearKeyframes() {
        keyframes.clear();
        lastPosition = null; // Сбрасываем кэш позиции
        if (AfkcamClient.getConfig().isDebugLogEnabled()) {
            LOGGER.infoDebug("All key frames have been cleared");
        }
    }

    public static void startPlayback() {
        if (keyframes.isEmpty()) {
            LOGGER.infoDebug("No keyframes for playback");
            return;
        }

        if (MC.player == null) {
            LOGGER.infoDebug("No player to play the animation");
            return;
        }

        if (!FreeCamManager.isEnabled()) {
            FreeCamManager.freecamToggle();
        }

        PlayerEntity player = MC.player;
        basePlayerX = player.getX();
        basePlayerY = player.getY();
        basePlayerZ = player.getZ();
        basePlayerYaw = player.getYaw();

        isPlaying = true;
        currentTime = 0.0;
        lastUpdateTime = System.nanoTime();
        lastPosition = null; // Сбрасываем кэш позиции
        lastPlayerYaw = Double.NaN; // Сбрасываем кэш угла

        LOGGER.infoDebug("Playback of the camera animation relative to the player's position has started:"
                + basePlayerX + ", " + basePlayerY + ", " + basePlayerZ + " (yaw: " + basePlayerYaw + ")");
    }

    public static void stopPlayback() {
        isPlaying = false;
        lastPosition = null;
        lastPlayerYaw = Double.NaN;
        LOGGER.infoDebug("The camera animation playback has stopped");
    }

    public static void togglePlayback() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    public static void seekToTime(double time) {
        currentTime = Math.max(0, time);
        lastPosition = null; // Сбрасываем кэш при перемотке
        updateCameraPosition();
    }

    public static void onRender(float tickDelta) {
        if (!isPlaying || keyframes.isEmpty() || FreeCamManager.getFreeCamera() == null || MC.player == null) {
            return;
        }

        long currentNanoTime = System.nanoTime();
        if (lastUpdateTime > 0) {
            double realDeltaTime = (currentNanoTime - lastUpdateTime) / 1_000_000_000.0;
            currentTime += realDeltaTime;
        }
        lastUpdateTime = currentNanoTime;

        double maxTime = keyframes.get(keyframes.size() - 1).getTime();
        if (currentTime > maxTime) {
            currentTime = maxTime;
            stopPlayback();
            return;
        }

        updateCameraPosition();
    }

    private static void updateCameraPosition() {
        if (keyframes.isEmpty() || MC.player == null) return;

        FreecamPosition relativePosition = interpolatePosition(currentTime);
        if (relativePosition == null) return;

        FreecamPosition absolutePosition;
        if (AfkcamClient.getConfig().isCameraFollow()) {
            absolutePosition = convertToAbsolutePosition(relativePosition);
        } else {
            absolutePosition = convertToAbsolutePositionStatic(relativePosition);
        }

        // Применяем сглаживание для уменьшения джиттера
        if (lastPosition != null && SMOOTHING_FACTOR > 0 &&
                !absolutePosition.positionEquals(lastPosition, 0.001)) {
            absolutePosition = smoothPosition(lastPosition, absolutePosition, SMOOTHING_FACTOR);
        }

        lastPosition = new FreecamPosition(); // Создаем новую копию
        lastPosition.setPosition(absolutePosition.getX(), absolutePosition.getY(), absolutePosition.getZ());
        lastPosition.setRotation(absolutePosition.getYaw(), absolutePosition.getPitch());

        FreeCamManager.getFreeCamera().applyPosition(absolutePosition);
    }

    // Сглаживание позиции для устранения джиттера
    private static FreecamPosition smoothPosition(FreecamPosition prev, FreecamPosition current, double factor) {
        FreecamPosition smoothed = new FreecamPosition();

        smoothed.setX(lerp(prev.getX(), current.getX(), factor));
        smoothed.setY(lerp(prev.getY(), current.getY(), factor));
        smoothed.setZ(lerp(prev.getZ(), current.getZ(), factor));

        smoothed.setRotation(
                (float) lerpAngle(prev.getYaw(), current.getYaw(), factor),
                (float) lerpAngle(prev.getPitch(), current.getPitch(), factor)
        );

        return smoothed;
    }

    private static FreecamPosition convertToAbsolutePosition(FreecamPosition relativePosition) {
        PlayerEntity player = MC.player;
        double currentPlayerX = player.getX();
        double currentPlayerY = player.getY();
        double currentPlayerZ = player.getZ();

        double currentPlayerYaw = player.getYaw();

        // Кэшируем тригонометрические вычисления
        if (Double.isNaN(lastPlayerYaw) || Math.abs(currentPlayerYaw - lastPlayerYaw) > 0.01) {
            double radianYaw = Math.toRadians(currentPlayerYaw);
            cachedCos = Math.cos(radianYaw);
            cachedSin = Math.sin(radianYaw);
            lastPlayerYaw = currentPlayerYaw;
        }

        double relX = relativePosition.getX();
        double relZ = relativePosition.getZ();

        double rotatedX = relX * cachedCos + relZ * cachedSin;
        double rotatedZ = relX * cachedSin - relZ * cachedCos;

        FreecamPosition absolutePosition = new FreecamPosition();

        absolutePosition.setX(currentPlayerX + rotatedX);
        absolutePosition.setY(currentPlayerY + relativePosition.getY());
        absolutePosition.setZ(currentPlayerZ + rotatedZ);

        absolutePosition.setRotation(
                (float) (player.getYaw() + relativePosition.getYaw()),
                relativePosition.getPitch()
        );

        return absolutePosition;
    }

    // Новый метод для статичной позиции (когда follow отключен)
    private static FreecamPosition convertToAbsolutePositionStatic(FreecamPosition relativePosition) {
        double radianYaw = Math.toRadians(basePlayerYaw);
        double cos = Math.cos(radianYaw);
        double sin = Math.sin(radianYaw);

        double relX = relativePosition.getX();
        double relZ = relativePosition.getZ();

        double rotatedX = relX * cos + relZ * sin;
        double rotatedZ = relX * sin - relZ * cos;

        FreecamPosition absolutePosition = new FreecamPosition();

        absolutePosition.setX(basePlayerX + rotatedX);
        absolutePosition.setY(basePlayerY + relativePosition.getY());
        absolutePosition.setZ(basePlayerZ + rotatedZ);

        absolutePosition.setRotation(
                (float) (basePlayerYaw + relativePosition.getYaw()),
                relativePosition.getPitch()
        );

        return absolutePosition;
    }

    // Получение текущего ключевого кадра
    private static CameraKeyframe getCurrentKeyframe() {
        for (CameraKeyframe keyframe : keyframes) {
            if (keyframe.getTime() >= currentTime) {
                return keyframe;
            }
        }
        return keyframes.get(keyframes.size() - 1);
    }

    // Интерполяция позиции камеры
    private static FreecamPosition interpolatePosition(double time) {
        if (keyframes.isEmpty()) return null;

        // Если время меньше первого кадра
        if (time <= keyframes.get(0).getTime()) {
            return keyframes.get(0).toFreecamPosition();
        }

        if (time >= keyframes.get(keyframes.size() - 1).getTime()) {
            return keyframes.get(keyframes.size() - 1).toFreecamPosition();
        }

        CameraKeyframe prevKeyframe = null;
        CameraKeyframe nextKeyframe = null;

        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (keyframes.get(i).getTime() <= time && keyframes.get(i + 1).getTime() >= time) {
                prevKeyframe = keyframes.get(i);
                nextKeyframe = keyframes.get(i + 1);
                break;
            }
        }

        if (prevKeyframe == null || nextKeyframe == null) {
            return getCurrentKeyframe().toFreecamPosition();
        }

        double t = (time - prevKeyframe.getTime()) / (nextKeyframe.getTime() - prevKeyframe.getTime());

        return interpolate(prevKeyframe, nextKeyframe, t);
    }

    private static FreecamPosition interpolate(CameraKeyframe prev, CameraKeyframe next, double t) {
        FreecamPosition result = new FreecamPosition();

        switch (prev.getInterpolation()) {
            case STEP:
                if (t < 0.5) {
                    result.setX(prev.getX());
                    result.setY(prev.getY());
                    result.setZ(prev.getZ());
                    result.setRotation((float) prev.getYaw(), (float) prev.getPitch());
                } else {
                    result.setX(next.getX());
                    result.setY(next.getY());
                    result.setZ(next.getZ());
                    result.setRotation((float) next.getYaw(), (float) next.getPitch());
                }
                break;

            case LINEAR:
                result.setX(lerp(prev.getX(), next.getX(), t));
                result.setY(lerp(prev.getY(), next.getY(), t));
                result.setZ(lerp(prev.getZ(), next.getZ(), t));
                result.setRotation(
                        (float) lerpAngle(prev.getYaw(), next.getYaw(), t),
                        (float) lerpAngle(prev.getPitch(), next.getPitch(), t)
                );
                break;

            case CATMULLROM:
                result = catmullRomInterpolate(prev, next, t);
                break;
        }

        return result;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double lerpAngle(double a, double b, double t) {
        double diff = b - a;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return a + diff * t;
    }

    private static FreecamPosition catmullRomInterpolate(CameraKeyframe prev, CameraKeyframe next, double t) {
        double smoothT = t * t * (3.0 - 2.0 * t); // smoothstep

        FreecamPosition result = new FreecamPosition();
        result.setX(lerp(prev.getX(), next.getX(), smoothT));
        result.setY(lerp(prev.getY(), next.getY(), smoothT));
        result.setZ(lerp(prev.getZ(), next.getZ(), smoothT));
        result.setRotation(
                (float) lerpAngle(prev.getYaw(), next.getYaw(), smoothT),
                (float) lerpAngle(prev.getPitch(), next.getPitch(), smoothT)
        );

        return result;
    }

    public static boolean isPlaying() { return isPlaying; }
    public static double getCurrentTime() { return currentTime; }
    public static int getKeyframeCount() { return keyframes.size(); }

    public static double getDuration() {
        if (keyframes.isEmpty()) return 0.0;
        return keyframes.get(keyframes.size() - 1).getTime();
    }

    public static double getBasePlayerX() { return basePlayerX; }
    public static double getBasePlayerY() { return basePlayerY; }
    public static double getBasePlayerZ() { return basePlayerZ; }
    public static double getBasePlayerYaw() { return basePlayerYaw; }
}