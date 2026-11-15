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
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
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

/** 灯箱文档阅读：独立变换与视口，动作语义与页内阅读一致。 */
public final class UiDocumentLightboxContent implements UiLightbox.Content, UiStatefulTarget, UiLayoutSource {
    private final UiWidgetShell shell = new UiWidgetShell(this, this::endInteraction);
    private final UiDocument document;
    private final DocumentActions actions;
    private final Component name;
    private UiDocumentView.Background background = (graphics, target, selected) -> {};
    private @Nullable Supplier<List<UiNode>> contentSource;
    private boolean dragging;
    private boolean panLocked;
    private boolean fitting = true;
    private @Nullable UiInteractionSession session;

    /** 文档与页内阅读相互独立，名称及外观由调用者提供。 */
    public UiDocumentLightboxContent(TextMeasurer measurer, Supplier<? extends UiStyleSource> theme, Component name) {
        this.name = name;
        document = new UiDocument(measurer, new UiTransform(), theme.get().surfaces().panelBorder(), false);
        document.setTextOverflow(UiDocument.TextOverflow.WRAP);
        actions = new DocumentActions(document);
    }
    /** 接收逻辑内容，保留手动变换并钳制边界。 */
    public void setContent(List<UiNode> nodes) {
        long previous = document.layoutRevision();
        document.setContent(nodes);
        if (previous == document.layoutRevision()) return;
        actions.refresh();
        if (fitting) fit(); else clampPan();
    }
    /** 接入随业务变化的逻辑内容；相同内容复用排版和变换。 */
    public void setContentSource(Supplier<List<UiNode>> source) { contentSource = source; setContent(source.get()); }
    /** 在同一文档坐标与裁剪中装配业务背景。 */
    public void setBackground(UiDocumentView.Background background) { this.background = background; }
    /** {@inheritDoc} */
    @Override public int contentWidth() { return document.contentWidth(); }
    /** {@inheritDoc} */
    @Override public int contentHeight() { return document.contentHeight(); }
    /** {@inheritDoc} */
    @Override public void setViewport(UiRect viewport) {
        if (bounds().equals(viewport)) return;
        actions.cancel();
        shell.setBounds(viewport.x(), viewport.y(), viewport.width(), viewport.height());
        document.setViewport(viewport.x(), viewport.y(), viewport.width(), viewport.height());
        actions.refresh();
        if (fitting) fit(); else clampPan();
    }
    /** {@inheritDoc} */
    @Override public void setBounds(int x, int y, int width, int height) { setViewport(new UiRect(x, y, Math.max(0, width), Math.max(0, height))); }
    /** {@inheritDoc} */
    @Override public UiRect bounds() { return shell.bounds(); }
    /** {@inheritDoc} */
    @Override public boolean isEnabled() { return shell.isEnabled(); }
    /** {@inheritDoc} */
    @Override public boolean isVisible() { return shell.isVisible(); }
    /** {@inheritDoc} */
    @Override public UiDocumentLightboxContent setEnabled(boolean value) { shell.setEnabled(value); return this; }
    /** {@inheritDoc} */
    @Override public UiDocumentLightboxContent setVisible(boolean value) { shell.setVisible(value); return this; }
    /** {@inheritDoc} */
    @Override public void setLayoutKey(String key) { shell.setLayoutKey(key); }
    /** {@inheritDoc} */
    @Override public List<UiLayoutNode> layoutNodes() {
        return List.of(new UiLayoutNode(UiLayoutNode.Kind.FRAME, bounds(), name, 0, bounds().width(),
                isEnabled(), isVisible(), document.screenLayoutNodes()).identified(shell.layoutKey(), this).clippedTo(bounds()));
    }
    /** {@inheritDoc} */
    @Override public void render(GuiGraphics graphics, Font font, UiPointer pointer) {
        if (!isVisible()) return;
        if (contentSource != null) setContent(contentSource.get());
        actions.refresh();
        UiRenderLayers.enableScissor(graphics, bounds());
        document.transform().push(graphics);
        try {
            var selected = actions.selected();
            for (var target : document.targets()) {
                if (target.contentClip() != null) UiRenderLayers.enableScissor(graphics, target.contentClip());
                try { background.render(graphics, target, isFocusVisible() && selected != null && selected.hit() == target.hit()); }
                finally { if (target.contentClip() != null) UiRenderLayers.disableScissor(graphics); }
            }
        } finally {
            graphics.pose().popPose();
            UiRenderLayers.disableScissor(graphics);
        }
        document.render(graphics, font, pointer.forTarget(this).restrict(isEnabled()));
    }

    @Override public boolean zoom(double amount, double anchorX, double anchorY) {
        if (!canFocus() || amount == 0) return false;
        fitting = false;
        actions.cancel();
        int previous = document.transform().zoomIndex();
        document.transform().zoomBy((int) Math.signum(amount), anchorX, anchorY);
        clampPan();
        return previous != document.transform().zoomIndex();
    }

