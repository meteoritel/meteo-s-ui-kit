package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiFocusTarget;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.List;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import java.util.function.Consumer;

/**
 * 屏幕坐标内容的滚动输入组合；普通命中受视口裁剪，捕获输入仍由 scope 直达目标。
 * 内容自行按滚动偏移布局和绘制，本类不再执行内容坐标变换。
 */
public final class UiScrollContainer implements UiInputTarget, UiLayoutSource {
    private final UiScrollView scroll;
    private final UiInputTarget content;
    private final UiChildren<UiInputTarget> members = new UiChildren<>();

    /** 读取真实内容布局并叠加同一滚动视口裁剪。 */
    @Override public List<UiLayoutNode> layoutNodes() {
        return members.layoutNodes().stream().map(node -> node.clippedTo(scroll.viewport())).toList();
    }

    /** 内容与滚动视图由调用方稳定持有，保留成员结构只在本类内维护。 */
    public UiScrollContainer(UiScrollView scroll, UiInputTarget content) {
        this.scroll = Objects.requireNonNull(scroll, "scroll");
        this.content = Objects.requireNonNull(content, "content");
        members.add(content);
        members.add(scroll);
    }

    /** 滚动条优先于内容，空白视口保留本容器的输入归属。 */
    @Override public @Nullable UiPointerHit hitTest(double x, double y) {
        if (!scroll.contains(x, y)) return null;
        UiPointerHit hit = scroll.hitTest(x, y);
        if (hit == null) hit = content.hitTest(x, y);
        return hit == null ? UiPointerHit.leaf(this, x, y) : hit.within(this);
    }

    /** {@inheritDoc} */
    @Override public void addFocusTargets(Consumer<UiFocusTarget> out) { members.addFocusTargets(out); }
    /** {@inheritDoc} */
    @Override public long focusRevision() { return members.focusRevision(); }
    /** {@inheritDoc} */
    @Override public boolean hasFocus() { return members.hasFocus(); }
    /** {@inheritDoc} */
    @Override public void scopeExited(UiInteractionSession.EndReason reason) { members.scopeExited(reason); }
    /** {@inheritDoc} */
    @Override public boolean mousePressed(UiInputContext context) { return members.mousePressed(context); }
    /** {@inheritDoc} */
    @Override public boolean mouseReleased(UiInputContext context) { return members.mouseReleased(context); }
    /** 子内容先消费滚轮，未消费时移动本视口。 */
    @Override public boolean mouseScrolled(UiInputContext context, double x, double y) {
        if (!context.hits(this)) return false;
        return members.mouseScrolled(context, x, y) || scroll.scrollBy(y);
    }
    /** {@inheritDoc} */
    @Override public boolean keyPressed(int key, int scan, int modifiers) { return members.keyPressed(key, scan, modifiers); }
    /** {@inheritDoc} */
    @Override public boolean keyReleased(int key, int scan, int modifiers) { return members.keyReleased(key, scan, modifiers); }
    /** {@inheritDoc} */
    @Override public boolean charTyped(char value, int modifiers) { return members.charTyped(value, modifiers); }
}
