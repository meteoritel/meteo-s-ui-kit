package com.meteorite.meteosuikit.fabric.client;

import com.meteorite.meteosuikit.internal.preview.PreviewPlatformBootstrap;
import com.meteorite.meteosuikit.internal.client.UiKitClientLifecycle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** 默认预览能力和共享绘制资源由库统一接入 Fabric 客户端生命周期。 */
public final class FabricUiKitClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PreviewPlatformBootstrap.install(new FabricPreviewGeometry());
        ClientTickEvents.END_CLIENT_TICK.register(client -> UiKitClientLifecycle.observe());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UiKitClientLifecycle.release());
    }
}
