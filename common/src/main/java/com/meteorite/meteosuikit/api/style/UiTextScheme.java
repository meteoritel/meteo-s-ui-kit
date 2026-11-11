package com.meteorite.meteosuikit.api.style;

/** 自绘文本的共同语义色；结构色与业务专属强调由各自配方维护。 */
public record UiTextScheme(int title, int body, int label, int hint,
                           int positive, int negative, int accent) {
    /** 浅色羊皮纸底上的无阴影文字预设。 */
    public static final UiTextScheme PARCHMENT = new UiTextScheme(
            0xFF3A2818, 0xFF5A422C, 0xFF6B573F, 0xFF6A5A45,
            0xFF37632F, 0xFFA33F1E, 0xFF7A5700);

    /** 暗色页面底上的文字预设。 */
    public static final UiTextScheme DARK = new UiTextScheme(
            0xFFFFFFFF, 0xFFE0E0E0, 0xFFB8B8B8, 0xFF999999,
            0xFF78D66A, 0xFFFF5555, 0xFFFFAA00);
}
