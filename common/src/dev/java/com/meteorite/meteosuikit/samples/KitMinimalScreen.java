package com.meteorite.meteosuikit.samples;

import com.meteorite.meteosuikit.api.control.UiControl;
import com.meteorite.meteosuikit.api.control.UiThemedScreen;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.style.UiControlRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 普通页面的最小接入；标题与按钮文案由调用者提供。 */
public final class KitMinimalScreen extends UiThemedScreen {
    private final UiControl closeButton = new UiControl();
    private final UiInputScope scope = new UiInputScope(closeButton);
    private final Component closeLabel;

    /** 稳定持有输入与控件，resize 只更新布局。 */
    public KitMinimalScreen(Component title, Component closeLabel) {
        super(title);
        this.closeLabel = closeLabel;
        closeButton.setTheme(theme(), UiControlRole.SECONDARY);
        closeButton.setFocusable(true);
        scope.setEscape(this::onClose);
    }

    @Override protected UiInputScope inputScope() { return scope; }
    @Override protected void init() {
        closeButton.configure(font, closeLabel, null, List.of(), this::onClose);
    }
    @Override protected void layoutUi() {
        closeButton.setBounds(12, 12, 100, 20);
    }
    @Override protected void renderThemedUi(GuiGraphics graphics, int x, int y, float delta) {
        var pointer = scope.pointer(x, y).resolve(closeButton);
        closeButton.render(graphics, font, pointer);
    }
}
