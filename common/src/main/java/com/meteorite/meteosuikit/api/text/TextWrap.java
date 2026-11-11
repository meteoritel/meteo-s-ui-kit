package com.meteorite.meteosuikit.api.text;

import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** 有行数上限的格式化文本排版，逻辑裁剪完成后统一执行视觉排序。 */
public final class TextWrap {
    private static final String ELLIPSIS = "...";

    private TextWrap() { }

    /**
     * 折行并返回可绘制的视觉序列，超出行数时在最后可见行追加 ASCII 省略号。
     * 宽度或行数非正时返回空列表；后缀放不下时只裁剪内容，保留完整码点和原有 Style。
     */
    public static List<FormattedCharSequence> fit(Font font, FormattedText text, int width, int maxLines) {
        if (width <= 0 || maxLines <= 0) return List.of();
        StringSplitter splitter = font.getSplitter();
        List<FormattedText> lines = splitter.splitLines(text, width, Style.EMPTY);
        int count = Math.min(lines.size(), maxLines);
        List<FormattedCharSequence> result = new ArrayList<>(count);
        int suffixWidth = font.width(ELLIPSIS);
        for (int index = 0; index < count; index++) {
            FormattedText line = lines.get(index);
            if (index == count - 1 && lines.size() > count && suffixWidth <= width) {
                line = FormattedText.composite(splitter.headByWidth(line, width - suffixWidth, Style.EMPTY),
                        FormattedText.of(ELLIPSIS));
            } else {
                line = splitter.headByWidth(line, width, Style.EMPTY);
            }
            result.add(Language.getInstance().getVisualOrder(line));
        }
        return List.copyOf(result);
    }
}
