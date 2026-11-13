package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiFocusTarget;
import com.meteorite.meteosuikit.api.input.UiInputContainer;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.layout.UiFormLayout;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.text.TextMeasurer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 表单的滚动正文：稳定持有真实字段，统一测量标签、字段与尾随动作，并按屏幕坐标投影。
 * 不创建输入作用域；宿主将 reveal 接到已有焦点 listener，业务校验和固定底栏留在宿主。
 */
public final class UiFormBody implements UiWidget, UiInputContainer {
    private static final int CONTENT_LEFT = 2;
    private static final int CONTENT_TOP = 3;
    private static final int CONTENT_BOTTOM = 8;
    private static final int HORIZONTAL_PADDING = 8;
    private static final int ROW_GAP = 11;
    private record Row(Component label, UiWidget widget, UiFormLayout.Control measure, List<UiControl> actions) {}
    private final List<Row> rows = new ArrayList<>();
    private List<Row> visibleRows = List.of();
    private final UiChildren<UiInputTarget> fields = new UiChildren<>();
    private final UiChildren<UiInputTarget> children = new UiChildren<>();
    private final UiScrollView scroll = new UiScrollView();
    private final UiScrollContainer container = new UiScrollContainer(scroll, fields);
    private final Supplier<? extends UiStyleSource> theme;
    private final TextMeasurer text;
    private final UiFocusFollow focusFollow;
    private UiFormLayout.Layout layout = new UiFormLayout.Layout(List.of(), 0);
    private boolean dirty = true;
    private int measuredWidth = -1;
    private UiStyleSource measuredTheme;
    private long themeRevision = -1;
    private Language language;
    private @Nullable UiInputScope pointerScope;
    private @Nullable UiPointerHit pointerHit;
    private @Nullable UiInteractionSession.Target pointerTarget;

    /** 使用调用方的稳定主题和字体度量，固定预留滚动条带宽。 */
    public UiFormBody(Supplier<? extends UiStyleSource> theme, TextMeasurer text) {
        this.theme = Objects.requireNonNull(theme);
        this.text = Objects.requireNonNull(text);
        scroll.setPolicy(new UiScrollView.Policy(1, 8, 0, false, true));
        children.add(container);
        focusFollow = new UiFocusFollow(scroll, target -> ownerOf(target) != null,
                this::focusBounds, this::project);
    }

    /** 添加固定高度字段；空标签用于无标签正文块。 */
    public void add(Component label, UiWidget widget, int height) {
        add(label, widget, new UiFormLayout.Control() {
            @Override public int preferredWidth(int availableWidth) { return availableWidth; }
            @Override public int measureHeight(int width) { return Math.max(0, height); }
        }, List.of());
    }

    /** 添加按宽度度量的正文块或字段，尾随动作与字段共享同一次布局。 */
    public void add(Component label, UiWidget widget, UiFormLayout.Control measure, List<UiControl> actions) {
        Objects.requireNonNull(widget);
        if (rows.stream().anyMatch(row -> row.widget() == widget))
            throw new IllegalArgumentException("Form widget already registered");
        rows.add(new Row(label.copy(), widget, Objects.requireNonNull(measure), List.copyOf(actions)));
        invalidateLayout();
    }

    /** 移除字段和行尾动作，取消属于被移除子树的交互并清理本地状态。 */
    public void remove(UiWidget widget) {
        Row row = rows.stream().filter(value -> value.widget() == widget).findFirst().orElse(null);
        if (row == null) return;
        detach(row.widget());
        row.actions().forEach(this::detach);
        fields.remove(row.widget());
        fields.removeAll(row.actions());
        rows.remove(row);
        invalidateLayout();
    }

    private void detach(UiWidget widget) {
        if (pointerScope != null && pointerHit != null && pointerHit.owns(widget) && pointerTarget != null)
            pointerScope.interaction().end(pointerTarget, UiInteractionSession.EndReason.UNMOUNTED);
        widget.addFocusTargets(target -> detachFocus(target));
        widget.scopeExited(UiInteractionSession.EndReason.UNMOUNTED);
    }

