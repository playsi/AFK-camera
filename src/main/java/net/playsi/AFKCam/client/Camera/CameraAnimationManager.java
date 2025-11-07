package net.playsi.Afkcam.client.Camera;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.playsi.Afkcam.client.AfkcamClient;
import net.playsi.Afkcam.config.Config;
import net.playsi.Afkcam.utils.LogUtils;

public class CameraAnimationManager {

    private static final MinecraftClient MC = AfkcamClient.getMC();
    private static final LogUtils LOGGER = new LogUtils(CameraAnimationManager.class);
    private static final Config CONFIG = Config.getInstance();

    @Getter
    private static CameraAnimation currentAnimation = null;

    private static boolean isPlaying = false;
    private static double currentTime = 0.0;
    private static long lastUpdateTime = 0L;

    private static double basePlayerX = 0.0;
    private static double basePlayerY = 0.0;
    private static double basePlayerZ = 0.0;
    private static double basePlayerYaw = 0.0;
    private static double basePlayerPitch = 0.0;

    // Кэш для уменьшения вычислений
    private static double lastPlayerYaw = Double.NaN;
    private static double cachedCos = 0.0;
    private static double cachedSin = 0.0;

    // Сглаживание
    private static Double lastX = null, lastY = null, lastZ = null;
    private static Float lastYaw = null, lastPitch = null;
    private static final double SMOOTHING_FACTOR = CONFIG.getSmoothingFactor();

    public static void setAnimation(CameraAnimation animation) {
        if (isPlaying) {
            stopPlayback();
        }
        currentAnimation = animation;
        resetCache();
        LOGGER.infoDebug("Animation set: " + (animation != null ? animation.getName() : "null"));
    }

