package com.meteorite.meteosuikit.neoforge.client;

import com.meteorite.meteosuikit.internal.client.UiKitClientLifecycle;
import com.meteorite.meteosuikit.internal.client.UiFrameCaptureState;
import com.meteorite.meteosuikit.internal.control.UiScreenMouse;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/** 客户端共用资源生命周期和组合屏幕的模态鼠标接入。 */
@EventBusSubscriber(modid = "meteos_ui_kit", value = Dist.CLIENT)
public final class NeoForgeUiKitEvents {
    private NeoForgeUiKitEvents() { }

    /** 整帧完成后才采集包含 Screen 的帧缓冲。 */
    @SubscribeEvent
    public static void frameEnded(RenderFrameEvent.Post event) { UiFrameCaptureState.frameEnded(); }

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

    /** 模态按下在普通屏幕监听器之前派发。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        UiScreenMouse mouse = UiScreenMouse.find(event.getScreen());
        if (mouse != null && mouse.beforePressed(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    /** 拖动沿根已接住的鼠标按下派发。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        UiScreenMouse mouse = UiScreenMouse.find(event.getScreen());
        if (mouse != null && mouse.beforeDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(),
                event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    /** 覆盖层关闭后，配对释放仍送回同一根。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        UiScreenMouse mouse = UiScreenMouse.find(event.getScreen());
        if (mouse != null && mouse.beforeReleased(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    /** 模态滚轮由顶层内容处理。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        UiScreenMouse mouse = UiScreenMouse.find(event.getScreen());
        if (mouse != null && mouse.beforeScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(),
                event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }
}
