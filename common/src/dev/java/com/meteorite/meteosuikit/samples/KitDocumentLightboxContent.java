package com.meteorite.meteosuikit.samples;

import com.meteorite.meteosuikit.api.control.UiDocumentLightboxContent;
import com.meteorite.meteosuikit.api.geometry.UiTransform;
import com.meteorite.meteosuikit.api.layout.UiNode;
import com.meteorite.meteosuikit.api.render.UiIcon;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.text.TextMeasurer;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 样例只组装逻辑内容；配对激活、焦点与变换由公开 reader 持有。 */
final class KitDocumentLightboxContent {
    private final Supplier<? extends UiStyleSource> theme;
    private final UiTransform frameTransform = new UiTransform();
    private final UiDocumentLightboxContent reader;
    private int activations;
    private long themeRevision = -1;
    private UiStyleSource appliedTheme;

    private KitDocumentLightboxContent(Font font, Supplier<? extends UiStyleSource> theme) {
        this.theme = theme;
        reader = new UiDocumentLightboxContent(TextMeasurer.of(font), theme, label("row"));
        reader.setContentSource(this::content);
    }

    static UiDocumentLightboxContent create(Font font, Supplier<? extends UiStyleSource> theme) {
        return new KitDocumentLightboxContent(font, theme).reader;
    }

    private List<UiNode> content() {
        if (appliedTheme != theme.get() || themeRevision != theme.get().revision()) {
            rebuild();
            appliedTheme = theme.get();
            themeRevision = appliedTheme.revision();
        }
        return nodes;
    }

    private List<UiNode> nodes = List.of();

    private void rebuild() {
        UiNode.InlineIcon icon = new UiNode.InlineIcon(new UiIcon.Item(new ItemStack(Items.GOLD_INGOT)),
                List.of(label("icon")), "icon", this::activate);
        nodes = List.of(new UiNode.Row(0, UiNode.NO_PARENT, icon,
                        Component.translatable(KitShowcaseContent.UI + "showcase.canvas.count", activations),
                        theme.get().textColor(true), List.of(), List.of(label("row")), "row", this::activate),
                new UiNode.Gap(8), new UiNode.Frame(new UiNode.FrameSpec(180, 48,
                        theme.get().surfaces().panelBorder(), frameTransform),
                        List.of(new UiNode.Row(label("child"), theme.get().textColor(true)))));
    }

    private void activate() { activations++; rebuild(); }
    private static Component label(String key) { return Component.translatable(KitShowcaseContent.UI + "showcase.canvas." + key); }
}
