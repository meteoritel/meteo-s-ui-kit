package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.input.UiFocusTarget;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import net.minecraft.network.chat.Component;

/** 普通与宿主特殊按钮的共同接入面，供模态等容器统一布局、快照和焦点。 */
public interface UiButtonWidget extends UiWidget, UiFocusTarget, UiLayoutSource {
    /** 普通按钮的资格由自身可见性与可用性决定。 */
    @Override
    default boolean canFocus() { return isVisible() && isEnabled(); }
    /** 使用自身字体量取标签、图标和留白。 */
    int preferredWidth(int padding);
    /** 原始本地化标签。 */
    Component label();
    /** 更新可用性并结束失效交互。 */
    UiButtonWidget setEnabled(boolean enabled);
}
