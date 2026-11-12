package com.meteorite.meteosuikit.api.layout;

import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.geometry.UiTransform;
import com.meteorite.meteosuikit.api.render.UiIcon;
import com.meteorite.meteosuikit.api.text.TextMeasurer;
import com.meteorite.meteosuikit.api.text.TextScroll;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 只读文档渲染器：声明式内容版本门控构建，脏标记门控排版，绘制与命中复用同一份几何缓存。
 * 命中、动作及业务 payload 由宿主解释。
 * 仅在客户端线程使用；视口及鼠标均使用调用方 GUI 坐标，外部 pose 仅支持正向轴对齐缩放和平移。
 *
 * <p>排版使用自然尺寸，默认单行悬停滚动，也可显式选择折行；文本宽度以视口宽度为上限，
 * 超出部分不参与排版宽度，改为悬停时在带内滚动（见 {@link TextScroll}）。因此平移只在
 * 图标本身超出可视区时才需要，且平移与缩放都不触发重排。</p>
 *
 * <p>Frame 内的子文档与父文档共用同一坐标系：父文档把收到的鼠标坐标换算到内容坐标后透传，
 * 子文档的悬停滚动与命中使用同一套几何。</p>
 */
public final class UiDocument implements UiInputTarget {
    /** 文本在逻辑行内的显示政策。 */
    public enum TextOverflow {
        /** 自然单行，超宽部分悬停滚动。 */
        HOVER_SCROLL,
        /** 带样式折行，逻辑动作与父行身份保持。 */
        WRAP
    }
    private TextOverflow textOverflow = TextOverflow.HOVER_SCROLL;
    private long layoutRevision;
    private static final int PADDING = 2;
    private static final int ICON_GAP = 3;
    /** 行文本带的最小宽度：图标再多也留出这一截，让超宽文本仍可悬停滚动。 */
    private static final int MIN_TEXT_BUDGET = 12;
    /** 连线柱到行左边界（即缩进起点）的水平距离。 */
    private static final int BRANCH_GAP = 6;
    private final TextMeasurer measurer;
    private final UiTransform transform;
    private final UiMetrics metrics;
    private final int branchColor;
    private List<UiNode> content = List.of();
    private int layoutWidth = -1;
    private LayoutBlock[] blocks = new LayoutBlock[0];
    /** 每个块的下标对应一份滚动计时：悬停且文本超宽时逐帧递增，否则归零。 */
    private int[] blockTicks = new int[0];
    private boolean dirty = true;
    private boolean versioned;
    private long revision;
    private int contentWidth;
    private int contentHeight;

    public UiDocument(TextMeasurer measurer, boolean profiling) {
        this(measurer, new UiTransform(), 0, profiling);
    }

    public UiDocument(TextMeasurer measurer, UiTransform transform, int branchColor, boolean profiling) {
        this.measurer = Objects.requireNonNull(measurer);
        this.transform = transform;
        this.branchColor = branchColor;
        this.metrics = new UiMetrics(profiling);
    }

    /** 更换文本政策后重新排版。 */
    public void setTextOverflow(TextOverflow policy) {
        if (textOverflow == policy) return;
        textOverflow = Objects.requireNonNull(policy);
        dirty = true;
    }
    /** 排版代次供阅读控件取消旧目标的按压。 */
    public long layoutRevision() { layout(); return layoutRevision; }

    public UiTransform transform() { return transform; }
    public UiMetrics metrics() { return metrics; }
    /** 与变换共享的视口；布局、裁剪、命中及 fit 使用同一份几何。 */
    public UiRect viewport() { return transform.viewport(); }
    public int contentWidth() { layout(); return contentWidth; }
    public int contentHeight() { layout(); return contentHeight; }

    /** 按内容版本接收文档；相同版本复用已接收内容，父行必须是同列表中位于子行之前的 Row。
     * @throws IllegalArgumentException 父行下标未指向前面的 Row
     */
    public void setContent(long newRevision, Supplier<List<UiNode>> builder) {
        if (versioned && revision == newRevision) return;
        long start = metrics.start();
        List<UiNode> next = List.copyOf(builder.get());
        validateParents(next);
        content = next;
        revision = newRevision;
        versioned = true;
        dirty = true;
        metrics.built(start);
    }

