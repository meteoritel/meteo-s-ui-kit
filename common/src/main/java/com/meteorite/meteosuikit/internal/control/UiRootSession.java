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
    public record Pointer(UiInputContext context, UiViewport viewport) { }

    private final UiInputRouter router;
    private final UiScreenBridge.Host host;
    private Language resources;
    private long resourceGeneration = -1;
    private long lifecycle;

    public UiRootSession(UiInputRouter router, UiScreenBridge.Host host) {
        this.router = router;
        this.host = host;
    }

    public boolean beginFrame(long nowMillis) {
        long version = lifecycle;
        Language current = Language.getInstance();
        long generation = UiKitClientLifecycle.generation();
        if (resources != current || resourceGeneration != generation) {
            host.invalidateUiResources();
            if (version != lifecycle) return false;
            resources = current;
            resourceGeneration = generation;
        }
        host.advanceUiFrame();
        if (version != lifecycle || !prepare(version)) return false;
        host.inputScope().content().advanceFrame(nowMillis);
        return version == lifecycle;
    }

    public void prepare() {
        prepare(lifecycle);
    }

    private boolean prepare(long version) {
        host.prepareUi();
        if (version != lifecycle) return false;
        host.layoutUi();
        if (version != lifecycle) return false;
        selectScope();
        return version == lifecycle;
    }

    public void selectScope() {
        long version = lifecycle;
        UiInputScope scope = host.inputScope();
        scope.refreshFocus();
        if (version == lifecycle) router.select(scope);
    }

    public UiInputContext pointer(double x, double y, int button) {
        return preparePointer(x, y, button, null).context();
    }

    UiInputScope inputScope() { return host.inputScope(); }

    UiViewport viewport() { return host.viewport(); }

    public Pointer preparePointer(double x, double y, int button, @Nullable UiInputScope origin) {
        prepare();
        UiViewport viewport = viewport();
        UiInputContext context = UiInputContext.of(origin == null ? inputScope() : origin, viewport.toLogical(x),
                viewport.toLogical(y), button, Screen.hasShiftDown(), Screen.hasControlDown(), Screen.hasAltDown());
        return new Pointer(context, viewport);
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
