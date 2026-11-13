package com.meteorite.meteosuikit.api.layout;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.geometry.UiViewport;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 一次排版的只读快照节点：矩形、文案与文本带宽都取自排版缓存，供外部诊断与自动化测试读取。
 *
 * <p>快照不参与绘制与命中，只描述「这一帧排成什么样」。{@code textWidth} 是文案的实际测量宽度，
 * {@code textBudget} 是留给文案的容量：文档行给的是行内带宽，超宽时排版只占带宽部分、其余靠悬停滚动补齐；
 * 控件节点给的是内容区宽度。两者都按同一口径参与 {@link #clipped()}，为真表示文案装不下。</p>
 *
 * <p>{@code enabled} 与 {@code visible} 只对控件类节点有意义；行、分隔线与 Frame 这类排版产物恒为真。</p>
 *
 * @param kind 节点种类
 * @param rect 节点矩形（GUI 坐标，绝对位置）
 * @param text 节点文案；无文案时为空文本，永不为 {@code null}
 * @param textWidth 文案测量宽度
 * @param textBudget 文案可用带宽；0 表示该节点不做带宽裁剪
 * @param enabled 节点当前是否可交互
 * @param visible 节点当前是否可见
 * @param children 子节点；叶节点为空列表，永不为 {@code null}
 * @param key 宿主稳定身份；空字符串表示仅展示
 * @param visibleRect 当前坐标空间内已叠加祖先裁剪的矩形
 * @param target 真实输入归属；展示节点为空，不可自动点击
 */
public record UiLayoutNode(Kind kind, UiRect rect, Component text, int textWidth, int textBudget,
                           boolean enabled, boolean visible, List<UiLayoutNode> children,
                           String key, UiRect visibleRect, @Nullable UiInputTarget target) {
    /** 节点种类：控件、文本输入、列表、树、文档行、分隔线、文档 Frame、模态层。 */
    public enum Kind { CONTROL, TEXT_INPUT, LIST, TREE, ROW, DIVIDER, FRAME, MODAL }

    public UiLayoutNode {
        text = text == null ? Component.empty() : text;
        children = List.copyOf(children);
    }

    /** 文案按带宽裁过：可用带宽装不下完整文案。 */
    public boolean clipped() {
        return textBudget > 0 && textWidth > textBudget;
    }

    /** 原有展示节点沿用自身矩形，输入归属由实际所有者补充。 */
    public UiLayoutNode(Kind kind, UiRect rect, Component text, int textWidth, int textBudget,
                        boolean enabled, boolean visible, List<UiLayoutNode> children) {
        this(kind, rect, text, textWidth, textBudget, enabled, visible, children, "", rect, null);
    }

    /** 业务身份与真实目标一同附着；不维护第二个控件登记表。 */
    public UiLayoutNode identified(String key, UiInputTarget target) {
        return new UiLayoutNode(kind, rect, text, textWidth, textBudget, enabled, visible, children,
                key, visibleRect, target);
    }

    /** 仅裁剪可见区域，保留原始几何，递归应用于子内容。 */
    public UiLayoutNode clippedTo(UiRect clip) {
        UiRect effective = visibleRect.intersect(clip);
        return new UiLayoutNode(kind, rect, text, textWidth, textBudget, enabled, visible,
                children.stream().map(child -> child.clippedTo(effective)).toList(), key, effective, target);
    }

    /** 根统一投影到原版 GUI 空间，文字带宽与几何使用相同单位。 */
    public UiLayoutNode projected(UiViewport viewport) {
        return new UiLayoutNode(kind, viewport.project(rect), text,
                (int) Math.ceil(textWidth * viewport.scale()), (int) Math.floor(textBudget * viewport.scale()),
                enabled, visible, children.stream().map(child -> child.projected(viewport)).toList(),
                key, viewport.project(visibleRect).intersect(viewport.bounds()), target);
    }
}
