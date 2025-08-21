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
    private static long lastRenderTime = 0;

    private static double basePlayerX = 0.0;
    private static double basePlayerY = 0.0;
    private static double basePlayerZ = 0.0;
    private static double basePlayerYaw = 0.0;

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
        basePlayerYaw = roundToNearest90(player.getYaw());

        isPlaying = true;
        currentTime = 0.0;
        lastRenderTime = System.nanoTime();

        LOGGER.infoDebug("Playback of the camera animation relative to the player's position has started:"
                + basePlayerX + ", " + basePlayerY + ", " + basePlayerZ + " (yaw: " + basePlayerYaw + ")");
    }

    public static void stopPlayback() {
        isPlaying = false;
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
        if (isPlaying) {
            lastRenderTime = System.nanoTime();
        }
        updateCameraPosition();
    }

    public static void onRender(float tickDelta) {
        if (!isPlaying || keyframes.isEmpty() || FreeCamManager.getFreeCamera() == null || MC.player == null) {
            return;
        }

        long currentSystemTime = System.nanoTime();
        double deltaTime = (currentSystemTime - lastRenderTime) / 1_000_000_000.0;
        lastRenderTime = currentSystemTime;

        currentTime += deltaTime;

        double maxTime = keyframes.get(keyframes.size() - 1).getTime();
        if (currentTime > maxTime) {
            currentTime = maxTime;
            stopPlayback();
        }

        updateCameraPosition();
    }

    public static void tick() {
        if (!isPlaying || keyframes.isEmpty() || FreeCamManager.getFreeCamera() == null || MC.player == null) {
            return;
        }

        long currentSystemTime = System.nanoTime();
        double deltaTime = (currentSystemTime - lastRenderTime) / 1_000_000_000.0;
        lastRenderTime = currentSystemTime;

        currentTime += deltaTime;

        double maxTime = keyframes.get(keyframes.size() - 1).getTime();
        if (currentTime > maxTime) {
            currentTime = maxTime;
            stopPlayback();
        }
        if (AfkcamClient.getConfig().isCameraFollow()){
            updateCameraPosition();
        }
    }

    private static void updateCameraPosition() {
        if (keyframes.isEmpty() || MC.player == null) return;

        FreecamPosition relativePosition = interpolatePosition(currentTime);
        if (relativePosition == null) return;

        // Преобразуем относительную позицию в абсолютную
        FreecamPosition absolutePosition = convertToAbsolutePosition(relativePosition);

        FreeCamManager.getFreeCamera().applyPosition(absolutePosition);
    }

    private static FreecamPosition convertToAbsolutePosition(FreecamPosition relativePosition) {
        PlayerEntity player = MC.player;
        double currentPlayerX = player.getX();
        double currentPlayerY = player.getY();
        double currentPlayerZ = player.getZ();

        double currentPlayerYaw = Math.toRadians(player.getYaw());

        double relX = relativePosition.getX();
        double relZ = relativePosition.getZ();

        double rotatedX = relX * Math.cos(currentPlayerYaw) + relZ * Math.sin(currentPlayerYaw);
        double rotatedZ = relX * Math.sin(currentPlayerYaw) - relZ * Math.cos(currentPlayerYaw);

        FreecamPosition absolutePosition = new FreecamPosition();

        absolutePosition.setX(currentPlayerX + rotatedX);
        absolutePosition.setY(currentPlayerY + relativePosition.getY());
        absolutePosition.setZ(currentPlayerZ + rotatedZ);

        absolutePosition.setRotation(
                player.getYaw() + relativePosition.getYaw(),
                relativePosition.getPitch()
        );

        return absolutePosition;
    }
    private static double roundToNearest90(double angle) {
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;

        long rounded = Math.round(angle / 90.0) * 90;

        return rounded % 360;
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

    // Интерполяция позиции камеры - используем double для time
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