    public static void startPlayback() {
        if (currentAnimation == null || currentAnimation.isEmpty()) {
            LOGGER.infoDebug("No animation or keyframes for playback");
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
        basePlayerPitch = player.getPitch();

        isPlaying = true;
        currentTime = 0.0;
        lastUpdateTime = System.nanoTime();
        resetCache();

        LOGGER.infoDebug("Animation playback started: " + currentAnimation.getName());
    }

    public static void stopPlayback() {
        isPlaying = false;
        resetCache();
        LOGGER.infoDebug("Animation playback stopped");
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
        resetCache();
        updateCameraPosition();
    }

    public static void onRender(float tickDelta) {
        if (!isPlaying || currentAnimation == null || currentAnimation.isEmpty()
                || FreeCamManager.getFreeCamera() == null || MC.player == null) {
            return;
        }

        long currentNanoTime = System.nanoTime();
        if (lastUpdateTime > 0) {
            double realDeltaTime = (currentNanoTime - lastUpdateTime) / 1_000_000_000.0;
            currentTime += realDeltaTime;
        }
        lastUpdateTime = currentNanoTime;

        double maxTime = currentAnimation.getDuration();
        if (currentTime > maxTime) {
            currentTime = maxTime;
            stopPlayback();
            return;
        }

        updateCameraPosition();
    }

    private static void updateCameraPosition() {
        if (currentAnimation == null || MC.player == null) return;

        FreecamPosition currentPosition = FreeCamManager.getFreeCamera().CurrentPosition();
        if (currentPosition == null) {
            currentPosition = new FreecamPosition();
        }

        // Интерполируем позицию (если есть кейфреймы)
        PositionData posData = interpolatePosition(currentTime);
        if (posData != null) {
            FreecamPosition absolutePosition = CONFIG.isCameraFollow()
                    ? convertToAbsolutePosition(posData.x, posData.y, posData.z)
                    : convertToAbsolutePositionStatic(posData.x, posData.y, posData.z);

            // Применяем сглаживание
            if (lastX != null && SMOOTHING_FACTOR > 0) {
                absolutePosition.setX(lerp(lastX, absolutePosition.getX(), SMOOTHING_FACTOR));
                absolutePosition.setY(lerp(lastY, absolutePosition.getY(), SMOOTHING_FACTOR));
                absolutePosition.setZ(lerp(lastZ, absolutePosition.getZ(), SMOOTHING_FACTOR));
            }

            lastX = absolutePosition.getX();
            lastY = absolutePosition.getY();
            lastZ = absolutePosition.getZ();

            currentPosition.setPosition(absolutePosition.getX(), absolutePosition.getY(), absolutePosition.getZ());
        }

        // Интерполируем ротацию (если есть кейфреймы)
        RotationData rotData = interpolateRotation(currentTime);
        if (rotData != null) {
            float absoluteYaw = CONFIG.isCameraFollow()
                    ? (float) (MC.player.getYaw() + rotData.yaw)
                    : (float) (basePlayerYaw + rotData.yaw);

            float absolutePitch = CONFIG.isCameraFollow()
                    ? rotData.pitch
                    : (float) (basePlayerPitch + rotData.pitch);

            // Применяем сглаживание
            if (lastYaw != null && SMOOTHING_FACTOR > 0) {
                absoluteYaw = (float) lerpAngle(lastYaw, absoluteYaw, SMOOTHING_FACTOR);
                absolutePitch = (float) lerpAngle(lastPitch, absolutePitch, SMOOTHING_FACTOR);
            }

            lastYaw = absoluteYaw;
            lastPitch = absolutePitch;

            currentPosition.setRotation(absoluteYaw, absolutePitch);
        }

        FreeCamManager.getFreeCamera().applyPosition(currentPosition);
    }

    // Интерполяция позиции
    private static PositionData interpolatePosition(double time) {
        var keyframes = currentAnimation.getPositionKeyframes();
        if (keyframes.isEmpty()) return null;

        if (time <= keyframes.get(0).getTime()) {
            var kf = keyframes.get(0);
            return new PositionData(kf.getX(), kf.getY(), kf.getZ());
        }

        if (time >= keyframes.get(keyframes.size() - 1).getTime()) {
            var kf = keyframes.get(keyframes.size() - 1);
            return new PositionData(kf.getX(), kf.getY(), kf.getZ());
        }

        for (int i = 0; i < keyframes.size() - 1; i++) {
            var prev = keyframes.get(i);
            var next = keyframes.get(i + 1);

            if (prev.getTime() <= time && next.getTime() >= time) {
                double t = (time - prev.getTime()) / (next.getTime() - prev.getTime());
                return interpolatePositionBetween(prev, next, t);
            }
        }

        return null;
    }

    // Интерполяция ротации
    private static RotationData interpolateRotation(double time) {
        var keyframes = currentAnimation.getRotationKeyframes();
        if (keyframes.isEmpty()) return null;

        if (time <= keyframes.get(0).getTime()) {
            var kf = keyframes.get(0);
            return new RotationData(kf.getYaw(), kf.getPitch());
        }

        if (time >= keyframes.get(keyframes.size() - 1).getTime()) {
            var kf = keyframes.get(keyframes.size() - 1);
            return new RotationData(kf.getYaw(), kf.getPitch());
        }

        for (int i = 0; i < keyframes.size() - 1; i++) {
            var prev = keyframes.get(i);
            var next = keyframes.get(i + 1);

            if (prev.getTime() <= time && next.getTime() >= time) {
                double t = (time - prev.getTime()) / (next.getTime() - prev.getTime());
                return interpolateRotationBetween(prev, next, t);
            }
        }

        return null;
    }

    private static PositionData interpolatePositionBetween(
            CameraAnimation.PositionKeyframe prev,
            CameraAnimation.PositionKeyframe next,
            double t) {

        double x, y, z;

        switch (prev.getInterpolation()) {
            case STEP -> {
                x = t < 0.5 ? prev.getX() : next.getX();
                y = t < 0.5 ? prev.getY() : next.getY();
                z = t < 0.5 ? prev.getZ() : next.getZ();
            }
            case LINEAR -> {
                x = lerp(prev.getX(), next.getX(), t);
                y = lerp(prev.getY(), next.getY(), t);
                z = lerp(prev.getZ(), next.getZ(), t);
            }
            case CATMULLROM -> {
                double smoothT = t * t * (3.0 - 2.0 * t);
                x = lerp(prev.getX(), next.getX(), smoothT);
                y = lerp(prev.getY(), next.getY(), smoothT);
                z = lerp(prev.getZ(), next.getZ(), smoothT);
            }
            default -> {
                x = prev.getX();
                y = prev.getY();
                z = prev.getZ();
            }
        }

        return new PositionData(x, y, z);
    }

    private static RotationData interpolateRotationBetween(
            CameraAnimation.RotationKeyframe prev,
            CameraAnimation.RotationKeyframe next,
            double t) {

        double yaw, pitch;

        switch (prev.getInterpolation()) {
            case STEP -> {
                yaw = t < 0.5 ? prev.getYaw() : next.getYaw();
                pitch = t < 0.5 ? prev.getPitch() : next.getPitch();
            }
            case LINEAR -> {
                yaw = lerpAngle(prev.getYaw(), next.getYaw(), t);
                pitch = lerpAngle(prev.getPitch(), next.getPitch(), t);
            }
            case CATMULLROM -> {
                double smoothT = t * t * (3.0 - 2.0 * t);
                yaw = lerpAngle(prev.getYaw(), next.getYaw(), smoothT);
                pitch = lerpAngle(prev.getPitch(), next.getPitch(), smoothT);
            }
            default -> {
                yaw = prev.getYaw();
                pitch = prev.getPitch();
            }
        }

        return new RotationData(yaw, pitch);
    }

    private static FreecamPosition convertToAbsolutePosition(double relX, double relY, double relZ) {
        PlayerEntity player = MC.player;
        double currentPlayerYaw = player.getYaw();

        if (Double.isNaN(lastPlayerYaw) || Math.abs(currentPlayerYaw - lastPlayerYaw) > 0.01) {
            double radianYaw = Math.toRadians(currentPlayerYaw);
            cachedCos = Math.cos(radianYaw);
            cachedSin = Math.sin(radianYaw);
            lastPlayerYaw = currentPlayerYaw;
        }

        double rotatedX = relX * cachedCos + relZ * cachedSin;
        double rotatedZ = relX * cachedSin - relZ * cachedCos;

        FreecamPosition result = new FreecamPosition();
        result.setX(player.getX() + rotatedX);
        result.setY(player.getY() + relY);
        result.setZ(player.getZ() + rotatedZ);

        return result;
    }

    private static FreecamPosition convertToAbsolutePositionStatic(double relX, double relY, double relZ) {
        double radianYaw = Math.toRadians(basePlayerYaw);
        double cos = Math.cos(radianYaw);
        double sin = Math.sin(radianYaw);

        double rotatedX = relX * cos + relZ * sin;
        double rotatedZ = relX * sin - relZ * cos;

        FreecamPosition result = new FreecamPosition();
        result.setX(basePlayerX + rotatedX);
        result.setY(basePlayerY + relY);
        result.setZ(basePlayerZ + rotatedZ);

        return result;
    }

    private static void resetCache() {
        lastX = null;
        lastY = null;
        lastZ = null;
        lastYaw = null;
        lastPitch = null;
        lastPlayerYaw = Double.NaN;
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

    // Вспомогательные классы для данных
    private static class PositionData {
        double x, y, z;
        PositionData(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class RotationData {
        double yaw;
        float pitch;
        RotationData(double yaw, double pitch) {
            this.yaw = yaw;
            this.pitch = (float) pitch;
        }
    }

    // Геттеры
    public static boolean isPlaying() { return isPlaying; }
    public static double getCurrentTime() { return currentTime; }
    public static double getDuration() {
        return currentAnimation != null ? currentAnimation.getDuration() : 0.0;
    }
}