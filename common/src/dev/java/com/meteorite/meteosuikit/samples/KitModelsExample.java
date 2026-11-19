package com.meteorite.meteosuikit.samples;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.util.function.Consumer;
import com.meteorite.meteosuikit.api.input.UiFocusTarget;

import com.meteorite.meteosuikit.api.control.UiControl;
import com.meteorite.meteosuikit.api.control.UiListView;
import com.meteorite.meteosuikit.api.control.UiWidget;
import com.meteorite.meteosuikit.api.control.UiWidgetRow;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiInputContainer;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import org.jetbrains.annotations.Nullable;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.layout.UiLinearLayout;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.style.UiControlRole;
import com.meteorite.meteosuikit.api.tree.UiTreeEditor;
import com.meteorite.meteosuikit.api.tree.UiTreeNavigation;
import com.meteorite.meteosuikit.api.tree.UiTreePath;
import com.meteorite.meteosuikit.api.tree.UiTreeProjection;
import com.meteorite.meteosuikit.api.tree.UiTreeViewState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 本地不可变树和自定义文本模型的真实消费者，无宿主业务类型。 */
final class KitModelsExample implements UiWidget, UiInputContainer, UiLayoutSource {
    private record Node(String key, List<Node> children) {
        private Node { children = List.copyOf(children); }
    }
    private Node root = new Node("root", List.of(new Node("first", List.of()), new Node("second", List.of())));
    private final UiChildren<UiInputTarget> children = new UiChildren<>();
    private final UiWidgetRow toolbar = new UiWidgetRow(4, UiWidgetRow.Alignment.START);
    private final KitTextModelExample text;
    private final UiListView<UiTreeProjection.Entry<Node>> tree;
    private final UiTreeEditor<Node> editor;
    private final KitControlExample restoreView;
    private UiTreeViewState view = UiTreeViewState.EMPTY.select(UiTreePath.ROOT);
    private @Nullable Node savedRoot;
    private UiTreeViewState savedView = UiTreeViewState.EMPTY;
    private boolean refreshingRows;
    private UiTreeProjection<Node> projection;
    private boolean failNextRow;
    private boolean restoredDrawing;
    private final RuntimeException rowFailure = new RuntimeException("controlled row failure");
    private UiRect bounds = new UiRect(0, 0, 0, 0);

    KitModelsExample(Font font, Supplier<? extends UiStyleSource> theme) {
        text = new KitTextModelExample(font);
        tree = new UiListView<>(theme, (graphics, renderFont, entry, index, row, selected, hovered, focused) -> {
            if (failNextRow) { failNextRow = false; throw rowFailure; }
            graphics.drawString(renderFont, label(entry.node().key()), row.x() + entry.depth() * 10,
                    row.y() + 4, theme.get().textColor(true), false);
        });
        tree.setLayoutKey("models/tree"); tree.setItemKey(entry -> "models/" + entry.node().key());
        editor = new UiTreeEditor<>(new UiTreeEditor.Adapter<>() {
            @Override public List<Node> children(Node node) { return node.children(); }
            @Override public Node withChildren(Node node, List<Node> nodes) { return new Node(node.key(), nodes); }
            @Override public int childCapacity(Node node) { return node.key().equals("root") ? UNLIMITED_CHILDREN : 0; }
        }, () -> root, change -> { root = change.after(); rebuild(); }, node -> null, label("invalid"), label("invalid"));
        editor.setOnViewChanged(change -> {
            view = change;
            refreshRows();
        });
        tree.setOnSelectionChanged(index -> {
            if (refreshingRows) return;
            var entry = tree.selectedItem();
            if (entry != null) editor.select(entry.path());
        });
        toolbar.add(button(font, theme, "move", this::moveFirst), UiLinearLayout.Child.remain());
        toolbar.add(button(font, theme, "fold",
                () -> editor.setCollapsed(UiTreePath.ROOT, !view.collapsed().contains(UiTreePath.ROOT))),
                UiLinearLayout.Child.remain());
        toolbar.add(button(font, theme, "save", this::saveView), UiLinearLayout.Child.remain());
        UiControl restore = new UiControl();
        restore.setTheme(theme, UiControlRole.SECONDARY);
        restore.setFocusable(true);
        restore.configure(font, label("restore"), null, List.of(), () -> editor.setViewState(savedView));
        restoreView = new KitControlExample(restore);
        toolbar.add(restoreView, UiLinearLayout.Child.remain());
        toolbar.add(button(font, theme, "form",
                () -> Minecraft.getInstance().setScreen(new KitFormExampleScreen(Minecraft.getInstance().screen))),
                UiLinearLayout.Child.remain());
        toolbar.add(button(font, theme, "failure", () -> failNextRow = true), UiLinearLayout.Child.fixed(72));
        children.addAll(List.of(toolbar, text, tree));
        rebuild();
        editor.select(UiTreePath.ROOT);
    }

