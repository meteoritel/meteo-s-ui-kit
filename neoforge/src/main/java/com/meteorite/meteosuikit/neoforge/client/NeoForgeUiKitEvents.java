package com.meteorite.meteosuikit.neoforge.client;

import com.meteorite.meteosuikit.internal.client.UiKitClientLifecycle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/** 共享工作区资源的观察和终止释放只在客户端运行。 */
@EventBusSubscriber(modid = "meteos_ui_kit", value = Dist.CLIENT)
public final class NeoForgeUiKitEvents {
    private NeoForgeUiKitEvents() { }

    /** 资源和世界身份变化后释放共享绘制工作区。 */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        UiKitClientLifecycle.observe();
    }

    /** 释放库持有的 GPU 工作区。 */
    @SubscribeEvent
    public static void stopping(GameShuttingDownEvent event) {
        UiKitClientLifecycle.release();
    }
}
