package org.playsi.afkcam.client.RPsParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.playsi.afkcam.client.AnimationsLogic.AnimationService;
import org.playsi.afkcam.client.AnimationsLogic.Parser.RawAnimation;
import org.playsi.afkcam.client.Utils.LogUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Environment(EnvType.CLIENT)
public class AFKcamResourceReloadListener implements SimpleResourceReloadListener<List<RawAnimation>> {

    private static final LogUtils LOGGER = new LogUtils(AFKcamResourceReloadListener.class);
    private static final Identifier LISTENER_ID = Identifier.of("cinematic:camera_reload");

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
    public CompletableFuture<List<RawAnimation>> load(ResourceManager manager, Executor executor) {
        return CompletableFuture.supplyAsync(() -> ANIM_SERVICE.onLoadRPs(manager), executor);
    }

    @Override
    public CompletableFuture<Void> apply(List<RawAnimation> animations, ResourceManager manager, Executor executor) {
        return CompletableFuture.runAsync(() -> ANIM_SERVICE.onApplyRPs(animations), executor);
    }
}