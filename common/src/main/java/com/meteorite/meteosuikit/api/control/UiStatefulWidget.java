package com.meteorite.meteosuikit.api.control;

/** 可绘制且具有语义状态入口的控件；状态由实际实现唯一持有。 */
public interface UiStatefulWidget extends UiWidget, UiStatefulTarget {
    /** {@inheritDoc} */
    @Override boolean isEnabled();
    /** {@inheritDoc} */
    @Override boolean isVisible();
    /** {@inheritDoc} */
    @Override default long focusRevision() { return UiStatefulTarget.super.focusRevision(); }
}
