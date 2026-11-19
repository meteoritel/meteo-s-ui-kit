package com.meteorite.meteosuikit.internal.control;

import com.meteorite.meteosuikit.api.geometry.UiViewport;
import com.meteorite.meteosuikit.api.input.UiInputBridge;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/** 原版组合根与平台前置鼠标事件共用的派发入口，由 GUI 线程管理挂载。 */
public final class UiScreenMouse {
    private static final Map<Screen, UiScreenMouse> MOUNTED = new IdentityHashMap<>();

    private final UiRootSession session;
    private final UiInputBridge input;
    private @Nullable Screen owner;

    public UiScreenMouse(UiRootSession session, UiInputBridge input) {
        this.session = session;
        this.input = input;
    }

    public void attach(Screen screen) {
        if (owner == screen) return;
        if (owner != null || MOUNTED.putIfAbsent(screen, this) != null) {
            throw new IllegalStateException("A screen and its mouse bridge must be attached one-to-one");
        }
        owner = screen;
    }

    public void detach() {
        if (owner != null) MOUNTED.remove(owner, this);
        owner = null;
    }

    public static @Nullable UiScreenMouse find(Screen screen) {
        return MOUNTED.get(screen);
    }

    public boolean beforePressed(double x, double y, int button) {
        return modalScope() != null && pressed(x, y, button);
    }

    public boolean beforeDragged(double x, double y, int button, double dx, double dy) {
        return ownsSequence(button) && dragged(x, y, button, dx, dy);
    }

    public boolean beforeReleased(double x, double y, int button) {
        return ownsSequence(button) && released(x, y, button);
    }

    public boolean beforeScrolled(double x, double y, double sx, double sy) {
        return modalScope() != null && scrolled(x, y, sx, sy);
    }

    public boolean pressed(double x, double y, int button) {
        return dispatch(x, y, button, input::mousePressed);
    }

    public boolean dragged(double x, double y, int button, double dx, double dy) {
        var pointer = session.preparePointer(x, y, button, modalScope());
        UiViewport viewport = pointer.viewport();
        return session.dispatch(() -> input.mouseDragged(pointer.context(), viewport.toLogical(dx), viewport.toLogical(dy)));
    }

    public boolean released(double x, double y, int button) {
        return dispatch(x, y, button, input::mouseReleased);
    }

    public boolean scrolled(double x, double y, double sx, double sy) {
        return dispatch(x, y, -1, context -> input.mouseScrolled(context, sx, sy));
    }

    private boolean dispatch(double x, double y, int button, Predicate<UiInputContext> event) {
        // 准备可能关闭灯箱，已属于灯箱的事件仍须携带原作用域。
        UiInputContext context = session.preparePointer(x, y, button, modalScope()).context();
        return session.dispatch(() -> event.test(context));
    }

    private boolean ownsSequence(int button) {
        return input.hasMousePress(button) || modalScope() != null;
    }

    private @Nullable UiInputScope modalScope() {
        UiInputScope scope = session.inputScope();
        return scope.content().blocksInput() ? scope : null;
    }
}
