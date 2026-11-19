package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiFocusTarget;
import com.meteorite.meteosuikit.api.input.UiInputContainer;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.layout.UiLinearLayout;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/** 在同一行布局、绘制并路由真实控件。由宿主的布局阶段调用 setBounds。 */
public final class UiWidgetRow implements UiWidget, UiInputContainer {
    /** 剩余空间留在尾部或分配到两端。 */
    public enum Alignment { START, CENTER }

    private final UiChildren<UiWidget> children = new UiChildren<>();
    private final Map<UiWidget, UiLinearLayout.Child> rules = new IdentityHashMap<>();
    private final UiLinearLayout layout;
    private final Alignment alignment;
    private UiRect bounds = new UiRect(0, 0, 0, 0);
    private List<UiLinearLayout.Child> visibleRules = List.of();

    /** 宽度由实际控件的尺寸规则求得，高度默认填满行。 */
    public UiWidgetRow(int spacing, Alignment alignment) {
        this.layout = new UiLinearLayout(UiLinearLayout.Axis.HORIZONTAL, spacing, 0);
        this.alignment = alignment;
    }

    /** 按绘制顺序登记控件。更新已有控件的尺寸规则时保留其位置和身份。 */
    public void add(UiWidget widget, UiLinearLayout.Child rule) {
        if (rules.put(widget, rule) == null) children.add(widget);
    }

    /** 供输入容器遍历实际成员；通过 add 登记尺寸规则及成员。 */
    @Override public UiChildren<UiWidget> inputChildren() { return children; }
    @Override public UiRect bounds() { return bounds; }

    /** 隐藏控件释放空间；禁用控件仍保留原布局位置。 */
    @Override public void setBounds(int x, int y, int width, int height) {
        bounds = new UiRect(x, y, Math.max(0, width), Math.max(0, height));
        List<UiLinearLayout.Child> nextRules = new ArrayList<>();
        for (UiWidget widget : children) {
            if (widget.isVisible()) nextRules.add(rules.get(widget));
        }
        if (!visibleRules.equals(nextRules)) {
            visibleRules = List.copyOf(nextRules);
            layout.setChildren(visibleRules);
        }
        layout.setBounds(x, y, bounds.width(), bounds.height());
        int shift = alignment == Alignment.CENTER ? Math.max(0, bounds.width() - layout.usedMain()) / 2 : 0;
        int index = 0;
        for (UiWidget widget : children) {
            if (!widget.isVisible()) continue;
            UiRect rect = layout.bounds(index++);
            widget.setBounds(rect.x() + shift, rect.y(), rect.width(), rect.height());
        }
    }

    @Override public @Nullable UiPointerHit hitTest(double x, double y) {
        return UiInputContainer.super.hitTest(x, y);
    }

    @Override public long focusRevision() { return UiInputContainer.super.focusRevision(); }

    @Override public void addFocusTargets(Consumer<UiFocusTarget> out) {
        UiInputContainer.super.addFocusTargets(out);
    }

    @Override public void render(GuiGraphics graphics, Font font, UiPointer pointer) {
        UiPointer resolved = pointer.resolve(this);
        for (UiWidget widget : children) {
            if (widget.isVisible()) widget.render(graphics, font, resolved);
        }
    }
}
