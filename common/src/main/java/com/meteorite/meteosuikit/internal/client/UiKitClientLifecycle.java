package com.meteorite.meteosuikit.internal.client;

import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/** 库持有的共享绘制工作区生命周期，独立于可选场景内容。 */
public final class UiKitClientLifecycle {
    private static @Nullable Object world;
    private static @Nullable Object language;
    private static @Nullable Object models;

    private UiKitClientLifecycle() { }

    /** 客户端 tick 观察共享工作区的资源身份，宿主自行使业务缓存失效。 */
    public static void observe() {
        Minecraft minecraft = Minecraft.getInstance();
        Object nextModels = minecraft.getBlockRenderer().getBlockModelShaper()
                .getBlockModel(Blocks.STONE.defaultBlockState());
        Object nextLanguage = Language.getInstance();
        if (world != minecraft.level || language != nextLanguage || models != nextModels) {
            UiRenderLayers.releaseModels();
            world = minecraft.level;
            language = nextLanguage;
            models = nextModels;
        }
    }

    /** 客户端终止时释放共享 GPU 工作区。 */
    public static void release() {
        UiFrameCaptureState.release();
        UiRenderLayers.releaseModels();
        world = null;
        language = null;
        models = null;
    }
}
