package com.meteorite.meteosuikit.api.preview;

import com.meteorite.meteosuikit.internal.preview.PreviewPlatformBootstrap;

/** 默认平台能力由库初始化，场景与线程池继续由实际宿主持有。 */
public final class PreviewPlatforms {
    private PreviewPlatforms() { }

    /**
     * 取得本加载器的 Geometry；可用于宿主独立创建的场景所有者。
     *
     * <p>客户端线程在安装完成后读取。Fabric 在 client initialize 安装；NeoForge 在
     * FMLClientSetupEvent 的 enqueueWork 任务安装，宿主在首次 Screen 构造或之后读取。</p>
     *
     * @return 已安装的平台几何适配
     * @throws IllegalStateException 客户端初始化尚未安装平台能力
     */
    public static PreviewScenes.Geometry geometry() {
        return PreviewPlatformBootstrap.geometry();
    }
}
