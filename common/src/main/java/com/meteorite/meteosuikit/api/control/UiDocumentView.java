package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.geometry.UiTransform;
import com.meteorite.meteosuikit.api.input.UiFocusState;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.layout.UiDocument;
import com.meteorite.meteosuikit.api.layout.UiDocumentHit;
import com.meteorite.meteosuikit.api.layout.UiDocumentTarget;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.layout.UiNode;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.text.TextMeasurer;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** 页内文档阅读：持有折行、滚动、动作导航与配对激活，业务只提供逻辑节点。 */
public final class UiDocumentView implements UiStatefulWidget, UiLayoutSource, UiInteractionSession.Target {
    /** 在同一内容坐标和裁剪中绘制业务背景；selected 表示键盘动作提示。 */
    @FunctionalInterface
    public interface Background {
        /** 绘制一个已排版逻辑目标的背景。 */
        void render(GuiGraphics graphics, UiDocumentTarget target, boolean selected);
    }
    private final UiWidgetShell shell = new UiWidgetShell(this, this::endInteraction);
    private final UiScrollView scroll = new UiScrollView();
    private final UiDocument document;
    private final DocumentActions actions;
    private final Supplier<? extends UiStyleSource> theme;
    private final Component name;
    private Background background = (graphics, target, selected) -> {};
    private @Nullable UiInteractionSession session;

    /** 字体只用于度量，主题与名称由调用方提供。 */
    public UiDocumentView(TextMeasurer measurer, Supplier<? extends UiStyleSource> theme, Component name) {
        this.theme = theme;
        this.name = name;
        document = new UiDocument(measurer, new UiTransform(), theme.get().surfaces().panelBorder(), false);
        document.setTextOverflow(UiDocument.TextOverflow.WRAP);
        actions = new DocumentActions(document);
        scroll.setStep(measurer.lineHeight() * 2);
    }
    /** 正文先准备范围，再推进同源滚动几何。 */
    @Override public void advanceFrame(long nowMillis) {
        if (!isVisible() || !isEnabled()) return;
        layoutDocument();
        scroll.advanceFrame(nowMillis);
    }
    /** 退出正文时取消动作与待完成滚动。 */
    @Override public void scopeExited(UiInteractionSession.EndReason reason) { endInteraction(reason); }