    @Override public boolean zoomBy(int direction) {
        if (!canFocus() || direction == 0) return false;
        fitting = false;
        actions.cancel();
        double centerX = bounds().x() + bounds().width() / 2.0;
        double centerY = bounds().y() + bounds().height() / 2.0;
        int previous = document.transform().zoomIndex();
        document.transform().zoomBy(direction, centerX, centerY);
        clampPan();
        return previous != document.transform().zoomIndex();
    }

    @Override public void fit() {
        fitting = true;
        actions.cancel();
        document.transform().fit(0, 0, contentWidth(), contentHeight(), 0);
        if (document.transform().scale() > 1) document.transform().reset();
        document.transform().restore(document.transform().zoomIndex(), 0, 0);
        clampPan();
    }

    @Override public boolean panBy(double screenDx, double screenDy) {
        if (!canFocus() || !dragging || panLocked) return false;
        fitting = false;
        document.transform().panBy(screenDx, screenDy);
        clampPan();
        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean mousePressed(UiInputContext context) {
        if (!canFocus() || context.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !bounds().contains(context.pointerX(), context.pointerY())) return false;
        actions.press(document.hit(context.pointerX(), context.pointerY()));
        dragging = !actions.hasPress();
        session = context.scope().interaction();
        session.begin(this, UiInteractionSession.Source.POINTER, context.button());
        return true;
    }
    /** {@inheritDoc} */
    @Override public boolean mouseDragged(UiInputContext context, double dx, double dy) {
        if (actions.hasPress()) actions.cancel();
        return panLocked || panBy(dx, dy);
    }
    @Override public void setPanLocked(boolean panLocked) { this.panLocked = panLocked; }
    /** {@inheritDoc} */
    @Override public boolean mouseReleased(UiInputContext context) {
        if (!canFocus() || context.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        dragging = false;
        return actions.release(document.hit(context.pointerX(), context.pointerY()));
    }
    /** {@inheritDoc} */
    @Override public void interactionEnded(UiInteractionSession.EndReason reason) { dragging = false; actions.cancel(); session = null; }
    private void endInteraction(UiInteractionSession.EndReason reason) {
        if (session != null) session.end(this, reason);
        actions.cancel(); dragging = false;
    }
    @Override public int zoomPercent() { return document.transform().scalePercent(); }
    /** {@inheritDoc} */
    @Override public @Nullable UiPointerHit hitTest(double x, double y) {
        if (!canFocus() || !bounds().contains(x, y)) return null;
        var hit = document.hitTest(x, y);
        return hit == null ? UiPointerHit.leaf(this, x, y)
                : hit.within(this).withTextTooltip(document.hit(x, y).tooltip(), bounds());
    }
    /** {@inheritDoc} */
    @Override public UiFocusState focusState() { return shell.focusState(); }
    /** {@inheritDoc} */
    @Override public Component accessibleName() { var target = actions.selected(); return target == null ? name : target.label(); }
    /** {@inheritDoc} */
    @Override public boolean activate() { return canFocus() && actions.activate(); }
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
        if (actions.navigate(key)) {
            var target = actions.selected();
            if (target != null) document.transform().restore(document.transform().zoomIndex(), document.transform().panX(), -target.contentBounds().y());
        } else {
            UiTransform transform = document.transform();
            switch (key) {
                case GLFW.GLFW_KEY_UP -> transform.panBy(0, bounds().height() / 8.0);
                case GLFW.GLFW_KEY_DOWN -> transform.panBy(0, -bounds().height() / 8.0);
                case GLFW.GLFW_KEY_HOME -> transform.restore(transform.zoomIndex(), transform.panX(), 0);
                case GLFW.GLFW_KEY_END -> transform.restore(transform.zoomIndex(), transform.panX(), -contentHeight());
                case GLFW.GLFW_KEY_PAGE_UP -> transform.panBy(0, bounds().height());
                case GLFW.GLFW_KEY_PAGE_DOWN -> transform.panBy(0, -bounds().height());
                default -> { return false; }
            }
        }
        actions.cancel();
        fitting = false;
        clampPan();
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
    /** {@inheritDoc} */
    @Override public void invalidateResources() { document.invalidateLayout(); actions.refresh(); clampPan(); }

    /** 文档始终可由逻辑内容绘制。 */
    @Override public @Nullable Component placeholder() { return null; }

    private void clampPan() {
        UiTransform transform = document.transform();
        UiRect visible = bounds();
        double vw = Math.max(1, visible.width());
        double vh = Math.max(1, visible.height());
        double scale = transform.scale();
        double panX = clampAxis(transform.panX(), document.contentWidth(), vw, scale);
        double panY = clampAxis(transform.panY(), document.contentHeight(), vh, scale);
        transform.restore(transform.zoomIndex(), panX, panY);
    }

    // 单轴：可见内容范围 X = [-pan, vw/s - pan]，内容占 [0, cw]；故 cw*s > vw 时 pan ∈ [vw/s - cw, 0]。
    private static double clampAxis(double pan, double content, double viewportSize, double scale) {
        double visible = viewportSize / scale;
        if (content * scale <= viewportSize) return 0;
        return Math.clamp(pan, visible - content, 0.0);
    }
}
