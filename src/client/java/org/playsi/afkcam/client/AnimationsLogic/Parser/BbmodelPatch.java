package org.playsi.afkcam.client.AnimationsLogic.Parser;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.playsi.afkcam.client.AFKmode.AFKcameraManager;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.Utils.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BbmodelPatch {
    private static final LogUtils LOGGER = new LogUtils(AFKcameraManager.class);

    private static final String CAMERA_RESOURCE_PATH = "cinematic_camera";
    private static final String BBMODEL_EXTENSION = ".bbmodel";

    public List<RawAnimation> loadAnimationsFromResources(ResourceManager manager) {
        List<RawAnimation> animToLoad = new ArrayList<>();

        if (!AfkcamClient.getConfig().isModEnabled()){
            return animToLoad;
        }

        try{
            LOGGER.infoDebug("Looking for .bbmodel files in " + CAMERA_RESOURCE_PATH + ":");
            Map<Identifier, Resource> bbmodelResources = manager.findResources(CAMERA_RESOURCE_PATH,
                    id -> id.getPath().endsWith(BBMODEL_EXTENSION));;
            LOGGER.info("Found " + bbmodelResources.size() + " .bbmodel files");

            if (bbmodelResources.isEmpty()){
                LOGGER.errorDebug("Make sure .bbmodel files are in the correct location:");
                LOGGER.errorDebug("   * assets/<namespace>/" + CAMERA_RESOURCE_PATH + "/*" + BBMODEL_EXTENSION);
            }

            bbmodelResources.forEach((id, resource) -> {
                if (shouldSkipAfkcamResource(id)) {
                    LOGGER.infoDebug("Skipping afkcam resource: " + id + " (default animations disabled)");
                    return;
                }

                try {
                    List<RawAnimation> animations = parseAnimFromBb(id, resource);
                    animToLoad.addAll(animations);

                } catch (Exception e) {
                    LOGGER.errorDebug("Failed to load camera file: " + id + " - " + e.getMessage());
                }
            });
            LOGGER.info("Total camera animations loaded: " + animToLoad.size());
        }   catch (Exception e){
            LOGGER.error(e.getMessage());
        }
    return animToLoad;
    }

    private boolean shouldSkipAfkcamResource(Identifier resourceId) {
        return "afkcam".equals(resourceId.getNamespace()) &&
                !AfkcamClient.getConfig().isLoadDefaultAnimation();
    }
    private List<RawAnimation>parseAnimFromBb(Identifier id, Resource resource) throws IOException {
        LOGGER.infoDebug("Processing .bbmodel file: " + id);

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            List<RawAnimation> animations = BbModelAnim.parseAnimationsFromJson(reader);

            if (animations.isEmpty()) {
                LOGGER.errorDebug("No camera animations extracted from file: " + id);
            } else {
                LOGGER.infoDebug("Successfully found " + animations.size() + " camera animations in: " + id);
            }

            return animations;
        }
    }

}
