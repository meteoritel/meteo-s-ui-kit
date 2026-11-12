package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.style.UiThemeContext;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

/** 带完整默认外观的屏幕；背景与内容共用一个稳定 Context，业务排版由子类拥有。 */
public abstract class UiThemedScreen extends UiScreen {
    private final Supplier<? extends UiStyleSource> theme;

    /** 创建 Minecraft 默认屏幕；控件绑定 theme() 即可共享完整外观。 */
    protected UiThemedScreen(Component title) { this(title, UiThemeContext.minecraft()); }

    /** 直接复用宿主 Context，避免屏幕背景与控件出现两个主题所有者。 */
    protected UiThemedScreen(Component title, UiThemeContext<? extends UiStyleSource> theme) {
        super(title);
        this.theme = theme::current;
    }

    /** 屏幕背景与子控件共同读取的稳定来源。 */
    protected final Supplier<? extends UiStyleSource> theme() { return theme; }

    /** 原版世界背景后绘制主题窗口；宿主可覆写整段背景画法。 */
    protected void renderThemedBackground(GuiGraphics graphics, int x, int y, float delta) {
        graphics.pose().pushPose();
        float inverse = (float) (1 / viewport().scale());
        graphics.pose().scale(inverse, inverse, 1);
        try { renderBackground(graphics, (int) (x * viewport().scale()), (int) (y * viewport().scale()), delta); }
        finally { graphics.pose().popPose(); }
        theme.get().drawWindow(graphics, new UiRect(0, 0, viewport().logicalWidth(), viewport().logicalHeight()));
    }

    /** 主题发布、布局和作用域准备完成后，在背景上绘制内容。 */
    protected abstract void renderThemedUi(GuiGraphics graphics, int x, int y, float delta);

    @Override protected final void renderUi(GuiGraphics graphics, int x, int y, float delta) {
        renderThemedBackground(graphics, x, y, delta);
        renderThemedUi(graphics, x, y, delta);
    }
}
