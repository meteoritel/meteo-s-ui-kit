package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiScreenBridge;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.geometry.UiViewport;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

final class RootContract extends ContractSupport {
    static void run() {
        closesDuringFrame();
        List<String> calls = new ArrayList<>();
        UiInputTarget target = new UiInputTarget() {
            @Override public void advanceFrame(long time) { calls.add("content:" + time); }
            @Override public boolean keyPressed(int key, int scan, int mods) { calls.add("key"); return false; }
        };
        UiInputScope scope = new UiInputScope(target);
        UiInputRouter router = new UiInputRouter();
        UiScreenBridge bridge = new UiScreenBridge(router, new UiScreenBridge.Host() {
            @Override public UiInputScope inputScope() { return scope; }
            @Override public UiViewport viewport() { return new UiViewport(1, 320, 240, new UiRect(0, 0, 320, 240)); }
            @Override public void prepareUi() { calls.add("prepare"); }
            @Override public void layoutUi() { calls.add("layout"); }
            @Override public void advanceUiFrame() { calls.add("frame"); }
            @Override public void invalidateUiResources() { calls.add("invalidate"); }
        });
        bridge.beginFrame(42);
        check(calls.equals(List.of("invalidate", "frame", "prepare", "layout", "content:42")), "frame ordering");
        calls.clear();
        check(!bridge.keyPressed(GLFW.GLFW_KEY_A, 0, 0), "unconsumed key falls back");
        check(calls.equals(List.of("prepare", "layout", "key", "prepare", "layout")), "events prepare before and after without frame advancement");
        scope.setNavigationBack(bridge::close);
        calls.clear();
        check(bridge.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0), "closing bridge stops fallback");
        check(router.active() == null && calls.equals(List.of("prepare", "layout", "key")), "closing callback does not reactivate root");
        bridge.beginFrame(43);
        check(router.active() == scope, "same bridge can mount again");
        ClosingScreen screen = new ClosingScreen();
        bridge.attach(screen);
        bridge.attach(screen);
        bridge.close();
        bridge.attach(new ClosingScreen());
        bridge.close();
        bridge.attach(screen);
        bridge.close();
        check(screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0) && !screen.hasActiveScope(), "inherited Adapter retains close semantics");
    }

    private static void closesDuringFrame() {
        List<String> phases = List.of("invalidate", "frame", "prepare", "layout", "content");
        for (String closeIn : phases.subList(0, 4)) {
            List<String> visited = new ArrayList<>();
            UiScreenBridge[] bridge = new UiScreenBridge[1];
            UiInputScope scope = new UiInputScope(new UiInputTarget() {
                @Override public void advanceFrame(long time) { visited.add("content"); }
            });
            UiInputRouter router = new UiInputRouter();
            bridge[0] = new UiScreenBridge(router, new UiScreenBridge.Host() {
                private boolean closed;

                private void visit(String phase) {
                    visited.add(phase);
                    if (!closed && phase.equals(closeIn)) {
                        closed = true;
                        bridge[0].close();
                    }
                }

                @Override public UiInputScope inputScope() { return scope; }
                @Override public UiViewport viewport() { return new UiViewport(1, 320, 240, new UiRect(0, 0, 320, 240)); }
                @Override public void invalidateUiResources() { visit("invalidate"); }
                @Override public void advanceUiFrame() { visit("frame"); }
                @Override public void prepareUi() { visit("prepare"); }
                @Override public void layoutUi() { visit("layout"); }
            });
            bridge[0].beginFrame(0);
            check(router.active() == null && visited.equals(phases.subList(0, phases.indexOf(closeIn) + 1)),
                    "closing during " + closeIn + " stops the remaining frame steps");
            visited.clear();
            bridge[0].beginFrame(1);
            check(router.active() == scope && visited.equals(phases), "closing during a frame permits remount");
            bridge[0].close();
        }
    }
}
