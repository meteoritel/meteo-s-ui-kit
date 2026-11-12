package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.input.UiFocusTarget;

/** 控件的几何与输入资格；具体实现负责同步其模型、焦点和子会话。 */
public interface UiStatefulTarget extends UiFocusTarget {
    /** 实际输入资格，独立于只读或业务编辑政策。 */
    boolean isEnabled();
    /** 是否参与绘制、命中和焦点序列。 */
    boolean isVisible();
    /** 改变资格并结清失效交互，返回当前目标。 */
    UiStatefulTarget setEnabled(boolean enabled);
    /** 改变可见性并结清失效交互，返回当前目标。 */
    UiStatefulTarget setVisible(boolean visible);
    /** 更新实际逻辑矩形，保持内容与局部编辑状态。 */
    void setBounds(int x, int y, int width, int height);
    /** 设置宿主提供的稳定布局身份。 */
    void setLayoutKey(String key);
    /** 具体控件可叠加只读选择或非空数据等资格。 */
    @Override default boolean canFocus() { return isVisible() && isEnabled(); }
    /** 资格变化通知父内容核对焦点，几何变化保持结构。 */
    @Override default long focusRevision() { return (isVisible() ? 1 : 0) | (isEnabled() ? 2 : 0); }
}