    /** 接收静态文档；父行必须是同列表中位于子行之前的 Row。
     * @throws IllegalArgumentException 父行下标未指向前面的 Row
     */
    public void setContent(List<UiNode> nodes) {
        long start = metrics.start();
        List<UiNode> next = List.copyOf(nodes);
        if (content.equals(next)) { versioned = false; return; }
        validateParents(next);
        content = next;
        versioned = false;
        dirty = true;
        metrics.built(start);
    }

    /** 设置完整视口；负尺寸钳零，移动视口保留缩放、平移与布局缓存。 */
    public void setViewport(int x, int y, int width, int height) {
        transform.setViewport(x, y, width, height);
    }

    // 字体或资源重载后由宿主调用；内容改变时必须同时更新内容版本。
    public void invalidateLayout() { dirty = true; }

    public void layout() {
        UiRect viewport = viewport();
        if (!dirty && layoutWidth == viewport.width()) return;
        long start = metrics.start();
        int size = content.size();
        int[] parentOf = new int[size];
        boolean[] hasLaterSibling = new boolean[size];
        UiRect[] rowRects = new UiRect[size];
        int[] lastChildCenterY = new int[size];
        int[] lastIndexWithParent = new int[size];
        for (int i = 0; i < size; i++) {
            parentOf[i] = content.get(i) instanceof UiNode.Row row ? row.parentRow() : UiNode.NO_PARENT;
            lastChildCenterY[i] = -1;
            lastIndexWithParent[i] = -1;
        }
        // 反向扫一遍得出「该行之后是否还有同父兄弟」，续行竖线据此只在需要处落笔。
        for (int i = size - 1; i >= 0; i--) {
            int parent = parentOf[i];
            if (parent < 0 || parent >= size) continue;
            hasLaterSibling[i] = lastIndexWithParent[parent] >= 0;
            lastIndexWithParent[parent] = i;
        }
        List<LayoutBlock> next = new ArrayList<>(size);
        int y = 0;
        int widest = 0;
        for (int index = 0; index < size; index++) {
            UiNode node = content.get(index);
            if (node instanceof UiNode.Gap(int height)) {
                y += height;
            } else if (node instanceof UiNode.Row row) {
                LayoutBlock block = layoutRow(row, index, y);
                BranchPaint branch = branchFor(index, row, y, parentOf, hasLaterSibling,
                        rowRects, lastChildCenterY, block.rect());
                block = new LayoutBlock(block.rect(), block.row().withBranch(branch), 0, null);
                rowRects[index] = block.rect();
                int parent = parentOf[index];
                if (parent >= 0 && parent < size && rowRects[parent] != null) {
                    lastChildCenterY[parent] = block.rect().y() + rowHeight(row) / 2;
                }
                next.add(block);
                y = block.rect().bottom();
                widest = Math.max(widest, block.rect().right());
            } else if (node instanceof UiNode.Divider(int color)) {
                UiRect rect = new UiRect(0, y, viewport.width(), 1);
                next.add(new LayoutBlock(rect, null, color, null));
                y++;
                widest = Math.max(widest, rect.width());
            } else if (node instanceof UiNode.Frame(var spec, var children)) {
                UiDocument child = new UiDocument(measurer, spec.transform(), spec.branchColor(), metrics.enabled());
                child.setViewport(0, y, spec.width(), spec.height());
                child.setTextOverflow(textOverflow);
                child.setContent(children);
                child.layout();
                next.add(new LayoutBlock(child.viewport(), null, 0, child));
                y += spec.height();
                widest = Math.max(widest, spec.width());
            }
        }
        blocks = next.toArray(LayoutBlock[]::new);
        if (blockTicks.length != blocks.length) blockTicks = new int[blocks.length];
        contentWidth = widest;
        contentHeight = y;
        layoutWidth = viewport.width();
        dirty = false;
        layoutRevision++;
        metrics.laidOut(start);
    }

    /** 导出一份只读排版快照；坐标为本文档内容坐标，不含视口平移。 */
    public List<UiLayoutNode> layoutNodes() {
        return layoutNodes(0, 0);
    }

