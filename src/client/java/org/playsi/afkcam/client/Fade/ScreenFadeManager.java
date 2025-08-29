//package org.playsi.afkcam.client.Fade;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.VertexFormat;
//import lombok.Getter;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.render.*;
//import net.minecraft.util.math.MathHelper;
//import org.lwjgl.opengl.GL11;
//

//    private static final long AFK_TIMEOUT_MS = 5 * 60 * 1000 ; // 5 минут в миллисекундах
//    private static final float FADE_IN_DURATION = 0.5f; // Длительность затухания в секундах
//    private static final float FADE_OUT_DURATION = 0.5f;
//    private static final float MIN_ANIMATION_DURATION = FADE_IN_DURATION + FADE_OUT_DURATION + 1.0f; // Минимальная длительность анимации (1с + 2*0.5с затухание)

///**
// * Управляет плавным затуханием и проявлением экрана для AFK-камеры
// */
//public class ScreenFadeManager {
//
//    private static final float FADE_DURATION = 0.5f; // Длительность затухания в секундах
//
//    // Геттеры для состояния
//    @Getter
//    private FadeState currentState = FadeState.NONE;
//    @Getter
//    private float fadeProgress = 0.0f;
//    private float fadeStartTime = 0.0f;
//    private Runnable onFadeComplete = null;
//
//    public enum FadeState {
//        NONE,        // Нет затухания
//        FADING_OUT,  // Затухание к черному
//        FADING_IN,   // Проявление от черного
//        BLACK        // Полностью черный экран
//    }
//
//    /**
//     * Тик метод - обновляет состояние затухания
//     */
//    public void tick() {
//        if (currentState == FadeState.NONE || currentState == FadeState.BLACK) {
//            return;
//        }
//
//        float currentTime = getGameTime();
//        float elapsed = currentTime - fadeStartTime;
//
//        if (elapsed >= FADE_DURATION) {
//            // Завершаем затухание
//            completeFade();
//        } else {
//            // Обновляем прогресс
//            fadeProgress = elapsed / FADE_DURATION;
//            fadeProgress = MathHelper.clamp(fadeProgress, 0.0f, 1.0f);
//        }
//    }
//
//    /**
//     * Рендер метод - отрисовывает затухание поверх всего
//     */
//    public void render(float tickDelta) {
//        if (currentState == FadeState.NONE) {
//            return;
//        }
//
//        float alpha = calculateAlpha();
//        if (alpha <= 0.0f) {
//            return;
//        }
//
//        renderFadeOverlay(alpha);
//    }
//
//    /**
//     * Начать затухание к черному экрану
//     */
//    public void startFadeOut(Runnable onComplete) {
//        currentState = FadeState.FADING_OUT;
//        fadeStartTime = getGameTime();
//        fadeProgress = 0.0f;
//        onFadeComplete = onComplete;
//    }
//
//    /**
//     * Начать проявление от черного экрана
//     */
//    public void startFadeIn(Runnable onComplete) {
//        currentState = FadeState.FADING_IN;
//        fadeStartTime = getGameTime();
//        fadeProgress = 0.0f;
//        onFadeComplete = onComplete;
//    }
//
//    /**
//     * Мгновенно установить черный экран
//     */
//    public void setBlack() {
//        currentState = FadeState.BLACK;
//        fadeProgress = 1.0f;
//        onFadeComplete = null;
//    }
//
//    /**
//     * Мгновенно убрать затухание
//     */
//    public void clearFade() {
//        currentState = FadeState.NONE;
//        fadeProgress = 0.0f;
//        onFadeComplete = null;
//    }
//
//    /**
//     * Завершение текущего затухания
//     */
//    private void completeFade() {
//        switch (currentState) {
//            case FADING_OUT:
//                currentState = FadeState.BLACK;
//                fadeProgress = 1.0f;
//                break;
//
//            case FADING_IN:
//                currentState = FadeState.NONE;
//                fadeProgress = 0.0f;
//                break;
//        }
//
//        // Выполняем callback если есть
//        if (onFadeComplete != null) {
//            Runnable callback = onFadeComplete;
//            onFadeComplete = null;
//            callback.run();
//        }
//    }
//
//    /**
//     * Вычисление текущей прозрачности
//     */
//    private float calculateAlpha() {
//        switch (currentState) {
//            case FADING_OUT:
//                return fadeProgress; // От 0 к 1
//
//            case FADING_IN:
//                return 1.0f - fadeProgress; // От 1 к 0
//
//            case BLACK:
//                return 1.0f;
//
//            default:
//                return 0.0f;
//        }
//    }
//
//    /**
//     * Отрисовка overlay затухания
//     */
//    private void renderFadeOverlay(float alpha) {
//        MinecraftClient mc = MinecraftClient.getInstance();
//
//        // Получаем размеры окна
//        int screenWidth = mc.getWindow().getScaledWidth();
//        int screenHeight = mc.getWindow().getScaledHeight();
//
//        // Настраиваем рендер состояние
//        RenderSystem.enableBlend();
//        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//        RenderSystem.disableDepthTest();
//        RenderSystem.depthMask(false);
//
//        // Создаем матрицы для рендера
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder buffer = tessellator.getBuffer();
//
//        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
//
//        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
//
//        // Рисуем черный прямоугольник поверх всего экрана
//        buffer.vertex(0, screenHeight, 0).color(0.0f, 0.0f, 0.0f, alpha);
//        buffer.vertex(screenWidth, screenHeight, 0).color(0.0f, 0.0f, 0.0f, alpha);
//        buffer.vertex(screenWidth, 0, 0).color(0.0f, 0.0f, 0.0f, alpha);
//        buffer.vertex(0, 0, 0).color(0.0f, 0.0f, 0.0f, alpha);
//
//        tessellator.draw();
//
//        // Восстанавливаем рендер состояние
//        RenderSystem.depthMask(true);
//        RenderSystem.enableDepthTest();
//        RenderSystem.disableBlend();
//    }
//
//    /**
//     * Получение игрового времени в секундах
//     */
//    private float getGameTime() {
//        MinecraftClient mc = MinecraftClient.getInstance();
//        if (mc.world != null) {
//            return (mc.world.getTime() + MinecraftClient.getInstance().getTickDelta()) / 20.0f;
//        }
//        return System.currentTimeMillis() / 1000.0f;
//    }
//
//    public boolean isFading() {
//        return currentState == FadeState.FADING_IN || currentState == FadeState.FADING_OUT;
//    }
//    public boolean isBlack() { return currentState == FadeState.BLACK; }
//}