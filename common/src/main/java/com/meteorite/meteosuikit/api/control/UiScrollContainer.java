package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiFocusTarget;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.List;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

/**
 * 屏幕坐标内容的滚动输入组合；普通命中受视口裁剪，捕获输入仍由 scope 直达目标。
 * 内容自行按滚动偏移布局和绘制，本类不再执行内容坐标变换。
 */
public final class UiScrollContainer implements UiInputTarget, UiLayoutSource {
    /** 内容在屏幕逻辑坐标中绘制，指针已按视口和滚动条限制。 */
    @FunctionalInterface public interface Painter {
        void render(UiPointer pointer);
    }

    private final UiScrollView scroll;
    private final UiInputTarget content;
    private final UiChildren<UiInputTarget> members = new UiChildren<>();
    private final Runnable project;
    private final DoubleUnaryOperator wheelPixels;

    /** 读取真实内容布局并叠加同一滚动视口裁剪。 */
    @Override public List<UiLayoutNode> layoutNodes() {
        return members.layoutNodes().stream().map(node -> node.clippedTo(scroll.viewport())).toList();
    }

    /** 内容与滚动视图由调用方稳定持有，保留成员结构只在本类内维护。 */
    public UiScrollContainer(UiScrollView scroll, UiInputTarget content) {
        this(scroll, content, () -> { });
    }

    /** 滚动推进后更新实际成员的屏幕投影，再推进子成员。 */
    public UiScrollContainer(UiScrollView scroll, UiInputTarget content, Runnable project) {
        this(scroll, content, project, null);
    }

    private UiScrollContainer(UiScrollView scroll, UiInputTarget content, Runnable project,
                              @Nullable DoubleUnaryOperator wheelPixels) {
        this.scroll = Objects.requireNonNull(scroll, "scroll");
        this.content = Objects.requireNonNull(content, "content");
        this.project = Objects.requireNonNull(project);
        this.wheelPixels = wheelPixels;
        members.add(content);
        members.add(scroll);
    }

    /** 自绘连续滚动内容使用原始滚轮量与当前步长，保留小数像素。 */
    public static UiScrollContainer forPixelWheel(UiScrollView scroll, UiInputTarget content) {
        return new UiScrollContainer(scroll, content, () -> { }, amount -> amount * scroll.step());
    }

    /** 先推进视口，再让实际内容同步屏幕投影。 */
    @Override public void advanceFrame(long nowMillis) {
        scroll.advanceFrame(nowMillis);
        project.run();
        content.advanceFrame(nowMillis);
    }

    /** 在视口内绘制内容，恢复裁剪后绘制滚动条。 */
    public void render(GuiGraphics graphics, UiPointer pointer, Supplier<? extends UiStyleSource> theme,
                       Painter painter) {
        UiPointer clipped = pointer.restrict(scroll.containsContent(pointer.x(), pointer.y()));
        UiRenderLayers.enableScissor(graphics, scroll.viewport());
        try {
            painter.render(clipped);
        } finally {
            UiRenderLayers.disableScissor(graphics);
        }
        scroll.renderScrollbar(graphics, theme);
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
        if (members.mouseScrolled(context, x, y)) return true;
        if (wheelPixels != null) return scroll.scrollByPixels(wheelPixels.applyAsDouble(y));
        return scroll.scrollBy(y);
    }
    /** {@inheritDoc} */
    @Override public boolean keyPressed(int key, int scan, int modifiers) { return members.keyPressed(key, scan, modifiers); }
    /** {@inheritDoc} */
    @Override public boolean keyReleased(int key, int scan, int modifiers) { return members.keyReleased(key, scan, modifiers); }
    /** {@inheritDoc} */
    @Override public boolean charTyped(char value, int modifiers) { return members.charTyped(value, modifiers); }
}