    /**
     * 导出调用方 GUI 坐标快照，包含本文档及 Frame 内子文档的实际平移与缩放。
     * 节点边界在最终 GUI 像素四舍五入；外层 pose 由宿主继续转换，快照保持只读。
     */
    public List<UiLayoutNode> screenLayoutNodes() {
        return screenLayoutNodes(1, 0, 0);
    }

    /**
     * 从真实排版缓存导出行和图标，按文档顺序包含嵌套 Frame。
     * 返回不可变快照，不做裁剪或动作去重；宿主可按 hit().action() 筛选键盘动作。
     * 内容、字体、尺寸或资源变化后重新查询；缩放和平移后重新查询屏幕矩形。
     * 同一内容结构下 path 标识每次出现，业务跨版本身份由 payload 决定。
     */
    public List<UiDocumentTarget> targets() {
        List<UiDocumentTarget> targets = new ArrayList<>();
        collectTargets(targets, new TargetProjection(new TargetSpace(1, 0, 0),
                new TargetSpace(transform.scale(), transform.toScreenX(0), transform.toScreenY(0)), List.of(), null));
        return List.copyOf(targets);
    }

    private void collectTargets(List<UiDocumentTarget> targets, TargetProjection projection) {
        layout();
        for (int index = 0; index < blocks.length; index++) {
            LayoutBlock block = blocks[index];
            List<Integer> path = new ArrayList<>(projection.path());
            path.add(index);
            if (block.frame() != null) {
                UiDocument child = block.frame();
                UiRect clip = projection.content().bounds(child.viewport());
                if (projection.clip() != null) clip = clip.intersect(projection.clip());
                child.collectTargets(targets, new TargetProjection(projection.content().child(child.transform),
                        projection.screen().child(child.transform), path, clip));
                continue;
            }
            RowPaint row = block.row();
            if (row == null) continue;
            addTarget(targets, projection.at(path, 0), row.target(), row.label());
            if (row.leadingTarget() != null) addTarget(targets, projection.at(path, 1), row.leadingTarget(), row.label());
            for (int icon = 0; icon < row.iconTargets().length; icon++) {
                UiDocumentHit hit = row.iconTargets()[icon];
                addTarget(targets, projection.at(path, icon + 2), hit, row.label());
            }
        }
    }

    private void addTarget(List<UiDocumentTarget> targets, TargetProjection projection, UiDocumentHit hit, Component fallback) {
        Component label = hit.kind() == UiDocumentHit.Kind.ICON && !hit.tooltip().isEmpty() ? hit.tooltip().getFirst() : fallback;
        targets.add(new UiDocumentTarget(this, projection.path(), hit, label,
                projection.content().bounds(hit.rect()), projection.screen().bounds(hit.rect()), projection.clip()));
    }

    /** 内容坐标到某个祖先坐标空间的只读正向变换。 */
    private record TargetSpace(double scale, double x, double y) {
        TargetSpace child(UiTransform transform) {
            return new TargetSpace(scale * transform.scale(), x + scale * transform.toScreenX(0), y + scale * transform.toScreenY(0));
        }
        UiRect bounds(UiRect rect) {
            int left = (int) Math.round(x + scale * rect.x());
            int top = (int) Math.round(y + scale * rect.y());
            return new UiRect(left, top, (int) Math.round(x + scale * rect.right()) - left,
                    (int) Math.round(y + scale * rect.bottom()) - top);
        }
    }

    /** 一次导出遍历共享的祖先变换与出现路径。 */
    private record TargetProjection(TargetSpace content, TargetSpace screen, List<Integer> path, @Nullable UiRect clip) {
        TargetProjection at(List<Integer> blockPath, int slot) {
            List<Integer> occurrence = new ArrayList<>(blockPath);
            occurrence.add(slot);
            return new TargetProjection(content, screen, occurrence, clip);
        }
    }

