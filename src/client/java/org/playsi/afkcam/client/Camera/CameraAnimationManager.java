package org.playsi.afkcam.client.Camera;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
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

    // Добавление ключевого кадра
    public static void addKeyframe(float time, double x, double y, double z, float yaw, float pitch,
                                   CameraKeyframe.InterpolationType interpolation) {
        CameraKeyframe keyframe = new CameraKeyframe(time, x, y, z, yaw, pitch, interpolation);

        // Вставляем в правильную позицию по времени
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

    // Добавление ключевого кадра с текущей позицией камеры
    public static void addKeyframeAtCurrentPosition(float time, CameraKeyframe.InterpolationType interpolation) {
        if (FreeCamManager.getFreeCamera() == null) {
            LOGGER.infoDebug("Нет активной камеры для создания ключевого кадра");
            return;
        }

        FreeCamera camera = FreeCamManager.getFreeCamera();
        addKeyframe(time, camera.getX(), camera.getY(), camera.getZ(),
                camera.getYaw(), camera.getPitch(), interpolation);
    }

    // Удаление ключевого кадра по индексу
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

    // Очистка всех ключевых кадров
    public static void clearKeyframes() {
        keyframes.clear();
        if (AfkcamClient.getConfig().isDebugLogEnabled()) {
            LOGGER.infoDebug("Очищены все ключевые кадры");
        }
    }

    // Запуск воспроизведения анимации
    public static void startPlayback() {
        if (keyframes.isEmpty()) {
            LOGGER.infoDebug("Нет ключевых кадров для воспроизведения");
            return;
        }

        if (!FreeCamManager.isEnabled()) {
            FreeCamManager.freecamToggle();
        }

        isPlaying = true;
        currentTime = 0.0f;
        lastRenderTime = System.nanoTime(); // Используем наносекунды для точности

        if (AfkcamClient.getConfig().isDebugLogEnabled()) {
            LOGGER.infoDebug("Начато воспроизведение анимации камеры");
        }
    }

    // Остановка воспроизведения
    public static void stopPlayback() {
        isPlaying = false;
        if (AfkcamClient.getConfig().isDebugLogEnabled()) {
            LOGGER.infoDebug("Остановлено воспроизведение анимации камеры");
        }
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
        if (!isPlaying || keyframes.isEmpty() || FreeCamManager.getFreeCamera() == null) {
            return;
        }

        long currentSystemTime = System.nanoTime();
        float deltaTime = (currentSystemTime - lastRenderTime) / 1_000_000_000.0f; // конвертируем в секунды
        lastRenderTime = currentSystemTime;

        currentTime += deltaTime;

        // Проверяем, закончилась ли анимация
        float maxTime = keyframes.get(keyframes.size() - 1).getTime();
        if (currentTime > maxTime) {
            currentTime = maxTime;
            stopPlayback();
        }

        updateCameraPosition();
    }

    // Обновление каждый тик (оставляем для совместимости, но рекомендуется использовать onRender)
    public static void tick() {
        if (!isPlaying || keyframes.isEmpty() || FreeCamManager.getFreeCamera() == null) {
            return;
        }

        long currentSystemTime = System.nanoTime();
        float deltaTime = (currentSystemTime - lastRenderTime) / 1_000_000_000.0f; // конвертируем в секунды
        lastRenderTime = currentSystemTime;

        currentTime += deltaTime;

        // Проверяем, закончилась ли анимация
        float maxTime = keyframes.get(keyframes.size() - 1).getTime();
        if (currentTime > maxTime) {
            currentTime = maxTime;
            stopPlayback();
        }

        updateCameraPosition();
    }

    // Обновление позиции камеры на основе текущего времени
    private static void updateCameraPosition() {
        if (keyframes.isEmpty()) return;

        CameraKeyframe keyframe = getCurrentKeyframe();
        if (keyframe == null) return;

        FreecamPosition position = interpolatePosition(currentTime);
        if (position != null) {
            FreeCamManager.getFreeCamera().applyPosition(position);
        }
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
                // Без интерполяции - используем предыдущий кадр
                result.setX(prev.getX());
                result.setY(prev.getY());
                result.setZ(prev.getZ());
                result.setRotation(prev.getYaw(), prev.getPitch());
                break;

            case LINEAR:
                // Линейная интерполяция
                result.setX(lerp(prev.getX(), next.getX(), t));
                result.setY(lerp(prev.getY(), next.getY(), t));
                result.setZ(lerp(prev.getZ(), next.getZ(), t));
                result.setRotation(
                        lerpAngle(prev.getYaw(), next.getYaw(), t),
                        lerpAngle(prev.getPitch(), next.getPitch(), t)
                );
                break;

            case CATMULLROM:
                // Catmull-Rom интерполяция (более плавная)
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
}
/*
// Создание простой анимации камеры:

// 1. Добавляем ключевые кадры
CameraAnimationManager.addKeyframe(0.0f, 0, 70, 0, 0, 0, CameraKeyframe.InterpolationType.LINEAR);
CameraAnimationManager.addKeyframe(3.0f, 10, 75, 10, 45, 0, CameraKeyframe.InterpolationType.LINEAR);
CameraAnimationManager.addKeyframe(6.0f, 0, 80, 20, 90, -15, CameraKeyframe.InterpolationType.CATMULLROM);

// 2. Или добавляем кадр с текущей позицией камеры
CameraAnimationManager.addKeyframeAtCurrentPosition(9.0f, CameraKeyframe.InterpolationType.LINEAR);

// 3. Запускаем воспроизведение
CameraAnimationManager.startPlayback();

// 4. Управление воспроизведением
CameraAnimationManager.stopPlayback();           // остановить
CameraAnimationManager.togglePlayback();         // пауза/возобновление
CameraAnimationManager.seekToTime(2.5f);         // перемотка к 2.5 секундам

// 5. Управление ключевыми кадрами
CameraAnimationManager.removeKeyframeAtTime(3.0f);  // удалить кадр в 3 секунды
CameraAnimationManager.removeKeyframe(0);           // удалить первый кадр
CameraAnimationManager.clearKeyframes();            // очистить все кадры

// 6. Получение информации
boolean playing = CameraAnimationManager.isPlaying();
float currentTime = CameraAnimationManager.getCurrentTime();
int keyframeCount = CameraAnimationManager.getKeyframeCount();
float duration = CameraAnimationManager.getDuration();
*/