    private void detachFocus(UiFocusTarget target) {
        UiInputScope scope = target.focusState().scope();
        if (scope != null) {
            UiInteractionSession.Target active = scope.interaction().target();
            if (active == target || active instanceof UiFocusTarget focus && focus.focusState() == target.focusState())
                scope.interaction().end(UiInteractionSession.EndReason.UNMOUNTED);
            if (target.focusState().containsFocus()) scope.focus().clearFocus();
        }
        for (UiFocusTarget child : target.focusTargets()) if (child != target) detachFocus(child);
    }

    /** 字段内容、自然高度或字体资源变化后请求重新测量，滚动本身只更新投影。 */
    public void invalidateLayout() { dirty = true; }

    /** 实际输入成员稳定保留；只读查询和输入前同步可见行。 */
    @Override public UiChildren<UiInputTarget> inputChildren() { ensureLayout(); return children; }
    /** 当前正文视口。 */
    @Override public UiRect bounds() { return scroll.viewport(); }
    /** 改变位置或高度只重投影；可用宽度改变时重测。 */
    @Override public void setBounds(int x, int y, int width, int height) {
        scroll.setViewport(x, y, width, height);
        ensureLayout();
        project();
    }
    /** 当前正文像素偏移。 */
    public int offset() { ensureLayout(); return scroll.offset(); }
    /** 恢复偏移并立即同步真实字段位置。 */
    public void setOffset(int offset) { ensureLayout(); scroll.setOffset(offset); project(); }
    /** 让目标所属行可见，支持复合字段内部目标；其他子树的焦点保持原样。 */
    public void reveal(@Nullable UiFocusTarget target) { ensureLayout(); focusFollow.reveal(target); }

    private @Nullable Row ownerOf(UiFocusTarget target) {
        for (Row row : visibleRows) {
            if (containsFocusTarget(row.widget(), target)) return row;
            for (UiControl action : row.actions()) if (action == target) return row;
        }
        return null;
    }
    private static boolean containsFocusTarget(UiInputTarget member, UiFocusTarget requested) {
        boolean[] found = {member == requested};
        member.addFocusTargets(target -> found[0] |= focusContains(target, requested));
        return found[0];
    }
    private static boolean focusContains(UiFocusTarget target, UiFocusTarget requested) {
        if (target == requested || target.focusState() == requested.focusState()) return true;
        for (UiFocusTarget child : target.focusTargets()) if (child != target && focusContains(child, requested)) return true;
        return target.focusState().containsFocus() && requested.isFocused();
    }
    private UiRect focusBounds(UiFocusTarget target) {
        int index = visibleRows.indexOf(ownerOf(target));
        UiFormLayout.RowGeometry geometry = layout.rows().get(index);
        return new UiRect(0, CONTENT_TOP + geometry.y(), scroll.contentWidth(), geometry.height());
    }
    private void ensureLayout() {
        List<Row> visible = rows.stream().filter(row -> row.widget().isVisible()).toList();
        int width = Math.max(0, scroll.contentWidth() - HORIZONTAL_PADDING);
        UiStyleSource currentTheme = theme.get();
        if (!dirty && visible.equals(visibleRows) && measuredWidth == width && measuredTheme == currentTheme
                && themeRevision == currentTheme.revision() && language == Language.getInstance()) return;
        List<UiInputTarget> actual = new ArrayList<>();
        for (Row row : visible) { actual.add(row.widget()); actual.addAll(row.actions()); }
        if (!fields.equals(actual)) {
            for (UiInputTarget old : List.copyOf(fields)) if (!actual.contains(old)) detach((UiWidget) old);
            fields.clear(); fields.addAll(actual);
        }
        visibleRows = visible;
        List<UiFormLayout.Row> inputs = visibleRows.stream().map(row -> new UiFormLayout.Row(
                row.label(), !row.label().getString().isEmpty(), false, true, -1, row.measure(),
                row.actions().stream().map(UiControl::accessibleName).toList())).toList();
        UiFormLayout.Metrics metrics = new UiFormLayout.Metrics(0, 0, text.lineHeight() + 4,
                0, 4, 40, 12, 60, 20, ROW_GAP);
        layout = UiFormLayout.layout(inputs, width, 0, metrics, text);
        scroll.setContentHeight(visibleRows.isEmpty() ? 0 : CONTENT_TOP + layout.height() + CONTENT_BOTTOM);
        scroll.setScrollbarVisible(scroll.maxOffset() > 0);
        measuredWidth = width;
        measuredTheme = currentTheme;
        themeRevision = currentTheme.revision();
        language = Language.getInstance();
        dirty = false;
        project();
    }
    private void project() {
        for (int index = 0; index < visibleRows.size(); index++) {
            Row row = visibleRows.get(index);
            UiFormLayout.RowGeometry geometry = layout.rows().get(index);
            UiRect control = geometry.control();
            int x = bounds().x() + CONTENT_LEFT + control.x();
            int y = scroll.toScreenY(CONTENT_TOP + geometry.y() + control.y());
            row.widget().setBounds(x, y, control.width(), control.height());
            for (int action = 0; action < row.actions().size(); action++) {
                UiRect button = geometry.buttons().get(action);
                row.actions().get(action).setBounds(x + button.x(), y + button.y(), button.width(), button.height());
            }
        }
    }

