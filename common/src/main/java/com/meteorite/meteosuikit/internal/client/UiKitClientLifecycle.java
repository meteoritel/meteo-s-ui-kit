package com.meteorite.meteosuikit.internal.client;

import com.meteorite.meteosuikit.internal.render.UiModelSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/** 库持有的共享绘制工作区生命周期，独立于可选场景内容。 */
public final class UiKitClientLifecycle {
    private static long generation;
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
            generation++;
            UiModelSurface.release();
            world = minecraft.level;
            language = nextLanguage;
            models = nextModels;
        }
    }

    /** GUI 根用同一资源世代失效自身缓存，读取不触发资源操作。 */
    public static long generation() { return generation; }

    /** 客户端终止时释放共享 GPU 工作区。 */
    public static void release() {
        UiFrameCaptureState.release();
        UiModelSurface.release();
        world = null;
        language = null;
        models = null;
    }
}
