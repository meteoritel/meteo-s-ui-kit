package com.meteorite.meteosuikit.api.input;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * kit 与原版父类之间的输入回退桥接，在 GUI 线程由组合接入的根持有。
 * <p>根先准备布局并在同一个 router 上选域，再调用本类；返回 true 时停止父类回退。
 * 已接住的按下保留根级配对，切域后到达的拖动与释放仍被接住。
 * 目标、焦点和持续交互仍由 router 与作用域拥有；根卸载调用 {@link #close()}。
 */
public final class UiInputBridge {
    private final UiInputRouter router;
    private final Set<Integer> claimedButtons = new HashSet<>();
    private final Set<Integer> claimedKeys = new HashSet<>();
    private long lifecycle;

    /** 组合本根唯一的 router；选域继续由宿主执行。 */
    public UiInputBridge(UiInputRouter router) {
        this.router = Objects.requireNonNull(router, "router");
    }

    /** 根是否仍持有该鼠标键的按下，用于在屏幕前置事件中接住配对操作。 */
    public boolean hasMousePress(int button) {
        return claimedButtons.contains(button);
    }

    /** 派发按下；消费或阻断后接住该鼠标键的配对拖动与释放。 */
    public boolean mousePressed(UiInputContext context) {
        long version = lifecycle;
        boolean blocked = blocksInput(context.scope());
        claimedButtons.remove(context.button());
        boolean consumed = router.mousePressed(context);
        boolean handled = stopsFallback(blocked, consumed, version);
        if (handled && lifecycle == version) claimedButtons.add(context.button());
        return handled;
    }

    /** 捕获拖动交给 router；已接住的按下即使切域也不回退父类。 */
    public boolean mouseDragged(UiInputContext context, double dragX, double dragY) {
        long version = lifecycle;
        boolean blocked = blocksInput(context.scope()) || hasMousePress(context.button());
        boolean consumed = router.mouseDragged(context, dragX, dragY);
        return stopsFallback(blocked, consumed, version);
    }

    /** 先摘除根级配对，再派发释放，允许结束回调重入或卸载根。 */
    public boolean mouseReleased(UiInputContext context) {
        long version = lifecycle;
        boolean claimed = claimedButtons.remove(context.button());
        boolean blocked = blocksInput(context.scope()) || claimed;
        boolean consumed = router.mouseReleased(context);
        return stopsFallback(blocked, consumed, version);
    }

    /** 滚轮只派发一次；派发前后任一活动域阻断时停止回退。 */
    public boolean mouseScrolled(UiInputContext context, double scrollX, double scrollY) {
        long version = lifecycle;
        boolean blocked = blocksInput(context.scope());
        boolean consumed = router.mouseScrolled(context, scrollX, scrollY);
        return stopsFallback(blocked, consumed, version);
    }

    /** 接住消费或阻断的键按下及其重复、释放，覆盖面关闭后继续保持配对。 */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        long version = lifecycle;
        boolean blocked = blocksInput() || claimedKeys.contains(keyCode);
        boolean consumed = router.keyPressed(keyCode, scanCode, modifiers);
        boolean handled = stopsFallback(blocked, consumed, version);
        if (handled && lifecycle == version) claimedKeys.add(keyCode);
        return handled;
    }

    /** 先摘除按键配对，再让 router 完成当前键会话或修饰态清理。 */
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        long version = lifecycle;
        boolean claimed = claimedKeys.remove(keyCode);
        boolean blocked = blocksInput() || claimed;
        boolean consumed = router.keyReleased(keyCode, scanCode, modifiers);
        return stopsFallback(blocked, consumed, version);
    }

    /** 字符沿 router 的焦点路径派发；覆盖面关闭回调不使本次字符穿透。 */
    public boolean charTyped(char codePoint, int modifiers) {
        long version = lifecycle;
        boolean blocked = blocksInput();
        boolean consumed = router.charTyped(codePoint, modifiers);
        return stopsFallback(blocked, consumed, version);
    }

    /**
     * 卸载根时先清空配对再关闭 router；可重复调用，再次挂载从空配对开始。
     * 当前事件回调内卸载后，派发返回不会重新登记旧根按下。
     */
    public void close() {
        lifecycle++;
        claimedButtons.clear();
        claimedKeys.clear();
        router.close();
    }

    private boolean blocksInput() {
        return blocksInput(router.active());
    }

    private static boolean blocksInput(@Nullable UiInputScope scope) {
        return scope != null && scope.content().blocksInput();
    }

    private boolean stopsFallback(boolean blockedBefore, boolean consumed, long version) {
        return blockedBefore || consumed || lifecycle != version || blocksInput();
    }
}
