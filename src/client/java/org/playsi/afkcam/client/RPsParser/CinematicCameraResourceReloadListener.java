package org.playsi.afkcam.client.RPsParser;

import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.Utils.ParsedAnimation;
import org.playsi.afkcam.client.Utils.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Resource reload listener that scans cinematic_camera .bbmodel files and parses camera animations.
 * This class handles automatic reloading of camera animation resources when the resource pack changes.
 */
@Environment(EnvType.CLIENT)
public class CinematicCameraResourceReloadListener implements SimpleResourceReloadListener<List<ParsedAnimation>> {

    private static final LogUtils LOGGER = new LogUtils(CinematicCameraResourceReloadListener.class);
    private static final Identifier LISTENER_ID = Identifier.of("cinematic:camera_reload");
    private static final String CAMERA_RESOURCE_PATH = "cinematic_camera";
    private static final String BBMODEL_EXTENSION = ".bbmodel";

    @Getter
    private static volatile List<ParsedAnimation> cachedAnimations = new ArrayList<>();

    /**
     * Registers this reload listener with the Fabric resource manager.
     */
    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new CinematicCameraResourceReloadListener());
    }

    @Override
    public Identifier getFabricId() {
        return LISTENER_ID;
    }

    @Override
    public CompletableFuture<List<ParsedAnimation>> load(ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> loadAnimations(manager), executor);
    }

    @Override
    public CompletableFuture<Void> apply(List<ParsedAnimation> animations, ResourceManager manager, Executor executor) {
        return CompletableFuture.runAsync(() -> applyCachedAnimations(animations), executor);
    }

    /**
     * Loads camera animations from .bbmodel files.
     *
     * @param manager The resource manager to use for loading resources
     * @return List of parsed animations
     */
    private List<ParsedAnimation> loadAnimations(ResourceManager manager) {
        if (!isModEnabled()) {
            LOGGER.infoDebug("Mod is disabled, skipping animation loading");
            return Collections.emptyList();
        }

        logResourceDiscovery(manager);

        Map<Identifier, Resource> bbmodelResources = findBbmodelResources(manager);
        if (bbmodelResources.isEmpty()) {
            logNoBbmodelFilesFound();
            return Collections.emptyList();
        }

        return processBbmodelFiles(bbmodelResources);
    }

    /**
     * Checks if the mod is enabled.
     */
    private boolean isModEnabled() {
        return AfkcamClient.getConfig().isModEnabled();
    }

    /**
     * Logs information about resource discovery for debugging purposes.
     */
    private void logResourceDiscovery(ResourceManager manager) {
        Map<Identifier, Resource> allResources = manager.findResources(CAMERA_RESOURCE_PATH, id -> true);

        if (allResources.isEmpty()) {
            LOGGER.errorDebug("No resources found in " + CAMERA_RESOURCE_PATH);
        } else {
            LOGGER.infoDebug("Found " + allResources.size() + " resources in " + CAMERA_RESOURCE_PATH);
            allResources.keySet().forEach(id -> LOGGER.infoDebug("   * " + id));
        }
    }

    /**
     * Finds all .bbmodel resources in the camera resource path.
     */
    private Map<Identifier, Resource> findBbmodelResources(ResourceManager manager) {
        LOGGER.infoDebug("Looking for .bbmodel files in " + CAMERA_RESOURCE_PATH + ":");
        return manager.findResources(CAMERA_RESOURCE_PATH,
                id -> id.getPath().endsWith(BBMODEL_EXTENSION));
    }

    /**
     * Logs error when no .bbmodel files are found and provides helpful guidance.
     */
    private void logNoBbmodelFilesFound() {
        LOGGER.errorDebug("No .bbmodel files found! This is the main issue.");
        LOGGER.errorDebug("Make sure .bbmodel files are in the correct location:");
        LOGGER.errorDebug("   * assets/<namespace>/" + CAMERA_RESOURCE_PATH + "/*" + BBMODEL_EXTENSION);
    }

    /**
     * Processes all found .bbmodel files and extracts animations.
     */
    private List<ParsedAnimation> processBbmodelFiles(Map<Identifier, Resource> bbmodelResources) {
        LOGGER.info("Found " + bbmodelResources.size() + " .bbmodel files");

        List<ParsedAnimation> allAnimations = new ArrayList<>();

        bbmodelResources.forEach((id, resource) -> {
            if (shouldSkipAfkcamResource(id)) {
                LOGGER.infoDebug("Skipping afkcam resource: " + id + " (default animations disabled)");
                return;
            }

            try {
                List<ParsedAnimation> animations = processSingleBbmodelFile(id, resource);
                allAnimations.addAll(animations);
            } catch (Exception e) {
                LOGGER.errorDebug("Failed to load camera file: " + id + " - " + e.getMessage());
            }
        });

        LOGGER.info("Total camera animations loaded: " + allAnimations.size());
        return allAnimations;
    }

    /**
     * Проверяет, нужно ли пропустить ресурс afkcam на основе настроек.
     *
     * @param resourceId Идентификатор ресурса
     * @return true если ресурс нужно пропустить, false иначе
     */
    private boolean shouldSkipAfkcamResource(Identifier resourceId) {
        return "afkcam".equals(resourceId.getNamespace()) &&
                !AfkcamClient.getConfig().isLoadDefaultAnimation();
    }

    /**
     * Processes a single .bbmodel file and extracts animations from it.
     */
    private List<ParsedAnimation> processSingleBbmodelFile(Identifier id, Resource resource) throws IOException {
        LOGGER.infoDebug("Processing .bbmodel file: " + id);

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            List<ParsedAnimation> animations = BBModelParser.parseAnimationsFromJson(reader);

            if (animations.isEmpty()) {
                LOGGER.errorDebug("No camera animations extracted from file: " + id);
            } else {
                logSuccessfulAnimationExtraction(id, animations);
            }

            return animations;
        }
    }

    /**
     * Logs successful animation extraction with details.
     */
    private void logSuccessfulAnimationExtraction(Identifier id, List<ParsedAnimation> animations) {
        LOGGER.infoDebug("Successfully found " + animations.size() + " camera animations in: " + id);

        animations.forEach(animation ->
                LOGGER.info("- Animation: " + animation.getName() +
                        " (position keys: " + animation.getPositionKeyframes().size() +
                        ", rotation keys: " + animation.getRotationKeyframes().size() + ")")
        );
    }

    /**
     * Applies the loaded animations to the cache and logs the result.
     */
    private void applyCachedAnimations(List<ParsedAnimation> animations) {
        // Thread-safe assignment
        synchronized (CinematicCameraResourceReloadListener.class) {
            cachedAnimations = new ArrayList<>(animations);
        }

        LOGGER.infoDebug("Applied " + animations.size() + " camera animations to cache");

        if (animations.isEmpty()) {
            LOGGER.warnDebug("No camera animations were loaded! Check your resource files.");
        } else {
            logLoadedAnimationNames(animations);
        }
    }

    /**
     * Logs the names of all loaded animations for debugging.
     */
    private void logLoadedAnimationNames(List<ParsedAnimation> animations) {
        animations.forEach(animation ->
                LOGGER.infoDebug("Loaded animation: " + animation.getName())
        );
    }

    /**
     * Returns a thread-safe copy of cached animations.
     *
     * @return Immutable list of cached animations
     */
    public static List<ParsedAnimation> getCachedAnimationsSafe() {
        synchronized (CinematicCameraResourceReloadListener.class) {
            return Collections.unmodifiableList(new ArrayList<>(cachedAnimations));
        }
    }
}