    private static UiControl button(Font font, Supplier<? extends UiStyleSource> theme, String name, Runnable action) {
        UiControl button = new UiControl();
        button.setTheme(theme, UiControlRole.SECONDARY);
        button.setFocusable(true);
        button.configure(font, label(name), null, List.of(), action::run);
        return button;
    }

    private void rebuild() {
        projection = UiTreeProjection.single(root, Node::children);
        restoreView.setEnabled(savedRoot == root);
        refreshRows();
    }
    private void saveView() {
        savedRoot = root;
        savedView = editor.viewState();
        restoreView.setEnabled(true);
    }
    private void refreshRows() {
        var rows = projection.visible(view);
        // 列表高亮是整份树状态的投影，程序回填不应再发单选命令覆盖多选快照。
        refreshingRows = true;
        try {
            tree.setItems(rows);
            int selected = -1;
            for (int i = 0; i < rows.size(); i++) if (rows.get(i).path().equals(view.selected())) { selected = i; break; }
            tree.setSelectedIndex(selected);
        } finally { refreshingRows = false; }
    }
    private void moveFirst() {
        UiTreePath source = UiTreePath.ROOT.child(0);
        if (editor.beginMove(source)) {
            var preview = editor.previewMove(root, source, UiTreePath.ROOT.child(1), UiTreeEditor.DropPosition.AFTER);
            editor.commitMove(preview);
        }
    }
    /** 树导航派生新视图，编辑器继续持有结构操作与路径映射。 */
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        var intent = UiTreeNavigation.fromKey(key);
        if (!tree.isFocused() || intent == null) return UiInputContainer.super.keyPressed(key, scan, modifiers);
        UiTreeViewState next = UiTreeNavigation.navigate(projection.visible(view), view, intent, 5);
        editor.setViewState(next);
        return true;
    }
    /** 同一成员树同时提供命中与焦点版本。 */
    @Override public @Nullable UiPointerHit hitTest(double x, double y) { return UiInputContainer.super.hitTest(x, y); }
    /** {@inheritDoc} */
    @Override public long focusRevision() { return UiInputContainer.super.focusRevision(); }
    /** {@inheritDoc} */
    @Override public void addFocusTargets(Consumer<UiFocusTarget> out) { if (isVisible()) children.addFocusTargets(out); }
    @Override public UiChildren<UiInputTarget> inputChildren() { return children; }
    /** {@inheritDoc} */
    @Override public UiRect bounds() { return bounds; }
    /** {@inheritDoc} */
    @Override public void setBounds(int x, int y, int width, int height) {
        bounds = new UiRect(x, y, width, height);
        toolbar.setBounds(x, y, width, 20);
        text.setBounds(x, y + 24, width, 20);
        tree.setBounds(x, y + 48, width, Math.max(0, height - 48));
    }
    /** {@inheritDoc} */
    @Override public void render(GuiGraphics graphics, Font font, UiPointer pointer) {
        toolbar.render(graphics, font, pointer);
        text.render(graphics, font, pointer);
        Matrix4f before = new Matrix4f(graphics.pose().last().pose());
        try { tree.render(graphics, font, pointer); }
        catch (RuntimeException failure) {
            if (failure != rowFailure) throw failure;
            if (!before.equals(graphics.pose().last().pose())) throw new IllegalStateException("List pose was not restored");
            restoredDrawing = true;
        }
        if (restoredDrawing) graphics.drawString(font, label("restored"), bounds.x(), bounds.bottom() + 2, 0xFF55FF55, false);
    }
    /** {@inheritDoc} */
    @Override public List<UiLayoutNode> layoutNodes() {
        var nodes = new ArrayList<>(toolbar.layoutNodes());
        nodes.add(new UiLayoutNode(UiLayoutNode.Kind.TEXT_INPUT, text.bounds(), text.accessibleName(), 0,
                text.bounds().width(), true, true, List.of()));
        nodes.addAll(tree.layoutNodes());
        return List.copyOf(nodes);
    }
    private static Component label(String key) { return Component.translatable(KitShowcaseContent.UI + "showcase.models." + key); }
}
