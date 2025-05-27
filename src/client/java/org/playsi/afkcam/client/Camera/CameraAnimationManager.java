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
    private static float currentTime = 0.0f;
    private static long lastRenderTime = 0;

    // Позиция игрока на момент начала анимации
    private static double basePlayerX = 0.0;
    private static double basePlayerY = 0.0;
    private static double basePlayerZ = 0.0;
    private static float basePlayerYaw = 0.0f;

    // Добавление ключевого кадра
    public static void addKeyframe(float time, double x, double y, double z, float yaw, float pitch,
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

    // Добавление ключевого кадра с текущей позицией камеры (относительно игрока)
    public static void addKeyframeAtCurrentPosition(float time, CameraKeyframe.InterpolationType interpolation) {
        if (FreeCamManager.getFreeCamera() == null || MC.player == null) {
            LOGGER.infoDebug("Нет активной камеры или игрока для создания ключевого кадра");
            return;
        }

        FreeCamera camera = FreeCamManager.getFreeCamera();
        PlayerEntity player = MC.player;

        double relativeX = camera.getX() - player.getX();
        double relativeY = camera.getY() - player.getY();
        double relativeZ = camera.getZ() - player.getZ();

        float playerYawRounded = roundToNearest90(player.getYaw());
        float relativeYaw = camera.getYaw() - playerYawRounded;

        float pitch = camera.getPitch();

        addKeyframe(time, relativeX, relativeY, relativeZ, relativeYaw, pitch, interpolation);
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

    // Удаление ключевого кадра по времени
    public static boolean removeKeyframeAtTime(float time) {
        for (int i = 0; i < keyframes.size(); i++) {
            if (Math.abs(keyframes.get(i).getTime() - time) < 0.01f) {
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
        currentTime = 0.0f;
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

    // Перемотка к определенному времени
    public static void seekToTime(float time) {
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
        float deltaTime = (currentSystemTime - lastRenderTime) / 1_000_000_000.0f;
        lastRenderTime = currentSystemTime;

        currentTime += deltaTime;

        float maxTime = keyframes.get(keyframes.size() - 1).getTime();
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
        float deltaTime = (currentSystemTime - lastRenderTime) / 1_000_000_000.0f;
        lastRenderTime = currentSystemTime;

        currentTime += deltaTime;

        float maxTime = keyframes.get(keyframes.size() - 1).getTime();
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

    // Преобразование относительной позиции в абсолютную
    private static FreecamPosition convertToAbsolutePosition(FreecamPosition relativePosition) {
        PlayerEntity player = MC.player;

        // Текущая позиция игрока
        double currentPlayerX = player.getX();
        double currentPlayerY = player.getY();
        double currentPlayerZ = player.getZ();
        float currentPlayerYaw = roundToNearest90(player.getYaw());

        // Вычисляем смещение от базовой позиции
        double deltaX = currentPlayerX - basePlayerX;
        double deltaY = currentPlayerY - basePlayerY;
        double deltaZ = currentPlayerZ - basePlayerZ;
        float deltaYaw = currentPlayerYaw - basePlayerYaw;

        // Применяем поворот к относительным координатам, если yaw изменился
        double rotatedX = relativePosition.getX();
        double rotatedZ = relativePosition.getZ();

        if (Math.abs(deltaYaw) > 0.1f) {
            double radians = Math.toRadians(deltaYaw);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);

            rotatedX = relativePosition.getX() * cos - relativePosition.getZ() * sin;
            rotatedZ = relativePosition.getX() * sin + relativePosition.getZ() * cos;
        }

        // Создаем абсолютную позицию
        FreecamPosition absolutePosition = new FreecamPosition();
        absolutePosition.setX(currentPlayerX + rotatedX);
        absolutePosition.setY(currentPlayerY + relativePosition.getY());
        absolutePosition.setZ(currentPlayerZ + rotatedZ);

        // Yaw = текущий округленный yaw игрока + относительный yaw анимации
        // Pitch остается из анимации (не относительный)
        absolutePosition.setRotation(
                currentPlayerYaw + relativePosition.getYaw(),
                relativePosition.getPitch()
        );

        return absolutePosition;
    }

    // Округление угла до ближайших 90 градусов
    private static float roundToNearest90(float angle) {
        // Нормализуем угол в диапазон 0-360
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;

        // Округляем до ближайших 90 градусов
        int rounded = Math.round(angle / 90.0f) * 90;

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

    // Интерполяция позиции камеры
    private static FreecamPosition interpolatePosition(float time) {
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

        // Вычисляем коэффициент интерполяции
        float t = (time - prevKeyframe.getTime()) / (nextKeyframe.getTime() - prevKeyframe.getTime());

        return interpolate(prevKeyframe, nextKeyframe, t);
    }

    // Интерполяция между двумя ключевыми кадрами
    private static FreecamPosition interpolate(CameraKeyframe prev, CameraKeyframe next, float t) {
        FreecamPosition result = new FreecamPosition();

        switch (prev.getInterpolation()) {
            case STEP:
                if (t < 0.5f) {
                    result.setX(prev.getX());
                    result.setY(prev.getY());
                    result.setZ(prev.getZ());
                    result.setRotation(prev.getYaw(), prev.getPitch());
                } else {
                    result.setX(next.getX());
                    result.setY(next.getY());
                    result.setZ(next.getZ());
                    result.setRotation(next.getYaw(), next.getPitch());
                }
                break;

            case LINEAR:
                result.setX(lerp(prev.getX(), next.getX(), t));
                result.setY(lerp(prev.getY(), next.getY(), t));
                result.setZ(lerp(prev.getZ(), next.getZ(), t));
                result.setRotation(
                        lerpAngle(prev.getYaw(), next.getYaw(), t),
                        lerpAngle(prev.getPitch(), next.getPitch(), t)
                );
                break;

            case CATMULLROM:
                result = catmullRomInterpolate(prev, next, t);
                break;
        }

        return result;
    }

    // Линейная интерполяция
    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    // Интерполяция углов с учетом цикличности
    private static float lerpAngle(float a, float b, float t) {
        float diff = b - a;
        if (diff > 180) diff -= 360;
        if (diff < -180) diff += 360;
        return a + diff * t;
    }

    // Catmull-Rom интерполяция (упрощенная версия)
    private static FreecamPosition catmullRomInterpolate(CameraKeyframe prev, CameraKeyframe next, float t) {
        // Для упрощения используем сглаженную версию линейной интерполяции
        float smoothT = t * t * (3.0f - 2.0f * t); // smoothstep

        FreecamPosition result = new FreecamPosition();
        result.setX(lerp(prev.getX(), next.getX(), smoothT));
        result.setY(lerp(prev.getY(), next.getY(), smoothT));
        result.setZ(lerp(prev.getZ(), next.getZ(), smoothT));
        result.setRotation(
                lerpAngle(prev.getYaw(), next.getYaw(), smoothT),
                lerpAngle(prev.getPitch(), next.getPitch(), smoothT)
        );

        return result;
    }

    // Геттеры для состояния
    public static boolean isPlaying() { return isPlaying; }
    public static float getCurrentTime() { return currentTime; }
    public static int getKeyframeCount() { return keyframes.size(); }

    public static float getDuration() {
        if (keyframes.isEmpty()) return 0.0f;
        return keyframes.get(keyframes.size() - 1).getTime();
    }

    // Дополнительные геттеры для отладки
    public static double getBasePlayerX() { return basePlayerX; }
    public static double getBasePlayerY() { return basePlayerY; }
    public static double getBasePlayerZ() { return basePlayerZ; }
    public static float getBasePlayerYaw() { return basePlayerYaw; }
}