    private List<UiLayoutNode> screenLayoutNodes(double parentScale, double parentX, double parentY) {
        layout();
        double scale = parentScale * transform.scale();
        double x = parentX + parentScale * transform.toScreenX(0);
        double y = parentY + parentScale * transform.toScreenY(0);
        List<UiLayoutNode> nodes = new ArrayList<>(blocks.length);
        for (LayoutBlock block : blocks) {
            UiRect local = block.rect();
            int left = (int) Math.round(x + local.x() * scale);
            int top = (int) Math.round(y + local.y() * scale);
            UiRect rect = new UiRect(left, top, (int) Math.round(x + local.right() * scale) - left,
                    (int) Math.round(y + local.bottom() * scale) - top);
            if (block.row() != null) {
                RowPaint row = block.row();
                nodes.add(new UiLayoutNode(UiLayoutNode.Kind.ROW, rect, row.label(),
                        (int) Math.round(row.textWidth() * scale), (int) Math.round(row.textBand() * scale),
                        true, true, List.of()));
            } else if (block.frame() != null) {
                nodes.add(new UiLayoutNode(UiLayoutNode.Kind.FRAME, rect, Component.empty(), 0, 0,
                        true, true, block.frame().screenLayoutNodes(scale, x, y)).clippedTo(rect));
            } else {
                nodes.add(new UiLayoutNode(UiLayoutNode.Kind.DIVIDER, rect, Component.empty(), 0, 0, true, true, List.of()));
            }
        }
        return List.copyOf(nodes);
    }

    /**
     * 导出一份只读排版快照，全部节点矩形按 {@code (dx, dy)} 平移。
     * 子文档的块坐标相对自身内容原点，宿主并回父文档坐标时用这里的偏移量结算。
     */
    public List<UiLayoutNode> layoutNodes(int dx, int dy) {
        layout();
        List<UiLayoutNode> nodes = new ArrayList<>(blocks.length);
        for (LayoutBlock block : blocks) {
            UiRect rect = offset(block.rect(), dx, dy);
            if (block.row() != null) {
                RowPaint row = block.row();
                nodes.add(new UiLayoutNode(UiLayoutNode.Kind.ROW, rect, row.label(), row.textWidth(),
                        row.textBand(), true, true, List.of()));
            } else if (block.frame() != null) {
                UiDocument child = block.frame();
                UiRect viewport = child.viewport();
                nodes.add(new UiLayoutNode(UiLayoutNode.Kind.FRAME, rect, Component.empty(), 0, 0, true, true,
                        child.layoutNodes(dx + viewport.x(), dy + viewport.y())));
            } else {
                nodes.add(new UiLayoutNode(UiLayoutNode.Kind.DIVIDER, rect, Component.empty(), 0, 0,
                        true, true, List.of()));
            }
        }
        return nodes;
    }

    private static UiRect offset(UiRect rect, int dx, int dy) {
        if (dx == 0 && dy == 0) return rect;
        return new UiRect(rect.x() + dx, rect.y() + dy, rect.width(), rect.height());
    }

