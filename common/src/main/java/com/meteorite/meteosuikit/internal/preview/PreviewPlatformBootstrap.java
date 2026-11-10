package com.meteorite.meteosuikit.internal.preview;

import com.meteorite.meteosuikit.api.preview.PreviewScenes;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/** 平台入口的内部接线；只持有默认能力和共享工作区的资源身份。 */
public final class PreviewPlatformBootstrap {
    private static @Nullable PreviewScenes.Geometry geometry;
    private static @Nullable Object world;
    private static @Nullable Object language;
    private static @Nullable Object models;

    private PreviewPlatformBootstrap() { }

    /** 平台客户端初始化时安装本端实现。 */
    public static void install(PreviewScenes.Geometry value) {
        geometry = value;
    }

    /** 仅在平台完成安装后提供默认能力，避免以空场景掩盖接线错误。 */
    public static PreviewScenes.Geometry geometry() {
        if (geometry == null) throw new IllegalStateException("Meteo's UI Kit 客户端平台尚未初始化");
        return geometry;
    }

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
        UiRenderLayers.releaseModels();
        world = null;
        language = null;
        models = null;
    }
}
