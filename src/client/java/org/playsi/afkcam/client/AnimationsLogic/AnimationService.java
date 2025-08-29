package org.playsi.afkcam.client.AnimationsLogic;

import net.minecraft.resource.ResourceManager;
import org.playsi.afkcam.client.AFKmode.AFKcameraManager;
import org.playsi.afkcam.client.AnimationsLogic.Parser.BbmodelPatch;
import org.playsi.afkcam.client.Utils.LogUtils;
import org.playsi.afkcam.client.AnimationsLogic.Parser.RawAnimation;

import java.util.*;

public class AnimationService {
    private static final LogUtils LOGGER = new LogUtils(AFKcameraManager.class);

    private AnimationService() {}
    private final BbmodelPatch animationLoader = new BbmodelPatch();

    private List<RawAnimation> loadedAnimations = new ArrayList<>();
    private Map<String, RawAnimation> animationCache = new HashMap<>();

    private static class Holder {
        private static final AnimationService INSTANCE = new AnimationService();
    }

    public static AnimationService getInstance() {
        return Holder.INSTANCE;
    }

    public List<RawAnimation> onLoadRPs(ResourceManager manager){
        return animationLoader.loadAnimationsFromResources(manager);
    }

    public void onApplyRPs(List<RawAnimation> animations) {
        synchronized (this) {
            loadedAnimations.clear();
            loadedAnimations.addAll(animations);
            updateCache();
        }
        LOGGER.infoDebug("Applied " + animations.size() + " camera animations");
        if (animations.isEmpty()) {
            LOGGER.warnDebug("No camera animations were loaded! Check your resource files.");
        }
    }

    public List<RawAnimation> getAllAnimations() {
        return new ArrayList<>(loadedAnimations);
    }

    public RawAnimation findAnimationByName(String name) {
        return animationCache.get(name);
    }

    public boolean hasAnimation(String name) {
        return animationCache.containsKey(name);
    }

    private void updateCache() {
        animationCache.clear();
        for (RawAnimation animation : loadedAnimations) {
            animationCache.put(animation.getName(), animation);
        }
    }
    


}
