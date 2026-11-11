package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiFocusState;
import com.meteorite.meteosuikit.api.input.UiFocusTarget;

/**
 * 通过组合复用本地状态的输入目标；渲染与具体激活由调用方提供。
 */
public interface UiShellTarget extends UiFocusTarget {
    /**
     * 当前控件的状态壳，包装与核心可返回同一实例。
     */
    UiWidgetShell shell();

    /**
     * {@inheritDoc}
     */
    @Override
    default UiRect bounds() {
        return shell().bounds();
    }

    /** 几何由同一个状态壳持有。 */
    default void setBounds(int x, int y, int width, int height) {
        shell().setBounds(x, y, width, height);
    }

    /** 当前可用性，禁用时由状态壳结束交互。 */
    default boolean isEnabled() {
        return shell().isEnabled();
    }

    /** 当前可见性，隐藏时由状态壳结束交互。 */
    default boolean isVisible() {
        return shell().isVisible();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean canFocus() {
        return shell().canFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default UiFocusState focusState() {
        return shell().focusState();
    }
    /** 状态变化使父内容重新核对焦点资格，几何变化保持当前结构。 */
    @Override
    default long focusRevision() { return (isVisible() ? 1 : 0) | (isEnabled() ? 2 : 0); }
}
