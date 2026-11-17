package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.render.UiTooltipPresenter;
import com.meteorite.meteosuikit.api.style.UiSurfaceStyle;
import com.meteorite.meteosuikit.api.style.UiTooltipSkin;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

final class TooltipContract extends ContractSupport {
    static void run() {
        RecordingGraphics graphics = new RecordingGraphics();
        MetricFont font = new MetricFont();
        UiPointerHit hit = UiPointerHit.leaf(new UiInputTarget() {}, 10, 20)
                .withTextTooltip(List.of(Component.literal("slot tooltip")), new UiRect(0, 0, 20, 20));
        for (int base : new int[]{0, UiRenderLayers.modalContent(0), UiRenderLayers.modalContent(1)}) {
            graphics.reset();
            graphics.pose().pushPose();
            graphics.pose().translate(13, 17, base);
            graphics.pose().scale(2, 2, 1);
            Matrix4f before = new Matrix4f(graphics.pose().last().pose());
            UiTooltipPresenter.render(graphics, font, hit, UiTooltipSkin.flat(UiSurfaceStyle.MINECRAFT), 10, 20);
            check(!graphics.fillDepths.isEmpty() && !graphics.textDepths.isEmpty(), "tooltip paints background and text");
            check(graphics.fillDepths.stream().allMatch(z -> z == base + 400)
                    && graphics.textDepths.stream().allMatch(z -> z == base + 400), "default tooltip encloses all paints above vanilla slots");
            check(base + 400 < (base == 0 ? 600 : base + 599), "next modal mask encloses previous tooltip");
            check(before.equals(graphics.pose().last().pose()) && graphics.clips == 0, "tooltip restores pose and clip");
            UiPointerHit rich = hit.withPresentation((g, f, x, y) -> {
                check(x == 33 && y == 57, "rich tooltip receives GUI coordinates");
                check(g.pose().last().pose().m00() == 1 && g.pose().last().pose().m30() == 0, "rich tooltip uses GUI pose");
                check(g.pose().last().pose().m32() == base, "rich tooltip retains scope baseline without double layering");
                throw new IllegalStateException("expected painter failure");
            });
            try {
                UiTooltipPresenter.render(graphics, font, rich, UiSurfaceStyle.MINECRAFT, 10, 20);
                throw new AssertionError("rich painter should throw");
            } catch (IllegalStateException expected) {
                check(expected.getMessage().equals("expected painter failure"), "painter failure propagated");
            }
            check(before.equals(graphics.pose().last().pose()), "failed painter restores pose");
            graphics.pose().popPose();
        }
        UiPointer empty = UiPointer.at(10, 20).resolve(new UiInputTarget() {});
        check(empty.hit() == UiPointerHit.EMPTY && empty.hit().owners().isEmpty(), "resolved empty is distinct from unresolved hit");
    }
}
