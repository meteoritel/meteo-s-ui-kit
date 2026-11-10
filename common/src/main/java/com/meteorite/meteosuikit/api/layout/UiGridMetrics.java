package com.meteorite.meteosuikit.api.layout;

import com.meteorite.meteosuikit.api.geometry.UiRect;

/**
 * 固定网格的不可变内容坐标度量，绘制和命中共用左闭右开的格子几何。
 * <p>列数选择、分页、滚动和选择状态由宿主持有；本类查询均为常数时间。
 * 坐标与高度超出 int 表达范围时抛出 {@link ArithmeticException}。
 *
 * @param columns 列数，必须为正
 * @param cellWidth 格子宽度，必须为正
 * @param cellHeight 格子高度，必须为正
 * @param horizontalGap 列间距，必须非负
 * @param verticalGap 行间距，必须非负
 */
public record UiGridMetrics(int columns, int cellWidth, int cellHeight, int horizontalGap, int verticalGap) {
    /** 非法配置在创建时抛出 {@link IllegalArgumentException}。 */
    public UiGridMetrics {
        if (columns <= 0 || cellWidth <= 0 || cellHeight <= 0 || horizontalGap < 0 || verticalGap < 0) {
            throw new IllegalArgumentException("Invalid UI grid metrics");
        }
    }

    /** 返回相对内容原点的格子矩形；索引必须非负，条目范围由宿主确定。 */
    public UiRect cellBounds(int index) {
        if (index < 0) throw new IllegalArgumentException("Negative cell index");
        int x = Math.toIntExact((index % columns) * ((long) cellWidth + horizontalGap));
        int y = Math.toIntExact((index / columns) * ((long) cellHeight + verticalGap));
        return new UiRect(x, y, cellWidth, cellHeight);
    }

    /**
     * 返回内容坐标处的条目索引；间隙、末行空格、负值、非有限坐标及范围外返回 -1。
     * 条目数必须非负。
     */
    public int indexAt(double contentX, double contentY, int itemCount) {
        int rows = rowCount(itemCount);
        if (!Double.isFinite(contentX) || !Double.isFinite(contentY) || contentX < 0 || contentY < 0) return -1;
        long columnPitch = (long) cellWidth + horizontalGap;
        long rowPitch = (long) cellHeight + verticalGap;
        double column = Math.floor(contentX / columnPitch);
        double row = Math.floor(contentY / rowPitch);
        if (column >= columns || row >= rows || contentX % columnPitch >= cellWidth
                || contentY % rowPitch >= cellHeight) return -1;
        long index = (long) row * columns + (int) column;
        return index < itemCount ? (int) index : -1;
    }

    /** 条目数必须非负；空内容没有行，末行不满仍占一行。 */
    public int rowCount(int itemCount) {
        if (itemCount < 0) throw new IllegalArgumentException("Negative item count");
        return itemCount == 0 ? 0 : 1 + (itemCount - 1) / columns;
    }

    /** 空内容高度为零，最后一行之后不计行间距；条目数必须非负。 */
    public int contentHeight(int itemCount) {
        int rows = rowCount(itemCount);
        return rows == 0 ? 0 : Math.toIntExact(rows * ((long) cellHeight + verticalGap) - verticalGap);
    }
}
