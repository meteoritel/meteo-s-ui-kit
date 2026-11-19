package com.meteorite.meteosuikit.internal.control;

import com.meteorite.meteosuikit.api.control.UiScreenBridge;
import com.meteorite.meteosuikit.api.geometry.UiViewport;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import org.jetbrains.annotations.Nullable;
import com.meteorite.meteosuikit.internal.client.UiKitClientLifecycle;

/** 继承与组合根共用的准备顺序和卸载世代；输入所有权仍由实际 router 持有。 */
public final class UiRootSession {
    private final UiInputRouter router;
    private final UiScreenBridge.Host host;
    private Language resources;
    private long resourceGeneration = -1;
    private long lifecycle;

    public UiRootSession(UiInputRouter router, UiScreenBridge.Host host) {
        this.router = router;
        this.host = host;
    }

    public void beginFrame(long nowMillis) {
        Language current = Language.getInstance();
        long generation = UiKitClientLifecycle.generation();
        if (resources != current || resourceGeneration != generation) {
            host.invalidateUiResources();
            resources = current;
            resourceGeneration = generation;
        }
        host.advanceUiFrame();
        prepare();
        host.inputScope().content().advanceFrame(nowMillis);
    }

    public void prepare() {
        host.prepareUi();
        host.layoutUi();
        selectScope();
    }

    public void selectScope() {
        UiInputScope scope = host.inputScope();
        scope.refreshFocus();
        router.select(scope);
    }

    public UiInputContext pointer(double x, double y, int button) {
        return pointer(x, y, button, null);
    }

    UiInputScope inputScope() { return host.inputScope(); }

    UiViewport viewport() { return host.viewport(); }

    UiInputContext pointer(double x, double y, int button, @Nullable UiInputScope origin) {
        prepare();
        UiViewport viewport = viewport();
        return UiInputContext.of(origin == null ? inputScope() : origin, viewport.toLogical(x), viewport.toLogical(y), button,
                Screen.hasShiftDown(), Screen.hasControlDown(), Screen.hasAltDown());
    }

    public boolean dispatch(BooleanSupplier action) {
        long version = lifecycle;
        boolean consumed = action.getAsBoolean();
        if (version == lifecycle) prepare();
        return consumed;
    }

    public void close() {
        lifecycle++;
        resources = null;
    }
}
