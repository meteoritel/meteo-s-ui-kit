package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.geometry.UiViewport;
import com.meteorite.meteosuikit.api.input.UiInputBridge;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.internal.control.UiRootSession;

/**
 * 原版 Screen 的组合入口：集中准备、布局、选域、坐标转换及事件后刷新。
 * GUI 线程稳定持有；返回 true 时停止父类回退，removed 时调用 close。
 */
public final class UiScreenBridge implements AutoCloseable {
    /** 根提供真实业务钩子；同一 router、scope 和控件树由宿主稳定持有。 */
    public interface Host {
        /** 当前页面或最上层模态的作用域。 */
        UiInputScope inputScope();
        /** 绘制与输入共用的视口；原版容器使用原生 GUI 的恒等视口。 */
        UiViewport viewport();
        /** 应用业务与待发布状态；在每次布局前执行。 */
        default void prepareUi() { }
        /** 更新现有控件几何；输入和绘制共享此布局。 */
        default void layoutUi() { }
        /** 每个绘制帧推进一次宿主动画，事件准备不调用。 */
        default void advanceUiFrame() { }
        /** 首帧或语言、模型资源世代改变后使根拥有的文字与资源缓存失效。 */
        default void invalidateUiResources() { }
    }

    private final Host host;
    private final UiInputBridge input;
    private final UiRootSession session;

    /** 组合本根唯一的 router 和宿主钩子，不创建额外输入状态树。 */
    public UiScreenBridge(UiInputRouter router, Host host) {
        this.host = host;
        input = new UiInputBridge(router);
        session = new UiRootSession(router, host);
    }

    /** 绘制前调用一次，时间与内容树的 advanceFrame 使用同一毫秒时钟。 */
    public void beginFrame(long nowMillis) { session.beginFrame(nowMillis); }
    /** init 或宿主在事件之外改变覆盖面后立即刷新准备、布局和选域。 */
    public void refresh() { session.prepare(); }
    /** 派发按下；消费、覆盖阻断和迟到配对均停止父类回退。 */
    public boolean mousePressed(double x, double y, int button) {
        UiInputContext context = session.pointer(x, y, button);
        return session.dispatch(() -> input.mousePressed(context));
    }
    /** 捕获拖动沿原所有者派发；位置和增量使用相同视口比例。 */
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        UiInputContext context = session.pointer(x, y, button);
        UiViewport viewport = host.viewport();
        return session.dispatch(() -> input.mouseDragged(context, viewport.toLogical(dx), viewport.toLogical(dy)));
    }
    /** 派发配对释放；切域后仍阻断已接住的原按下。 */
    public boolean mouseReleased(double x, double y, int button) {
        UiInputContext context = session.pointer(x, y, button);
        return session.dispatch(() -> input.mouseReleased(context));
    }
    /** 滚轮派发一次，保留原版双轴滚动值。 */
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        UiInputContext context = session.pointer(x, y, -1);
        return session.dispatch(() -> input.mouseScrolled(context, sx, sy));
    }
    /** 准备后派发按键；完成时重新选择实际活动域。 */
    public boolean keyPressed(int key, int scan, int modifiers) {
        session.prepare();
        return session.dispatch(() -> input.keyPressed(key, scan, modifiers));
    }
    /** 按键释放保留根级配对裁决。 */
    public boolean keyReleased(int key, int scan, int modifiers) {
        session.prepare();
        return session.dispatch(() -> input.keyReleased(key, scan, modifiers));
    }
    /** 字符沿活动域的焦点路径派发。 */
    public boolean charTyped(char value, int modifiers) {
        session.prepare();
        return session.dispatch(() -> input.charTyped(value, modifiers));
    }
    /** 先使旧事件完成失效，再关闭配对和 router；同一实例可重新挂载。 */
    @Override public void close() { session.close(); input.close(); }
}
