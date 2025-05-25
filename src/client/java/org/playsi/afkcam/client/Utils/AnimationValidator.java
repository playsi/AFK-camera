//package org.playsi.afkcam.client.Utils;
//package org.playsi.afkcam.client.Camera;
//
//import net.minecraft.block.BlockState;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.World;
//import org.playsi.afkcam.client.AfkcamClient;
//import org.playsi.afkcam.client.Utils.LogUtils;
//import org.playsi.afkcam.client.Utils.ParsedAnimation;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Валидирует анимации камеры и обрезает их при обнаружении коллизий с блоками
// */
//public class AnimationValidator {
//
//    private static final MinecraftClient MC = AfkcamClient.getMC();
//    private static final LogUtils LOGGER = new LogUtils(AnimationValidator.class);
//
//    private static final float MIN_ANIMATION_DURATION = 2.0f; // Минимальная длительность (с учетом fade)
//    private static final float FADE_DURATION = 0.5f;
//    private static final float COLLISION_CHECK_STEP = 0.1f; // Шаг проверки коллизий в секундах
//    private static final double CAMERA_COLLISION_RADIUS = 0.3; // Радиус коллизии камеры
//
//    /**
//     * Проверяет, является ли анимация валидной для использования
//     */
//    public boolean isAnimationValid(ParsedAnimation animation) {
//        if (animation == null) {
//            return false;
//        }
//
//        // Проверяем, есть ли keyframes
//        if (animation.getPositionKeyframes().isEmpty() && animation.getRotationKeyframes().isEmpty()) {
//            LOGGER.infoDebug("Анимация " + animation.getName() + " не содержит keyframes");
//            return false;
//        }
//
//        // Проверяем минимальную длительность
//        float duration = getAnimationDuration(animation);
//        if (duration < MIN_ANIMATION_DURATION) {
//            LOGGER.infoDebug("Анимация " + animation.getName() + " слишком короткая: " + duration + "с");
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * Обрезает анимацию, удаляя части где камера пересекается с твердыми блоками
//     */
//    public ParsedAnimation trimAnimationForCollisions(ParsedAnimation originalAnimation) {
//        if (MC.world == null || MC.player == null) {
//            return originalAnimation; // Нет мира для проверки коллизий
//        }
//
//        Vec3d playerPos = MC.player.getPos();
//        float duration = getAnimationDuration(originalAnimation);
//
//        // Проверяем коллизии по всей длительности анимации
//        float lastValidTime = findLastValidTime(originalAnimation, playerPos, duration);
//
//        if (lastValidTime < MIN_ANIMATION_DURATION) {
//            LOGGER.infoDebug("Анимация " + originalAnimation.getName() + " была обрезана до неприемлемой длительности");
//            return null; // Анимация непригодна
//        }
//
//        if (lastValidTime >= duration - 0.1f) {
//            return originalAnimation; // Анимация не нуждается в обрезке
//        }
//
//        // Создаем обрезанную версию анимации
//        return createTrimmedAnimation(originalAnimation, lastValidTime);
//    }
//
//    /**
//     * Находит последнее валидное время без коллизий
//     */
//    private float findLastValidTime(ParsedAnimation animation, Vec3d playerPos, float duration) {
//        float lastValidTime = duration;
//
//        for (float time = 0; time <= duration; time += COLLISION_CHECK_STEP) {
//            Vec3d cameraPos = interpolateCameraPosition(animation, time, playerPos);
//
//            if (hasCameraCollision(cameraPos)) {
//                lastValidTime = Math.max(0, time - COLLISION_CHECK_STEP);
//                break;
//            }
//        }
//
//        return lastValidTime;
//    }
//
//    /**
//     * Интерполирует позицию камеры для заданного времени
//     */
//    private Vec3d interpolateCameraPosition(ParsedAnimation animation, float time, Vec3d basePos) {
//        List<ParsedAnimation.Keyframe> positionFrames = animation.getPositionKeyframes();
//
//        if (positionFrames.isEmpty()) {
//            return basePos;
//        }
//
//        // Находим ближайшие keyframes
//        ParsedAnimation.Keyframe prevFrame = null;
//        ParsedAnimation.Keyframe nextFrame = null;
//
//        for (int i = 0; i < positionFrames.size(); i++) {
//            ParsedAnimation.Keyframe frame = positionFrames.get(i);
//
//            if (frame.getTime() <= time) {
//                prevFrame = frame;
//            }
//
//            if (frame.getTime() >= time && nextFrame == null) {
//                nextFrame = frame;
//                break;
//            }
//        }
//
//        // Если есть только один frame или время точно совпадает
//        if (prevFrame == null && nextFrame != null) {
//            float[] pos = nextFrame.getValues();
//            return basePos.add(pos[0], pos[1], pos[2]);
//        }
//
//        if (nextFrame == null && prevFrame != null) {
//            float[] pos = prevFrame.getValues();
//            return basePos.add(pos[0], pos[1], pos[2]);
//        }
//
//        if (prevFrame == null && nextFrame == null) {
//            return basePos;
//        }
//
//        // Интерполируем между двумя frames
//        if (prevFrame != null && nextFrame != null && prevFrame != nextFrame) {
//            float t = (time - prevFrame.getTime()) / (nextFrame.getTime() - prevFrame.getTime());
//            t = Math.max(0, Math.min(1, t));
//
//            float[] prevPos = prevFrame.getValues();
//            float[] nextPos = nextFrame.getValues();
//
//            double x = basePos.x + lerp(prevPos[0], nextPos[0], t);
//            double y = basePos.y + lerp(prevPos[1], nextPos[1], t);
//            double z = basePos.z + lerp(prevPos[2], nextPos[2], t);
//
//            return new Vec3d(x, y, z);
//        }
//
//        float[] pos = (prevFrame != null ? prevFrame : nextFrame).getValues();
//        return basePos.add(pos[0], pos[1], pos[2]);
//    }
//
//    /**
//     * Проверяет коллизию камеры в заданной позиции
//     */
//    private boolean hasCameraCollision(Vec3d pos) {
//        World world = MC.world;
//        if (world == null) return false;
//
//        // Проверяем блоки в радиусе камеры
//        int minX = (int) Math.floor(pos.x - CAMERA_COLLISION_RADIUS);
//        int maxX = (int) Math.ceil(pos.x + CAMERA_COLLISION_RADIUS);
//        int minY = (int) Math.floor(pos.y - CAMERA_COLLISION_RADIUS);
//        int maxY = (int) Math.ceil(pos.y + CAMERA_COLLISION_RADIUS);
//        int minZ = (int) Math.floor(pos.z - CAMERA_COLLISION_RADIUS);
//        int maxZ = (int) Math.ceil(pos.z + CAMERA_COLLISION_RADIUS);
//
//        for (int x = minX; x <= maxX; x++) {
//            for (int y = minY; y <= maxY; y++) {
//                for (int z = minZ; z <= maxZ; z++) {
//                    BlockPos blockPos = new BlockPos(x, y, z);
//                    BlockState blockState = world.getBlockState(blockPos);
//
//                    // Проверяем, является ли блок твердым (полным)
//                    if (!blockState.isAir() && blockState.isFullCube(world, blockPos)) {
//                        // Проверяем, пересекается ли камера с этим блоком
//                        if (intersectsBlock(pos, blockPos)) {
//                            return true;
//                        }
//                    }
//                }
//            }
//        }
//    }
//}