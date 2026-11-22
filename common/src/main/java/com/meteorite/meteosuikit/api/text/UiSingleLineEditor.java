package com.meteorite.meteosuikit.api.text;

import com.meteorite.meteosuikit.api.control.UiWidget;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import java.util.function.Consumer;
import org.jetbrains.annotations.Nullable;

/** 单行文本的提交、取消与会话通知；文本模型和呈现由实际编辑控件持有。 */
public interface UiSingleLineEditor extends UiWidget, UiTextValue {
    /** 设置 Enter 提交通知。 */
    UiSingleLineEditor setOnCommit(Consumer<String> listener);

    /** 设置 Esc 传到编辑器时的取消通知。路由器优先用 Esc 结束活动交互。 */
    UiSingleLineEditor setOnCancel(Runnable listener);

    /** 设置文本交互结束通知，组合控件据此提交或恢复自己的值。 */
    UiSingleLineEditor setOnInteractionEnd(@Nullable Consumer<UiInteractionSession.EndReason> listener);
}
