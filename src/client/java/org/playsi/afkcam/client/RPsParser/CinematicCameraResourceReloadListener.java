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
import org.playsi.afkcam.client.Utils.CameraAnimation;
import org.playsi.afkcam.client.Utils.LogUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Reload listener that scans cinematic_camera .bbmodel files and parses camera animations.
 */
@Environment(EnvType.CLIENT)
public class CinematicCameraResourceReloadListener implements SimpleResourceReloadListener<List<CameraAnimation>> {
    private static final LogUtils LOGGER = new LogUtils(BBModelParser.class);
    private static final Identifier ID = Identifier.of("cinematic:camera_reload");
    private static final String BBE_PATH = "cinematic_camera";

    @Getter
    private static List<CameraAnimation> cachedAnimations = new ArrayList<>();

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new CinematicCameraResourceReloadListener());
    }

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public CompletableFuture<List<CameraAnimation>> load(ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            if (!AfkcamClient.getConfig().isModEnabled()) {
                return new ArrayList<>();
            }
            List<CameraAnimation> animations = new ArrayList<>();

            Map<Identifier, Resource> resources = manager.findResources(BBE_PATH, id -> true);
            if (resources.isEmpty()) {
                LOGGER.errorDebug("- NO resources found in " + BBE_PATH);
            } else {
                LOGGER.infoDebug("- Found " + resources.size() + " resources in " + BBE_PATH);
                for (Identifier id : resources.keySet()) {
                    LOGGER.infoDebug("   * " + id);
                }
            }

            // Check for .bbmodel files specifically
            LOGGER.infoDebug("Looking for .bbmodel files in " + BBE_PATH + ":");
            Map<Identifier, Resource> bbmodelResources = manager.findResources(BBE_PATH, id -> id.getPath().endsWith(".bbmodel"));
            if (bbmodelResources.isEmpty()) {
                LOGGER.errorDebug("- NO .bbmodel files found! This is the main issue.");
                LOGGER.errorDebug("- Make sure .bbmodel files are in the correct location:");
                LOGGER.errorDebug("   * assets/<namespace>/cinematic_camera/*.bbmodel");
            } else {
                LOGGER.info(" Found " + bbmodelResources.size() + " .bbmodel files");

                // Process each .bbmodel file
                bbmodelResources.forEach((id, resource) -> {
                    LOGGER.infoDebug("Processing .bbmodel file: " + id);
                    try (InputStream is = resource.getInputStream();
                         BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(is, StandardCharsets.UTF_8))) {

                        // Use BBModelParser to parse the file
                        List<CameraAnimation> parsedAnimations = BBModelParser.parseAnimationsFromJson(reader);

                        if (parsedAnimations.isEmpty()) {
                            LOGGER.errorDebug("No camera animations extracted from file: " + id);
                        } else {
                            LOGGER.infoDebug("Successfully found " + parsedAnimations.size() + " camera animations in: " + id);
                            for (CameraAnimation anim : parsedAnimations) {
                                LOGGER.info("- Animation: " + anim.getName() +
                                        " (pos keys: " + anim.getPositionKeyframes().size() +
                                        ", rot keys: " + anim.getRotationKeyframes().size() + ")");
                            }
                            animations.addAll(parsedAnimations);
                        }
                    } catch (Exception e) {
                        LOGGER.errorDebug("Failed to load camera file: " + id + e);
                    }
                });
            }

            LOGGER.info("Total camera animations loaded: " + animations.size());
            return animations;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(List<CameraAnimation> animations, ResourceManager manager, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            cachedAnimations = animations;
            LOGGER.infoDebug("Loaded camera animations: " + animations.size());
            if (animations.isEmpty()) {
                LOGGER.warnDebug("No camera animations were loaded! Check your resource files.");
            } else {
                // Print loaded animation names for debugging
                animations.forEach(anim -> LOGGER.infoDebug("Loaded animation: " + anim.getName()));
            }
        }, executor);
    }
}