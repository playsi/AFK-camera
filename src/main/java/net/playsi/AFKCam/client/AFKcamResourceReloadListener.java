package net.playsi.Afkcam.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.playsi.Afkcam.Afkcam;
import net.playsi.Afkcam.client.Animations.AnimationService;
import net.playsi.Afkcam.client.Animations.RawAnimation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Environment(EnvType.CLIENT)
public class AFKcamResourceReloadListener implements SimpleResourceReloadListener<List<RawAnimation>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Afkcam.MOD_NAME + AFKcamResourceReloadListener.class);
    private static final Identifier LISTENER_ID = Identifier.tryParse("cinematic:camera_reload");

    private static final AnimationService ANIM_SERVICE = AnimationService.getInstance();


    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new AFKcamResourceReloadListener());
    }

    @Override
    public Identifier getFabricId() {
        return LISTENER_ID;
    }

    @Override
    public CompletableFuture<List<RawAnimation>> load(ResourceManager resourceManager, /*? if <=1.21.1*/ Profiler profiler, /*?*/ Executor executor) {
        return CompletableFuture.supplyAsync(() -> ANIM_SERVICE.onLoadRPs(resourceManager), executor);
    }

    @Override
    public CompletableFuture<Void> apply(List<RawAnimation> rawAnimations, ResourceManager resourceManager, /*? if <=1.21.1*/ Profiler profiler, /*?*/ Executor executor) {
        return CompletableFuture.runAsync(() -> ANIM_SERVICE.onApplyRPs(rawAnimations), executor);
    }
}