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
    // Используем double для времени для большей точности
    private static double currentTime = 0.0;
    private static long lastRenderTime = 0;

    // Позиция игрока на момент начала анимации - используем double
    private static double basePlayerX = 0.0;
    private static double basePlayerY = 0.0;
    private static double basePlayerZ = 0.0;
    private static double basePlayerYaw = 0.0;

    // Добавление ключевого кадра - изменяем time на double
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
            LOGGER.infoDebug("Добавлен ключевой кадр в " + time + "с: " + keyframe);
        }
    }

    public static boolean removeKeyframe(int index) {
        if (index >= 0 && index < keyframes.size()) {
            CameraKeyframe removed = keyframes.remove(index);
            if (AfkcamClient.getConfig().isDebugLogEnabled()) {
                LOGGER.infoDebug("Удален ключевой кадр: " + removed);
            }
            return true;
        }
        return false;
    }

    // Удаление ключевого кадра по времени - используем double
    public static boolean removeKeyframeAtTime(double time) {
        for (int i = 0; i < keyframes.size(); i++) {
            if (Math.abs(keyframes.get(i).getTime() - time) < 0.001) { // Увеличили точность
                return removeKeyframe(i);
            }
        }
        return false;
    }

    public static void clearKeyframes() {
        keyframes.clear();
        if (AfkcamClient.getConfig().isDebugLogEnabled()) {
            LOGGER.infoDebug("Очищены все ключевые кадры");
        }
    }

    public static void startPlayback() {
        if (keyframes.isEmpty()) {
            LOGGER.infoDebug("Нет ключевых кадров для воспроизведения");
            return;
        }

        if (MC.player == null) {
            LOGGER.infoDebug("Нет игрока для воспроизведения анимации");
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

        LOGGER.infoDebug("Начато воспроизведение анимации камеры относительно позиции игрока: "
                + basePlayerX + ", " + basePlayerY + ", " + basePlayerZ + " (yaw: " + basePlayerYaw + ")");
    }

    // Остановка воспроизведения
    public static void stopPlayback() {
        isPlaying = false;
        LOGGER.infoDebug("Остановлено воспроизведение анимации камеры");
    }

    // Пауза/возобновление
    public static void togglePlayback() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    // Перемотка к определенному времени - используем double
    public static void seekToTime(double time) {
        currentTime = Math.max(0, time);
        if (isPlaying) {
            lastRenderTime = System.nanoTime();
        }
        updateCameraPosition();
    }

    // Обновление на каждом кадре (вызывать из GameRenderer)
    public static void onRender() {
        if (!isPlaying || keyframes.isEmpty() || FreeCamManager.getFreeCamera() == null || MC.player == null) {
            return;
        }

        long currentSystemTime = System.nanoTime();
        // Используем double для deltaTime для большей точности
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

    // Обновление каждый тик
    public static void tick() {
        if (!isPlaying || keyframes.isEmpty() || FreeCamManager.getFreeCamera() == null || MC.player == null) {
            return;
        }

        long currentSystemTime = System.nanoTime();
        // Используем double для deltaTime для большей точности
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

    // Обновление позиции камеры на основе текущего времени
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
        double currentPlayerYaw = Math.toRadians(roundToNearest90(player.getYaw()));

        // Применяем поворот к относительным координатам
        double relX = relativePosition.getX();
        double relZ = relativePosition.getZ();

        double rotatedX = relX * Math.cos(currentPlayerYaw) - relZ * Math.sin(currentPlayerYaw);
        double rotatedZ = relX * Math.sin(currentPlayerYaw) + relZ * Math.cos(currentPlayerYaw);

        FreecamPosition absolutePosition = new FreecamPosition();
        double playerYaw = roundToNearest90(player.getYaw());

        if (playerYaw == 0f || playerYaw == 180f){
            absolutePosition.setX(currentPlayerX + rotatedX);
            absolutePosition.setY(currentPlayerY + relativePosition.getY());
            absolutePosition.setZ(currentPlayerZ - rotatedZ);

            absolutePosition.setRotation(
                    player.getYaw() + relativePosition.getYaw(),
                    relativePosition.getPitch()
            );

        }else if ( playerYaw == 90f || playerYaw == 270f) {
            absolutePosition.setX(currentPlayerX - rotatedX);
            absolutePosition.setY(currentPlayerY + relativePosition.getY());
            absolutePosition.setZ(currentPlayerZ + rotatedZ);

            absolutePosition.setRotation(
                    player.getYaw() + relativePosition.getYaw(),
                    relativePosition.getPitch()
            );
        }

        return absolutePosition;
    }

    // Округление угла до ближайших 90 градусов - возвращаем double
    private static double roundToNearest90(double angle) {
        // Нормализуем угол в диапазон 0-360
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;

        // Округляем до ближайших 90 градусов
        long rounded = Math.round(angle / 90.0) * 90;

        // Возвращаем в диапазон 0-360
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

        // Если время больше последнего кадра
        if (time >= keyframes.get(keyframes.size() - 1).getTime()) {
            return keyframes.get(keyframes.size() - 1).toFreecamPosition();
        }

        // Находим два ключевых кадра для интерполяции
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

        // Вычисляем коэффициент интерполяции - используем double
        double t = (time - prevKeyframe.getTime()) / (nextKeyframe.getTime() - prevKeyframe.getTime());

        return interpolate(prevKeyframe, nextKeyframe, t);
    }

    // Интерполяция между двумя ключевыми кадрами - используем double для t
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

    // Линейная интерполяция - используем double
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // Интерполяция углов с учетом цикличности - используем double
    private static double lerpAngle(double a, double b, double t) {
        double diff = b - a;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return a + diff * t;
    }

    // Catmull-Rom интерполяция (упрощенная версия) - используем double
    private static FreecamPosition catmullRomInterpolate(CameraKeyframe prev, CameraKeyframe next, double t) {
        // Для упрощения используем сглаженную версию линейной интерполяции
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

    // Геттеры для состояния - возвращаем double
    public static boolean isPlaying() { return isPlaying; }
    public static double getCurrentTime() { return currentTime; }
    public static int getKeyframeCount() { return keyframes.size(); }

    public static double getDuration() {
        if (keyframes.isEmpty()) return 0.0;
        return keyframes.get(keyframes.size() - 1).getTime();
    }

    // Дополнительные геттеры для отладки
    public static double getBasePlayerX() { return basePlayerX; }
    public static double getBasePlayerY() { return basePlayerY; }
    public static double getBasePlayerZ() { return basePlayerZ; }
    public static double getBasePlayerYaw() { return basePlayerYaw; }
}