    /** 标签、字段和动作共用裁剪；tooltip 由退出裁剪后的根命中绘制。 */
    @Override public void render(GuiGraphics graphics, Font font, UiPointer pointer) {
        ensureLayout(); project();
        UiRenderLayers.enableScissor(graphics, bounds());
        try {
            for (int index = 0; index < visibleRows.size(); index++) {
                Row row = visibleRows.get(index);
                UiFormLayout.RowGeometry geometry = layout.rows().get(index);
                if (geometry.labelShown()) graphics.drawString(font, row.label(), bounds().x() + CONTENT_LEFT,
                        scroll.toScreenY(CONTENT_TOP + geometry.y()), theme.get().surfaces().text(), false);
                UiPointer clipped = pointer.restrict(bounds().contains(pointer.x(), pointer.y()));
                row.widget().render(graphics, font, clipped);
                for (UiControl action : row.actions()) action.render(graphics, font, clipped);
            }
        } finally {
            UiRenderLayers.disableScissor(graphics);
        }
        scroll.renderScrollbar(graphics, theme);
    }
    /** 快照使用真实字段几何并叠加祖先视口。 */
    @Override public List<UiLayoutNode> layoutNodes() { ensureLayout(); project(); return children.layoutNodes(); }
    /** 命中始终读取最新偏移的屏幕投影。 */
    @Override public @Nullable UiPointerHit hitTest(double x, double y) {
        ensureLayout(); project();
        UiPointerHit hit = container.hitTest(x, y);
        return hit == null ? null : hit.within(this);
    }
    /** 记录实际捕获归属，字段移除时只取消它自己的会话。 */
    @Override public boolean mousePressed(UiInputContext context) {
        boolean consumed = UiInputContainer.super.mousePressed(context);
        pointerScope = context.scope(); pointerHit = context.hit();
        pointerTarget = pointerScope.interaction().target();
        project();
        return consumed;
    }
    /** 子字段优先消费滚轮；外层滚动后立即投影，捕获拖动在下次查询前同步。 */
    @Override public boolean mouseScrolled(UiInputContext context, double x, double y) {
        boolean consumed = UiInputContainer.super.mouseScrolled(context, x, y);
        project();
        return consumed;
    }
    /** 正文不设焦点，导航完全来自真实字段。 */
    @Override public void addFocusTargets(Consumer<UiFocusTarget> out) { ensureLayout(); children.addFocusTargets(out); }
    /** UiWidget 与容器的默认版本入口统一到真实成员树。 */
    @Override public long focusRevision() { ensureLayout(); return children.focusRevision(); }
}
