package com.meteorite.meteosuikit.neoforge.client;

import com.meteorite.meteosuikit.internal.preview.PreviewPlatformBootstrap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** 安装平台能力排到客户端主线程，宿主无需复制平台 Geometry。 */
@EventBusSubscriber(modid = "meteos_ui_kit", value = Dist.CLIENT)
public final class NeoForgeUiKitClient {
    private NeoForgeUiKitClient() { }

    /** 完成初始化后公开默认平台能力。 */
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PreviewPlatformBootstrap.install(new NeoForgePreviewGeometry()));
    }
}