    /** 业务背景保持在文档呈现接缝，阅读机制由控件持有。 */
    public void setBackground(Background background) { this.background = background; }
    /** 稳定内容重复提交保留位置；内容身份变化由宿主选择恢复偏移。 */
    public void setContent(List<UiNode> nodes) { document.setContent(nodes); layoutDocument(); }
    /** 按版本构建内容，滚动和绘制不重复调用 builder。 */
    public void setContent(long revision, Supplier<List<UiNode>> builder) { document.setContent(revision, builder); layoutDocument(); }
    /** 字体或语言变化后重新排版，保留合法阅读位置。 */
    public void invalidateLayout() { document.invalidateLayout(); layoutDocument(); }
    /** 当前正文偏移，单位为内容逻辑像素。 */
    public int offset() { return scroll.offset(); }
    /** 内容准备后设置阅读位置，按真实高度钳制。 */
    public void setOffset(int value) { layoutDocument(); scroll.setOffset(value); }
    private void layoutDocument() {
        int width = Math.max(1, bounds().width() - UiScrollView.SCROLLBAR_WIDTH);
        document.setViewport(0, 0, width, Math.max(1, bounds().height()));
        scroll.setContentHeight(document.contentHeight());
        scroll.setScrollbarVisible(scroll.maxOffset() > 0);
        document.setViewport(0, 0, width, Math.max(1, scroll.contentHeight()));
        actions.refresh();
    }
    /** {@inheritDoc} */
    @Override public void setBounds(int x, int y, int width, int height) {
        UiRect previous = bounds();
        shell.setBounds(x, y, width, height);
        if (!previous.equals(bounds())) actions.cancel();
        scroll.setViewport(x, y, width, height);
        layoutDocument();
    }
    /** {@inheritDoc} */
    @Override public UiRect bounds() { return shell.bounds(); }
    /** {@inheritDoc} */
    @Override public boolean isEnabled() { return shell.isEnabled(); }
    /** {@inheritDoc} */
    @Override public boolean isVisible() { return shell.isVisible(); }
    /** {@inheritDoc} */
    @Override public UiDocumentView setEnabled(boolean enabled) { shell.setEnabled(enabled); return this; }
    /** {@inheritDoc} */
    @Override public UiDocumentView setVisible(boolean visible) { shell.setVisible(visible); return this; }
    /** {@inheritDoc} */
    @Override public void setLayoutKey(String key) { shell.setLayoutKey(key); }
    /** {@inheritDoc} */
    @Override public UiFocusState focusState() { return shell.focusState(); }
    /** {@inheritDoc} */
    @Override public Component accessibleName() { var target = actions.selected(); return target == null ? name : target.label(); }
    /** {@inheritDoc} */
    @Override public List<UiLayoutNode> layoutNodes() {
        layoutDocument();
        return List.of(new UiLayoutNode(UiLayoutNode.Kind.FRAME, bounds(), name, 0, bounds().width(), isEnabled(), isVisible(),
                document.screenLayoutNodes().stream().map(this::toScreen).toList()).identified(shell.layoutKey(), this));
    }
    private UiLayoutNode toScreen(UiLayoutNode node) {
        UiRect rect = node.rect();
        UiRect clip = node.visibleRect();
        return new UiLayoutNode(node.kind(), new UiRect(bounds().x() + rect.x(), bounds().y() + rect.y() - offset(), rect.width(), rect.height()),
                node.text(), node.textWidth(), node.textBudget(), node.enabled(), node.visible(),
                node.children().stream().map(this::toScreen).toList(), node.key(),
                new UiRect(bounds().x() + clip.x(), bounds().y() + clip.y() - offset(), clip.width(), clip.height()),
                node.target()).clippedTo(bounds());
    }
    /** {@inheritDoc} */
    @Override public void render(GuiGraphics graphics, Font font, UiPointer incoming) {
        if (!isVisible()) return;
        layoutDocument();
        UiPointer pointer = incoming.forTarget(this).restrict(isEnabled() && !scroll.hitScrollbar(incoming.x(), incoming.y()));
        UiPointer local = pointer.withPosition(scroll.toContentX(pointer.x()), scroll.toContentY(pointer.y()));
        scroll.push(graphics);
        try {
            var selected = actions.selected();
            for (var target : document.targets()) {
                if (target.contentClip() != null) UiRenderLayers.enableScissor(graphics, target.contentClip());
                try { background.render(graphics, target, isFocusVisible() && selected != null && selected.hit() == target.hit()); }
                finally { if (target.contentClip() != null) UiRenderLayers.disableScissor(graphics); }
            }
            document.transform().setOrigin(0, 0);
            document.render(graphics, font, local);
        } finally { scroll.pop(graphics); }
        scroll.renderScrollbar(graphics, theme);
    }
    private @Nullable UiDocumentHit hit(double x, double y) {
        if (!isVisible() || !isEnabled() || !scroll.contains(x, y) || scroll.hitScrollbar(x, y)) return null;
        document.setViewport(0, 0, Math.max(1, bounds().width() - UiScrollView.SCROLLBAR_WIDTH), Math.max(1, scroll.contentHeight()));
        return document.hit(scroll.toContentX(x), scroll.toContentY(y));
    }
    /** {@inheritDoc} */
    @Override public @Nullable UiPointerHit hitTest(double x, double y) {
        if (!isEnabled() || !isVisible() || !bounds().contains(x, y)) return null;
        layoutDocument();
        var bar = scroll.hitTest(x, y);
        if (bar != null) return bar.within(this);
        var contentHit = document.hitTest(scroll.toContentX(x), scroll.toContentY(y));
        return contentHit == null ? UiPointerHit.leaf(this, x, y)
                : contentHit.within(this).withTextTooltip(hit(x, y).tooltip(), bounds());
    }
    /** {@inheritDoc} */
    @Override public boolean mousePressed(UiInputContext context) {
        if (!canFocus() || !bounds().contains(context.pointerX(), context.pointerY())) return false;
        layoutDocument();
        if (scroll.mousePressed(context)) return true;
        if (context.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        actions.press(hit(context.pointerX(), context.pointerY()));
        if (actions.hasPress()) {
            session = context.scope().interaction();
            session.begin(this, UiInteractionSession.Source.POINTER, context.button());
        }
        return true;
    }
    /** {@inheritDoc} */
    @Override public boolean mouseReleased(UiInputContext context) { return canFocus() && context.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && actions.release(hit(context.pointerX(), context.pointerY())); }
    /** 拖动正文取消动作按压，滚动条由自身捕获处理。 */
    @Override public boolean mouseDragged(UiInputContext context, double dx, double dy) {
        boolean pressed = actions.hasPress();
        actions.cancel();
        return pressed;
    }
    /** {@inheritDoc} */
    @Override public void interactionEnded(UiInteractionSession.EndReason reason) { actions.cancel(); session = null; }
    private void endInteraction(UiInteractionSession.EndReason reason) {
        scroll.scopeExited(reason);
        if (session != null) session.end(this, reason);
        actions.cancel();
    }
    /** {@inheritDoc} */
    @Override public boolean mouseScrolled(UiInputContext context, double x, double y) {
        return canFocus() && bounds().contains(context.pointerX(), context.pointerY()) && scroll.scrollBy(y);
    }
    /** {@inheritDoc} */
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!canFocus() || !isFocused()) return false;
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            var scope = focusState().scope();
            if (scope == null || actions.selected() == null) return false;
            var interaction = scope.interaction();
            if (interaction.owns(this) && interaction.matches(UiInteractionSession.Source.KEY, key)) return true;
            interaction.begin(this, UiInteractionSession.Source.KEY, key);
            session = interaction;
            actions.press(actions.selected().hit());
            return true;
        }
        if (actions.navigate(key)) { revealAction(); return true; }
        actions.cancel();
        switch (key) {
            case GLFW.GLFW_KEY_UP -> scroll.setOffset(offset() - scroll.step());
            case GLFW.GLFW_KEY_DOWN -> scroll.setOffset(offset() + scroll.step());
            case GLFW.GLFW_KEY_HOME -> scroll.setOffset(0);
            case GLFW.GLFW_KEY_END -> scroll.setOffset(scroll.maxOffset());
            case GLFW.GLFW_KEY_PAGE_UP -> scroll.setOffset(offset() - bounds().height());
            case GLFW.GLFW_KEY_PAGE_DOWN -> scroll.setOffset(offset() + bounds().height());
            default -> { return false; }
        }
        return true;
    }
    /** 键盘释放复用相同动作身份；重复按下和失效后的迟到释放不会重新激活。 */
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (session == null || !session.owns(this) || !session.matches(UiInteractionSession.Source.KEY, key)) return false;
        var selected = actions.selected();
        if (canFocus() && isFocused()) actions.release(selected == null ? null : selected.hit());
        else actions.cancel();
        return true;
    }
    private void revealAction() { var target = actions.selected(); if (target != null) scroll.ensureVisible(target.contentClip() == null ? target.contentBounds() : target.contentBounds().intersect(target.contentClip())); }
    /** {@inheritDoc} */
    @Override public void focusChanged(boolean focused) { if (focused) revealAction(); }
    /** {@inheritDoc} */
    @Override public boolean activate() { return canFocus() && actions.activate(); }
}
