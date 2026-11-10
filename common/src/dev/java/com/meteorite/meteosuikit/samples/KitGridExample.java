package com.meteorite.meteosuikit.samples;

import com.meteorite.meteosuikit.api.control.UiWidget;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.layout.UiGridMetrics;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** 带间隙且末行不满的本地网格，绘制、布局快照与点击共用内容度量。 */
final class KitGridExample implements UiWidget, UiLayoutSource {
    private static final int ITEM_COUNT = 8;
    private static final int COLUMNS = 3;
    private static final int CELL_HEIGHT = 24;
    private static final int GAP = 6;
    private static final int HEADER_HEIGHT = 20;
    static final int CONTENT_HEIGHT = HEADER_HEIGHT + 3 * CELL_HEIGHT + 2 * GAP;

    private final UiStyleSource theme;
    private final List<Component> labels = new ArrayList<>();
    private UiRect bounds = new UiRect(0, 0, 0, 0);
    private UiGridMetrics grid = new UiGridMetrics(COLUMNS, 1, CELL_HEIGHT, GAP, GAP);
    private List<UiRect> cells = List.of();
    private Component selection = text("hint");

    KitGridExample(UiStyleSource theme) {
        this.theme = theme;
        for (int index = 0; index < ITEM_COUNT; index++) labels.add(text("cell", index));
    }

    /** {@inheritDoc} */
    @Override public UiRect bounds() { return bounds; }

    /** 重排时缓存屏幕矩形，稳定绘制帧直接读取。 */
    @Override public void setBounds(int x, int y, int width, int height) {
        UiRect next = new UiRect(x, y, Math.max(0, width), Math.max(0, height));
        if (bounds.equals(next)) return;
        bounds = next;
        int cellWidth = Math.max(1, (bounds.width() - (COLUMNS - 1) * GAP) / COLUMNS);
        if (grid.cellWidth() != cellWidth) grid = new UiGridMetrics(COLUMNS, cellWidth, CELL_HEIGHT, GAP, GAP);
        List<UiRect> positioned = new ArrayList<>();
        for (int index = 0; index < ITEM_COUNT; index++) {
            UiRect cell = grid.cellBounds(index);
            positioned.add(new UiRect(x + cell.x(), y + HEADER_HEIGHT + cell.y(), cell.width(), cell.height()));
        }
        cells = List.copyOf(positioned);
    }

    private int indexAt(double x, double y) {
        return bounds.contains(x, y) ? grid.indexAt(x - bounds.x(), y - bounds.y() - HEADER_HEIGHT, ITEM_COUNT) : -1;
    }

    /** 间隙与末行空格没有叶子命中。 */
    @Override public @Nullable UiPointerHit hitTest(double x, double y) {
        int index = indexAt(x, y);
        return index < 0 ? null : UiPointerHit.leaf(this, x, y)
                .withRow(this, index, Integer.toString(index), cells.get(index));
    }

    /** 本地索引只在实际格子的左键按下时改变。 */
    @Override public boolean mousePressed(UiInputContext context) {
        if (context.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        int index = indexAt(context.pointerX(), context.pointerY());
        if (index < 0) return false;
        selection = text("selection", index);
        return true;
    }

    /** {@inheritDoc} */
    @Override public void render(GuiGraphics graphics, Font font, UiPointer pointer) {
        UiRenderLayers.enableScissor(graphics, bounds);
        try {
            graphics.drawString(font, selection, bounds.x(), bounds.y(), theme.textColor(true), false);
            int hovered = pointer.allowed() ? indexAt(pointer.x(), pointer.y()) : -1;
            for (int index = 0; index < cells.size(); index++) {
                UiRect cell = cells.get(index);
                theme.drawChip(graphics, cell, false, index == hovered);
                graphics.drawCenteredString(font, labels.get(index), cell.x() + cell.width() / 2,
                        cell.y() + (cell.height() - font.lineHeight) / 2, theme.textColor(true));
            }
        } finally {
            UiRenderLayers.disableScissor(graphics);
        }
    }

    /** {@inheritDoc} */
    @Override public List<UiLayoutNode> layoutNodes() {
        List<UiLayoutNode> nodes = new ArrayList<>();
        for (int index = 0; index < cells.size(); index++) {
            UiRect cell = cells.get(index);
            nodes.add(new UiLayoutNode(UiLayoutNode.Kind.ROW, cell, labels.get(index),
                    cell.width(), cell.width(), true, true, List.of()));
        }
        return List.copyOf(nodes);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable(KitShowcaseContent.UI + "container.grid." + key, args);
    }
}
