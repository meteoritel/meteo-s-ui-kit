package com.meteorite.meteosuikit.api.style;

import java.util.Objects;

/**
 * 每个 UI 根持有的主题发布状态。子控件通过稳定 Supplier 读取 current；替换只发布主题与版本，编辑状态继续由控件持有。
 * 在 GUI 线程调用；指针手势结束后的发布时机与布局刷新由根负责。
 *
 * @param <T> 宿主可保留自己的主题扩展
 */
public class UiThemeContext<T extends UiStyleSource> {
    private T current;
    private long revision;

    /**
     * 创建指定初始主题的上下文。
     */
    public UiThemeContext(T initial) {
        current = Objects.requireNonNull(initial, "initial");
    }

    /**
     * 创建可替换、默认使用原版主题的上下文。
     */
    public static UiThemeContext<UiStyleSource> minecraft() {
        return new UiThemeContext<>(UiMinecraftTheme.INSTANCE);
    }

    /**
     * 当前不可变主题。
     */
    public final T current() {
        return current;
    }

    /**
     * 发布新主题；同一实例保持版本。
     */
    public final void setTheme(T next) {
        Objects.requireNonNull(next, "theme");
        if (current == next) return;
        current = next;
        revision++;
    }

    /** 根准备与布局使用的主题发布版本。 */
    public final long revision() { return revision; }
}
