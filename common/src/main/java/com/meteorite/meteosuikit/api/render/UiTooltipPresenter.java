package com.meteorite.meteosuikit.api.render;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.style.UiSurfaceStyle;
import com.meteorite.meteosuikit.api.style.UiTooltipSkin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/** 将一次命中的普通或富提示呈现在原版 GUI 空间；调用方先检查作用域门禁并退出内容裁剪。 */
public final class UiTooltipPresenter {
    private UiTooltipPresenter() { }

    /** 使用原版提示外框和指定配色；指针位于当前轴对齐二维 pose 内。 */
    public static void render(GuiGraphics graphics, Font font, UiPointerHit hit,
                              UiSurfaceStyle style, int pointerX, int pointerY) {
        render(graphics, font, hit, UiTooltipSkin.minecraft(style), pointerX, pointerY);
    }

    /**
     * 普通提示由 panel 应用默认提示层；富提示保持作用域基线，供原版 tooltip 或 panel 自行加层。
     * GUI 坐标换算只撤销二维平移和缩放，保留模态深度；异常时恢复 pose 与已提交批次。
     */
    public static void render(GuiGraphics graphics, Font font, UiPointerHit hit,
                              UiTooltipSkin skin, int pointerX, int pointerY) {
        if (hit.presentation() == null && (hit.tooltip() == null || hit.tooltip().tooltip().isEmpty())) return;
        Matrix4f matrix = graphics.pose().last().pose();
        float scaleX = matrix.m00(), scaleY = matrix.m11();
        int guiX = Math.round(scaleX * pointerX + matrix.m30());
        int guiY = Math.round(scaleY * pointerY + matrix.m31());
        graphics.flush();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(-matrix.m30() / scaleX, -matrix.m31() / scaleY, 0);
            graphics.pose().scale(1 / scaleX, 1 / scaleY, 1);
            if (hit.presentation() != null) hit.presentation().render(graphics, font, guiX, guiY);
            else UiFlatText.componentTooltip(graphics, font,
                    new UiRect(0, 0, graphics.guiWidth(), graphics.guiHeight()), hit.tooltip().tooltip(),
                    UiTooltipPanel.DEFAULT_MAX_WIDTH, guiX, guiY, skin);
        } finally {
            try { graphics.flush(); }
            finally { graphics.pose().popPose(); }
        }
    }
}
