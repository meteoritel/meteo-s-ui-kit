package com.meteorite.meteosuikit.api.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.List;
import java.util.Optional;

/** 注入自然文本宽高，供文档和控件布局在测量阶段读取。 */
public interface TextMeasurer {
    /** 测量组件的自然像素宽度。 */
    int width(Component text);
    /** 按给定宽度分行，保留样式；极窄宽度仍须推进文本，空文本返回空行。 */
    List<Component> wrap(Component text, int width);
    /** 当前字体的自然行高。 */
    int lineHeight();

    /** 创建原版 Font 的度量适配器。 */
    static TextMeasurer of(Font font) { return new FontMeasurer(font); }

    /** 原版字体适配器，仅在排版阶段测量。 */
    record FontMeasurer(Font font) implements TextMeasurer {
        @Override public int width(Component text) { return font.width(text); }
        @Override public List<Component> wrap(Component text, int width) {
            return font.getSplitter().splitLines(text, Math.max(1, width), Style.EMPTY).stream().map(line -> {
                var result = Component.empty();
                line.visit((style, value) -> {
                    result.append(Component.literal(value).setStyle(style));
                    return Optional.empty();
                }, Style.EMPTY);
                return (Component) result;
            }).toList();
        }
        @Override public int lineHeight() { return font.lineHeight; }
    }
}
