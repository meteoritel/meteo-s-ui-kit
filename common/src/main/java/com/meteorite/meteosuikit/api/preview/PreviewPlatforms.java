package com.meteorite.meteosuikit.api.preview;

import com.meteorite.meteosuikit.internal.preview.PreviewPlatformBootstrap;

/** 默认平台能力由库初始化，场景与线程池继续由实际宿主持有。 */
public final class PreviewPlatforms {
    private PreviewPlatforms() { }

    /**
     * 取得本加载器的 Geometry；可用于宿主独立创建的场景所有者。
     *
     * @return 已安装的平台几何适配
     * @throws IllegalStateException 客户端初始化尚未安装平台能力
     */
    public static PreviewScenes.Geometry geometry() {
        return PreviewPlatformBootstrap.geometry();
    }
}
