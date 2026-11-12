package com.meteorite.meteosuikit.samples;

import com.meteorite.meteosuikit.api.control.UiButtonWidget;
import com.meteorite.meteosuikit.api.control.UiControl;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiFocusState;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** 包装现有 control 的最小示例：几何、焦点、资格与持续交互都归核心持有。 */
final class KitControlExample implements UiButtonWidget {
    private final UiControl control;

    KitControlExample(UiControl control) { this.control = control; }

    @Override public UiRect bounds() { return control.bounds(); }
    @Override public void setBounds(int x, int y, int width, int height) { control.setBounds(x, y, width, height); }
    @Override public boolean isEnabled() { return control.isEnabled(); }
    @Override public boolean isVisible() { return control.isVisible(); }
    @Override public KitControlExample setEnabled(boolean enabled) { control.setEnabled(enabled); return this; }
    @Override public KitControlExample setVisible(boolean visible) { control.setVisible(visible); return this; }
    @Override public void setLayoutKey(String key) { control.setLayoutKey(key); }
    @Override public UiFocusState focusState() { return control.focusState(); }
    @Override public boolean canFocus() { return control.canFocus(); }
    @Override public long focusRevision() { return control.focusRevision(); }
    @Override public boolean activate() { return control.activate(); }
    @Override public Component accessibleName() { return control.accessibleName(); }
    @Override public Component label() { return control.label(); }
    @Override public int preferredWidth(int padding) { return control.preferredWidth(padding); }
    @Override public List<UiLayoutNode> layoutNodes() { return control.layoutNodes(); }
    @Override public @Nullable UiPointerHit hitTest(double x, double y) {
        UiPointerHit hit = control.hitTest(x, y);
        return hit == null ? null : hit.within(this);
    }
    @Override public boolean mousePressed(UiInputContext context) { return control.mousePressed(context); }
    @Override public void render(GuiGraphics graphics, Font font, UiPointer pointer) { control.render(graphics, font, pointer.forTarget(this)); }
}
