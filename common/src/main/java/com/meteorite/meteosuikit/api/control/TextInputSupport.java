package com.meteorite.meteosuikit.api.control;

import java.util.function.Supplier;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 原版文本适配共用的内边距与最小尺寸。
 */
final class TextInputSupport {
    static final int PADDING_X = 3;
    static final int PADDING_Y = 2;
    static final int MIN_WIDTH = 24;

    static void drawBackground(GuiGraphics graphics, UiRect bounds, Supplier<? extends UiStyleSource> theme) {
        theme.get().drawInset(graphics, bounds);
        graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1, theme.get().surfaces().inputFill());
    }

    private TextInputSupport() {
    }

}
