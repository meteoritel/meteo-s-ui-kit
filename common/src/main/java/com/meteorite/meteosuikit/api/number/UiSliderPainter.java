package com.meteorite.meteosuikit.api.number;

import com.meteorite.meteosuikit.api.geometry.UiRect;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 滑块绘制策略：把几何与状态交给实现方，实现方决定像素画法。
 *
 * <p>kit 只提供一帧几何（轨道、已填充段、手柄、文字槽）与状态标志，不描述美术细节，因此宿主
 * 可以替换成贴图绘制而不改变控件逻辑。实现方不得修改控件状态。</p>
 */
public interface UiSliderPainter {

    /**
     * 一帧只读绘制数据。
     *
     * @param bounds        控件整体矩形
     * @param track         轨道矩形
     * @param filled        已填充段矩形，宽度可为 0
     * @param thumb         手柄矩形
     * @param textSlot      数值文字槽矩形，宽度可为 0
     * @param style         注入样式
     * @param enabled       是否可用
     * @param hovered       指针是否悬停
     * @param pressed       是否按下
     * @param focused       是否聚焦
     * @param dragging      是否正在拖动
     * @param error         是否处于错误状态
     * @param overflowBelow 当前值低于显示窗口下沿
     * @param overflowAbove 当前值高于显示窗口上沿
     */
    record Frame(UiRect bounds, UiRect track, UiRect filled, UiRect thumb, UiRect textSlot,
                 UiSliderStyle style, boolean enabled, boolean hovered, boolean pressed,
                 boolean focused, boolean dragging, boolean error,
                 boolean overflowBelow, boolean overflowAbove) {
    }

    /** 绘制完整轨道、指定手柄与整控件标记；文字由控件随后绘制。 */
    void paint(GuiGraphics graphics, Frame frame);

    /**
     * 仅绘制指定手柄及其装饰；Frame 保留真实控件、轨道、填充和文字槽几何，状态属于该端点。
     * 零尺寸手柄跳过绘制；轨道、文字与整控件错误标记由完整绘制负责。
     */
    void paintThumb(GuiGraphics graphics, Frame frame);
}
