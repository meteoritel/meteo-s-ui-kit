package com.meteorite.meteosuikit.internal.preview;

import com.meteorite.meteosuikit.api.preview.PreviewScenes;
import org.jetbrains.annotations.Nullable;

/** 默认预览平台能力的内部登记；共享绘制资源由客户端生命周期持有。 */
public final class PreviewPlatformBootstrap {
    private static @Nullable PreviewScenes.Geometry geometry;

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

}
