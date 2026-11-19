package com.meteorite.meteosuikit.fabric.client;

import com.meteorite.meteosuikit.internal.preview.PreviewPlatformBootstrap;
import com.meteorite.meteosuikit.internal.client.UiKitClientLifecycle;
import com.meteorite.meteosuikit.internal.control.UiScreenMouse;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/** 默认预览能力和共享绘制资源由库统一接入 Fabric 客户端生命周期。 */
public final class FabricUiKitClient implements ClientModInitializer {
    private static final ResourceLocation MODAL_MOUSE_PHASE =
            ResourceLocation.fromNamespaceAndPath("meteos_ui_kit", "modal_mouse");

    @Override
    public void onInitializeClient() {
        PreviewPlatformBootstrap.install(new FabricPreviewGeometry());
        ClientTickEvents.END_CLIENT_TICK.register(client -> UiKitClientLifecycle.observe());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> UiKitClientLifecycle.release());
        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> registerMouseEvents(screen));
    }

    private static void registerMouseEvents(Screen screen) {
        var click = ScreenMouseEvents.allowMouseClick(screen);
        click.addPhaseOrdering(MODAL_MOUSE_PHASE, Event.DEFAULT_PHASE);
        click.register(MODAL_MOUSE_PHASE, (current, x, y, button) -> {
            UiScreenMouse mouse = UiScreenMouse.find(current);
            return mouse == null || !mouse.beforePressed(x, y, button);
        });

        var release = ScreenMouseEvents.allowMouseRelease(screen);
        release.addPhaseOrdering(MODAL_MOUSE_PHASE, Event.DEFAULT_PHASE);
        release.register(MODAL_MOUSE_PHASE, (current, x, y, button) -> {
            UiScreenMouse mouse = UiScreenMouse.find(current);
            return mouse == null || !mouse.beforeReleased(x, y, button);
        });

        var scroll = ScreenMouseEvents.allowMouseScroll(screen);
        scroll.addPhaseOrdering(MODAL_MOUSE_PHASE, Event.DEFAULT_PHASE);
        scroll.register(MODAL_MOUSE_PHASE, (current, x, y, sx, sy) -> {
            UiScreenMouse mouse = UiScreenMouse.find(current);
            return mouse == null || !mouse.beforeScrolled(x, y, sx, sy);
        });
    }
}
