package com.meteorite.meteosuikit.api.geometry;

/** 根屏幕的像素对齐视口；实际像素不足最小画布时保留连续缩小。 */
public record UiViewport(double scale, int logicalWidth, int logicalHeight, UiRect bounds) {
    /**
     * 以实际 framebuffer 选择整数物理倍率，原版 GUI 倍率决定放大上限。
     *
     * @param framebufferWidth 实际 framebuffer 宽度
     * @param framebufferHeight 实际 framebuffer 高度
     * @param guiScale 原版 GUI 倍率，须为有限正数
     * @param minimumWidth 最小逻辑画布宽度，须为正
     * @param minimumHeight 最小逻辑画布高度，须为正
     * @return 绘制、输入及布局快照共用的视口；零尺寸保持合法逻辑画布
     */
    public static UiViewport fit(int framebufferWidth, int framebufferHeight, double guiScale,
                                 int minimumWidth, int minimumHeight) {
        if (minimumWidth <= 0 || minimumHeight <= 0) throw new IllegalArgumentException("Invalid minimum canvas");
        if (!Double.isFinite(guiScale) || guiScale <= 0) throw new IllegalArgumentException("Invalid GUI scale");
        int width = (int) Math.ceil(Math.max(0, framebufferWidth) / guiScale);
        int height = (int) Math.ceil(Math.max(0, framebufferHeight) / guiScale);
        UiRect bounds = new UiRect(0, 0, Math.max(0, width), Math.max(0, height));
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return new UiViewport(1, Math.max(minimumWidth, width), Math.max(minimumHeight, height), bounds);
        }
        double fittingPixels = Math.min(guiScale, Math.min(
                (double) framebufferWidth / minimumWidth, (double) framebufferHeight / minimumHeight));
        double integerPixels = Math.floor(fittingPixels);
        double pixels = integerPixels >= 1 ? integerPixels : fittingPixels;
        if (pixels == guiScale) return new UiViewport(1, width, height, bounds);
        return new UiViewport(pixels / guiScale,
                Math.max(minimumWidth, (int) Math.floor(framebufferWidth / pixels)),
                Math.max(minimumHeight, (int) Math.floor(framebufferHeight / pixels)), bounds);
    }

    /** GUI 位置及拖动距离共用逆变换。 */
    public double toLogical(double value) { return value / scale; }

    /** 向内取整避免自动化选点越过实际可见边缘。 */
    public UiRect project(UiRect rect) {
        int x = (int) Math.ceil(rect.x() * scale);
        int y = (int) Math.ceil(rect.y() * scale);
        return new UiRect(x, y, Math.max(0, (int) Math.floor(rect.right() * scale) - x),
                Math.max(0, (int) Math.floor(rect.bottom() * scale) - y));
    }
}