    private static void validateParents(List<UiNode> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) instanceof UiNode.Frame frame) {
                validateParents(frame.content());
            }
            if (!(nodes.get(i) instanceof UiNode.Row row)) continue;
            int parent = row.parentRow();
            if (parent != UiNode.NO_PARENT && (parent >= i || !(nodes.get(parent) instanceof UiNode.Row))) {
                throw new IllegalArgumentException("Row parent must reference a preceding row: " + i + " -> " + parent);
            }
        }
    }

    /**
     * 连线几何只在排版阶段算一次：折线从父行文字下方接到本行垂直中心，
     * 各祖先若有后续兄弟则在同一列落一条穿过本行高度的续行线（逐行相接即视觉连续）。
     */
    @Nullable
    private BranchPaint branchFor(int index, UiNode.Row row, int y, int[] parentOf,
                                  boolean[] hasLaterSibling, UiRect[] rowRects, int[] lastChildCenterY, UiRect rowRect) {
        int parent = parentOf[index];
        // 只接受位于本行之前的父行：链严格递减，既排除自环也排除乱序输入。
        if (branchColor == 0 || parent < 0 || parent >= index) return null;
        UiRect parentRect = rowRects[parent];
        if (parentRect == null) return null;
        int trunkX = rowRect.x() - BRANCH_GAP;
        if (trunkX < 0) return null;
        int height = rowHeight(row);
        int centerY = y + height / 2;
        int previous = lastChildCenterY[parent];
        int from = previous >= 0 ? previous : parentRect.y() + measurer.lineHeight();
        int count = 0;
        for (int a = parent; a >= 0; a = parentOf[a]) {
            if (rowRects[a] != null && hasLaterSibling[a]) count++;
        }
        int[] continuations = new int[count * 3];
        int slot = 0;
        for (int a = parent; a >= 0; a = parentOf[a]) {
            UiRect rect = rowRects[a];
            if (rect == null || !hasLaterSibling[a]) continue;
            continuations[slot] = rect.x() - BRANCH_GAP;
            continuations[slot + 1] = y;
            continuations[slot + 2] = rowRect.bottom();
            slot += 3;
        }
        return new BranchPaint(trunkX, from, centerY, rowRect.x(), continuations);
    }

    private int rowHeight(UiNode.Row row) {
        int height = measurer.lineHeight();
        if (row.leading() != null) height = Math.max(height, row.leading().icon().height());
        for (int i = 0; i < row.icons().size(); i++) height = Math.max(height, row.icons().get(i).icon().height());
        return height + PADDING * 2;
    }

    private LayoutBlock layoutRow(UiNode.Row row, int index, int y) {
        int firstHeight = rowHeight(row);
        int indent = textOverflow == TextOverflow.WRAP ? Math.min(row.indent(), Math.max(0, viewport().width() - MIN_TEXT_BUDGET)) : row.indent();
        int textX = indent + PADDING;
        UiIcon leadingIcon = row.leading() == null ? null : row.leading().icon();
        if (leadingIcon != null) textX += leadingIcon.width() + ICON_GAP;
        int iconsWidth = row.icons().stream().mapToInt(icon -> ICON_GAP + icon.icon().width()).sum();
        int budget = Math.max(MIN_TEXT_BUDGET, viewport().width() - textX - iconsWidth - PADDING);
        List<Component> lines = textOverflow == TextOverflow.WRAP ? measurer.wrap(row.text(), budget) : List.of(row.text());
        if (lines.isEmpty()) lines = List.of(Component.empty());
        int height = firstHeight + (lines.size() - 1) * (measurer.lineHeight() + PADDING * 2);
        List<TextPaint> text = new ArrayList<>();
        List<UiRect> fragments = new ArrayList<>();
        int widest = 0;
        int naturalWidth = 0;
        for (int line = 0; line < lines.size(); line++) {
            Component value = lines.get(line);
            int width = measurer.width(value);
            naturalWidth = Math.max(naturalWidth, width);
            int band = Math.min(width, budget);
            int lineY = y + (line == 0 ? 0 : firstHeight + (line - 1) * (measurer.lineHeight() + PADDING * 2));
            int lineHeight = line == 0 ? firstHeight : measurer.lineHeight() + PADDING * 2;
            text.add(new TextPaint(value.getVisualOrderText(), textX, lineY + (lineHeight - measurer.lineHeight()) / 2, width, budget));
            fragments.add(new UiRect(line == 0 ? indent : textX, lineY,
                    Math.max(1, textX + band + (line == 0 ? iconsWidth : 0) + PADDING - (line == 0 ? indent : textX)), lineHeight));
            widest = Math.max(widest, band);
        }
        UiDocumentHit leadingTarget = leadingIcon == null ? null : new UiDocumentHit(UiDocumentHit.Kind.ICON, index,
                row.leading().payload(), new UiRect(indent + PADDING, y + (firstHeight - leadingIcon.height()) / 2,
                leadingIcon.width(), leadingIcon.height()), row.leading().tooltip(), row.leading().action());
        int x = textX + Math.min(measurer.width(lines.getFirst()), budget);
        UiDocumentHit[] iconTargets = new UiDocumentHit[row.icons().size()];
        UiIcon[] icons = new UiIcon[iconTargets.length];
        for (int i = 0; i < icons.length; i++) {
            UiNode.InlineIcon inline = row.icons().get(i);
            UiIcon icon = inline.icon();
            x += ICON_GAP;
            UiRect iconRect = new UiRect(x, y + (firstHeight - icon.height()) / 2, icon.width(), icon.height());
            iconTargets[i] = new UiDocumentHit(UiDocumentHit.Kind.ICON, index, inline.payload(), iconRect, inline.tooltip(), inline.action());
            icons[i] = icon;
            x += icon.width();
        }
        UiRect rect = new UiRect(indent, y, Math.max(x + PADDING, textX + widest + PADDING) - indent, height);
        UiDocumentHit target = new UiDocumentHit(UiDocumentHit.Kind.ROW, index, row.payload(), rect, row.tooltip(), row.action());
        RowPaint paint = new RowPaint(List.copyOf(text), row.text(), row.color(), naturalWidth, budget,
                leadingIcon, leadingTarget, target, icons, iconTargets, List.copyOf(fragments), null);
        return new LayoutBlock(rect, paint, 0, null);
    }

    /** 无鼠标位置的绘制：等价于整篇（含 Frame 内子文档）都不悬停，长文本不滚动。 */
    public void render(GuiGraphics graphics, Font font) {
        render(graphics, font, UiPointer.at(Double.NaN, Double.NaN));
    }

    /** 使用已解析指针绘制；编辑输入由宿主另外装配。 */
    public void render(GuiGraphics graphics, Font font, UiPointer incomingPointer) {
        layout();
        UiPointer pointer = incomingPointer.resolve(this).forTarget(this);
        double mouseX = pointer.x();
        double mouseY = pointer.y();
        long start = metrics.start();
        UiRect viewport = viewport();
        if (viewport.width() > 0 && viewport.height() > 0) {
            boolean hovering = pointer.contains(viewport);
            double localX = transform.toLocalX(mouseX);
            double localY = transform.toLocalY(mouseY);
            UiRenderLayers.enableScissor(graphics, viewport);
            transform.push(graphics);
            try {
                double top = transform.toLocalY(viewport.y());
                double bottom = transform.toLocalY(viewport.bottom());
                double left = transform.toLocalX(viewport.x());
                double right = transform.toLocalX(viewport.right());
                // 缩放小于 1 时线宽会掉到亚像素而消失，按档位取整保证屏幕上线宽不少于 1 像素。
                int thickness = Math.max(1, (int) Math.ceil(1.0 / transform.scale()));
                for (int i = firstBlockAt(top); i < blocks.length; i++) {
                    LayoutBlock block = blocks[i];
                    if (block.rect().y() >= bottom) break;
                    if (block.rect().right() <= left || block.rect().x() >= right) continue;
                    if (block.row() != null) {
                        boolean hovered = hovering && block.rect().contains(localX, localY);
                        int ticks = hovered ? blockTicks[i] + 1 : 0;
                        blockTicks[i] = ticks;
                        renderRow(graphics, font, block.row(), left, top, right, bottom, thickness, hovered, ticks);
                    } else if (block.frame() != null) {
                        // 鼠标坐标先换算到内容坐标再透传，Frame 内子文档才能像根文档一样判悬停。
                        block.frame().render(graphics, font, pointer.withPosition(localX, localY));
                    } else {
                        graphics.fill(block.rect().x(), block.rect().y(), block.rect().right(),
                                block.rect().bottom(), block.dividerColor());
                    }
                }
            } finally {
                // 先提交仍受裁剪约束的顶点，再恢复外层状态，避免延迟批次泄漏。
                graphics.pose().popPose();
                UiRenderLayers.disableScissor(graphics);
            }
        }
        metrics.rendered(start);
    }

    private void renderRow(GuiGraphics graphics, Font font, RowPaint row,
                           double left, double top, double right, double bottom, int thickness,
                           boolean hovered, int ticks) {
        if (row.branch() != null) renderBranch(graphics, row.branch(), thickness);
        UiDocumentHit leading = row.leadingTarget();
        if (row.leading() != null && leading != null) {
            UiRect rect = leading.rect();
            if (rect.right() > left && rect.x() < right && rect.bottom() > top && rect.y() < bottom) {
                row.leading().render(graphics, rect.x(), rect.y());
            }
        }
        for (TextPaint text : row.text()) {
            TextScroll.draw(graphics, font, text.text(), text.width(), text.x(), text.y(),
                    text.band(), row.color(), textOverflow == TextOverflow.HOVER_SCROLL && hovered, ticks);
        }
        for (int i = 0; i < row.icons().length; i++) {
            UiRect rect = row.iconTargets()[i].rect();
            if (rect.right() > left && rect.x() < right && rect.bottom() > top && rect.y() < bottom) {
                row.icons()[i].render(graphics, rect.x(), rect.y());
            }
        }
    }

    /** 原版进度界面同样用 1 像素填充画连线；这里沿用该做法，因此连线随框一起缩放平移。 */
    private void renderBranch(GuiGraphics graphics, BranchPaint branch, int thickness) {
        int[] continuations = branch.continuations();
        for (int i = 0; i < continuations.length; i += 3) {
            int x = continuations[i];
            graphics.fill(x, continuations[i + 1], x + thickness, continuations[i + 2], branchColor);
        }
        graphics.fill(branch.trunkX(), branch.verticalFromY(), branch.trunkX() + thickness,
                branch.verticalToY(), branchColor);
        graphics.fill(branch.trunkX(), branch.verticalToY(), branch.horizontalToX(),
                branch.verticalToY() + thickness, branchColor);
    }

    /** 查询文档语义命中；返回的矩形属于实际叶子文档的内容坐标。 */
    @Nullable
    public UiDocumentHit hit(double mouseX, double mouseY) {
        layout();
        long start = metrics.start();
        ResolvedHit resolved = resolveHit(mouseX, mouseY);
        UiDocumentHit result = resolved == null ? null : resolved.target();
        metrics.hit(start);
        return result;
    }


    private int firstBlockAt(double y) {
        int low = 0;
        int high = blocks.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (blocks[mid].rect().bottom() <= y) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** 排版缓存中的块，空白不进入绘制数组。 */
    private record LayoutBlock(@NotNull UiRect rect, @Nullable RowPaint row,
                               int dividerColor, @Nullable UiDocument frame) {}

    /** 行绘制数据与命中目标共用自然坐标，避免缩放时重建矩形。 */
    private record TextPaint(FormattedCharSequence text, int x, int y, int width, int band) {}

    private record RowPaint(List<TextPaint> text, Component label, int color, int textWidth, int textBand,
                            @Nullable UiIcon leading, @Nullable UiDocumentHit leadingTarget,
                            UiDocumentHit target, UiIcon[] icons, UiDocumentHit[] iconTargets,
                            List<UiRect> fragments, @Nullable BranchPaint branch) {
        RowPaint withBranch(@Nullable BranchPaint value) {
            return new RowPaint(text, label, color, textWidth, textBand, leading, leadingTarget,
                    target, icons, iconTargets, fragments, value);
        }
    }

    /**
     * 一行的连线几何：折线竖段从 {@code verticalFromY} 到 {@code verticalToY}，
     * 横段从 {@code trunkX} 到 {@code horizontalToX}；{@code continuations} 是
     * (x, y0, y1) 三元组序列，表示穿过本行的祖先续行线。
     */
    private record BranchPaint(int trunkX, int verticalFromY, int verticalToY,
                               int horizontalToX, int[] continuations) {}
    /** 只读命中与绘制共享排版缓存，Frame 保留子文档归属。 */
    @Override
    public @Nullable UiPointerHit hitTest(double x, double y) {
        ResolvedHit resolved = resolveHit(x, y);
        if (resolved == null) return null;
        UiDocumentHit target = resolved.target();
        return resolved.input().withTextTooltip(target.tooltip(), target.rect());
    }

    private record ResolvedHit(UiDocumentHit target, UiPointerHit input) { }

    private @Nullable ResolvedHit resolveHit(double x, double y) {
        layout();
        if (!viewport().contains(x, y)) return null;
        double localX = transform.toLocalX(x);
        double localY = transform.toLocalY(y);
        int index = firstBlockAt(localY);
        if (index >= blocks.length || !blocks[index].rect().contains(localX, localY)) return null;
        LayoutBlock block = blocks[index];
        if (block.frame() != null) {
            ResolvedHit child = block.frame().resolveHit(localX, localY);
            return child == null ? null : new ResolvedHit(child.target(), child.input().within(this));
        }
        RowPaint row = block.row();
        if (row == null) return null;
        if (row.fragments().stream().noneMatch(rect -> rect.contains(localX, localY))) return null;
        UiDocumentHit target = row.target();
        if (row.leadingTarget() != null && row.leadingTarget().rect().contains(localX, localY))
            target = row.leadingTarget();
        else for (UiDocumentHit icon : row.iconTargets()) {
            if (icon.rect().contains(localX, localY)) { target = icon; break; }
        }
        return new ResolvedHit(target, UiPointerHit.leaf(this, localX, localY));
    }

}
