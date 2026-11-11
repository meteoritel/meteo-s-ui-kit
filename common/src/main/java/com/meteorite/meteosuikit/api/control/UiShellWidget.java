package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.geometry.UiRect;

/** 使用状态壳的可绘制控件；状态默认行为统一来自非渲染输入目标。 */
public interface UiShellWidget extends UiWidget, UiShellTarget {
    /** {@inheritDoc} */
    @Override default UiRect bounds() { return UiShellTarget.super.bounds(); }
    /** {@inheritDoc} */
    @Override default void setBounds(int x, int y, int width, int height) { UiShellTarget.super.setBounds(x, y, width, height); }
    /** {@inheritDoc} */
    @Override default boolean isEnabled() { return UiShellTarget.super.isEnabled(); }
    /** {@inheritDoc} */
    @Override default boolean isVisible() { return UiShellTarget.super.isVisible(); }
    /** {@inheritDoc} */
    @Override default long focusRevision() { return UiShellTarget.super.focusRevision(); }
}
