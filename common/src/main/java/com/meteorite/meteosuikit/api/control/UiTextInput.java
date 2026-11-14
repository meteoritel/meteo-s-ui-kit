package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.input.UiFocusState;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import java.util.function.Supplier;

import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.meteorite.meteosuikit.api.text.UiSingleLineEditor;
import com.meteorite.meteosuikit.api.text.UiTextEditSession;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.input.UiHistoryShortcut;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.style.UiMinecraftTheme;
import java.util.Objects;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.locale.Language;
import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.util.FormattedCharSequence;
import java.text.Bidi;
import java.util.ArrayList;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * 单行文本输入框。
 * <p>保留原版 {@link EditBox} 编辑模型；私有单行视图统一无阴影文字、选区、光标与指针几何，主题绘制内凹像素框；
 * 支持占位提示、长度限制、输入过滤、提交（Enter）与取消（Esc）。
 * <p>点击输入框可定位光标并拖动选择；焦点顺序由宿主的 {@code UiFocusManager} 统一维护。
 */
public final class UiTextInput implements UiSingleLineEditor, UiStatefulWidget, UiLayoutSource, UiInteractionSession.Target {

    // 左右内边距
    private static final int PADDING_X = TextInputSupport.PADDING_X;
    // 上下内边距
    private static final int PADDING_Y = TextInputSupport.PADDING_Y;
    // 最小宽度
    private static final int MIN_WIDTH = TextInputSupport.MIN_WIDTH;
    private static final long BLINK_INTERVAL_MILLIS = 300L;

    // 原版输入框
    private final TrackedEditBox editBox;
    private final UiTextEditSession editSession;
    private final Font font;
    // 控件矩形
    private final UiWidgetShell shell = new UiWidgetShell(this, this::endInteraction);
    // 提交回调（Enter）
    private Consumer<String> onCommit;
    // 取消回调（Esc）
    private Runnable onCancel;
    private @Nullable Consumer<UiInteractionSession.EndReason> onInteractionEnd;
    // 无障碍名称
    private Component accessibleName;
    // 是否可编辑（原版 EditBox 未暴露读取方法，这里自行记录）
    private boolean editable = true;

    // 注入的宿主主题
    private final Supplier<? extends UiStyleSource> theme;
    private final SingleLineView textView = new SingleLineView();
    private long focusedTime;
    private Component hint;

    /** {@inheritDoc} */
    @Override public UiRect bounds() { return shell.bounds(); }
    /** {@inheritDoc} */
    @Override public boolean isEnabled() { return shell.isEnabled(); }
    /** {@inheritDoc} */
    @Override public boolean isVisible() { return shell.isVisible(); }
    /** {@inheritDoc} */
    @Override public UiFocusState focusState() { return shell.focusState(); }
    /** {@inheritDoc} */
    @Override public void setLayoutKey(String key) { shell.setLayoutKey(key); }
    /** {@inheritDoc} */
    @Override public UiTextInput setEnabled(boolean enabled) {
        shell.setEnabled(enabled);
        editSession.setEnabled(enabled);
        editBox.active = enabled;
        return this;
    }

    /** 使用原版默认主题创建单行输入。 */
    public UiTextInput(Font font, Component hint) { this(() -> UiMinecraftTheme.INSTANCE, font, hint); }

    /** 注入稳定主题来源，文本模型与局部历史由同一实例保留。 */
    public UiTextInput(Supplier<? extends UiStyleSource> theme, Font font, Component hint) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.font = font;
        this.hint = hint;
        this.editBox = new TrackedEditBox(font, theme.get().rowHeight());
        this.editBox.setBordered(false);
        // 长度由码点过滤器控制；程序回填不经过原版截断。
        this.editBox.setMaxLength(Integer.MAX_VALUE);
        this.accessibleName = hint;
        editSession = new UiTextEditSession(new UiTextEditSession.Model() {
            @Override
            public String value() {
                return editBox.getValue();
            }

            @Override
            public void write(String text) {
                editBox.setValue(text);
            }

            @Override
            public int cursor() {
                return editBox.getCursorPosition();
            }

            @Override
            public int anchor() {
                return editBox.anchor();
            }

            @Override
            public void seek(double pointerX, double pointerY) {
                seekCursor(pointerX, pointerY);
            }

            @Override
            public void select(int anchor, int cursor) {
                editBox.setHighlightPos(anchor);
                editBox.setCursorPosition(cursor);
            }
        }, 256);
        installFilter();
        this.editBox.setCanLoseFocus(true);
        this.editBox.setResponder(text -> editSession.changed());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value() {
        return editBox.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput setValue(String value) {
        backfill(value == null ? "" : value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput clear() {
        backfill("");
        return this;
    }

    // 设置长度上限（Unicode 码点）：超出上限的输入整体被拒绝，不截断，并触发 onOverflow

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput setMaxLength(int maxLength) {
        editSession.acceptance().setMaximum(Math.max(1, maxLength));
        installFilter();
        return this;
    }

    // 设置输入过滤规则：与码点上限共同构成 EditBox 的过滤器
    public UiTextInput setFilter(Predicate<String> filter) {
        editSession.acceptance().setFilter(filter);
        installFilter();
        return this;
    }

    // 安装过滤器：先判码点上限（超限整体拒绝并上报），再判业务规则
    private void installFilter() {
        editBox.setFilter(editSession.acceptance()::accepts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput setOnOverflow(@Nullable Runnable onOverflow) {
        editSession.acceptance().setOnOverflow(onOverflow);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput setOnValueChanged(@Nullable Runnable onValueChanged) {
        editSession.acceptance().setOnChanged(() -> {
            UiInputScope scope = focusState().scope();
            if (onInteractionEnd != null && isFocused() && scope != null) {
                scope.interaction().begin(this, UiInteractionSession.Source.TEXT, 0);
            }
            if (onValueChanged != null) onValueChanged.run();
        });
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput setEditable(boolean editable) {
        if (!editable) endInteraction(UiInteractionSession.EndReason.DISABLED);
        this.editable = editable;
        editBox.setEditable(editable);
        return this;
    }

    public UiTextInput setHint(Component hint) {
        this.hint = hint;
        return this;
    }

    /**
     * 数值编辑绑定通过作用域会话统一接收完成或取消通知。
     */
    @Override
    public UiTextInput setOnInteractionEnd(@Nullable Consumer<UiInteractionSession.EndReason> handler) {
        onInteractionEnd = handler;
        return this;
    }

    @Override
    public void interactionEnded(UiInteractionSession.EndReason reason) {
        if (onInteractionEnd != null) onInteractionEnd.accept(reason);
    }

    /** 设置 Enter 的显式提交回调。 */
    @Override
    public UiTextInput setOnCommit(Consumer<String> onCommit) {
        this.onCommit = onCommit;
        return this;
    }

    /** 设置 Esc 的取消回调。 */
    @Override
    public UiTextInput setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    /** 隐藏时结束当前交互并退出焦点序列，恢复显示保留内容。 */
    public UiTextInput setVisible(boolean nextVisible) {
        shell.setVisible(nextVisible);
        editSession.setVisible(nextVisible);
        editBox.setVisible(nextVisible);
        return this;
    }

    // 全选当前文本

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput selectAll() {
        editBox.setHighlightPos(0);
        editBox.moveCursorToEnd(true);
        return this;
    }

    // 把光标与水平滚动复位到开头；回填后调用一次，编辑期间不调用

    /**
     * {@inheritDoc}
     */
    @Override
    public UiTextInput moveCursorToStart() {
        editBox.moveCursorToStart(false);
        editBox.setHighlightPos(0);
        return this;
    }

    /**
     * 更新布局；文本宽度变化时重算水平视口，保留光标与选择锚点。
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        shell.setBounds(x, y, width, height);
        editBox.setX(bounds().x() + PADDING_X);
        editBox.setY(bounds().y() + Math.max(PADDING_Y, (bounds().height() - font.lineHeight) / 2));
        editBox.setWidth(Math.max(1, bounds().width() - PADDING_X * 2));
        editBox.setHeight(Math.max(1, bounds().height() - PADDING_Y * 2));
    }

    /**
     * 导出输入框的排版快照。
     *
     * <p>文案取当前输入值；输入为空时退回占位提示，与屏幕上的可见内容一致。
     * 带宽扣除左右内边距，探针据此判断长文本是否被横向滚动裁切。</p>
     */
    @Override
    public List<UiLayoutNode> layoutNodes() {
        if (!isVisible() || bounds().width() <= 0 || bounds().height() <= 0) {
            return List.of();
        }
        String text = value();
        Component content = text.isEmpty() ? accessibleName : Component.literal(text);
        int budget = Math.max(0, bounds().width() - PADDING_X * 2);
        return List.of(new UiLayoutNode(UiLayoutNode.Kind.TEXT_INPUT, bounds(), content, font.width(content),
                budget, isEnabled() && editable, true, List.of()).identified(shell.layoutKey(), this));
    }

    /** {@inheritDoc} */
    @Override
    public void render(GuiGraphics graphics, Font font, UiPointer incomingPointer) {
        if (!isVisible()) return;
        TextInputSupport.drawBackground(graphics, bounds(), theme);
        UiRenderLayers.draw(graphics, UiRenderLayers.FOREGROUND, () -> textView.render(graphics));
        shell.renderFocusOutline(graphics, bounds(), theme);
    }

    /** {@inheritDoc} */
    @Override
    public boolean mousePressed(UiInputContext context) {
        return editSession.beginSelection(context, bounds(), canFocus());
    }

    @Override
    public boolean mouseReleased(UiInputContext context) {
        return editSession.mouseReleased(context);
    }

    @Override
    public boolean mouseDragged(UiInputContext context, double dragX, double dragY) {
        return editSession.mouseDragged(context, dragX, dragY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endInteraction(UiInteractionSession.EndReason reason) {
        editSession.endInteraction(reason);
        UiInputScope scope = focusState().scope();
        if (scope != null) scope.interaction().end(this, reason);
    }

    private void seekCursor(double mouseX, double mouseY) {
        textView.sync();
        editBox.moveCursorTo(textView.indexAt(mouseX - editBox.getX()), false);
        if (!editSession.isSelecting()) return;
        int cursor = editBox.getCursorPosition();
        // 越界拖动越过可见文本的端点，推动水平视口；按码点移动，避免拆开代理对。
        if (mouseX < editBox.getX() && cursor > 0) {
            editBox.setCursorPosition(value().offsetByCodePoints(cursor, -1));
        } else if (mouseX >= editBox.getX() + editBox.getWidth() && cursor < value().length()) {
            editBox.setCursorPosition(value().offsetByCodePoints(cursor, 1));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!canFocus() || !editBox.isFocused()) {
            return false;
        }
        UiHistoryShortcut shortcut = UiHistoryShortcut.fromKey(keyCode, modifiers);
        if (shortcut != null && editable) {
            editSession.applyHistory(shortcut);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            editSession.endInteraction(UiInteractionSession.EndReason.RELEASE);
            UiInputScope scope = focusState().scope();
            if (scope == null || !scope.interaction().end(this, UiInteractionSession.EndReason.RELEASE)) {
                if (onCommit != null) onCommit.accept(editBox.getValue());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            editSession.endInteraction(UiInteractionSession.EndReason.RELEASE);
            if (onCancel != null) {
                onCancel.run();
                return true;
            }
            return false;
        }
        return editSession.edit(() -> editBox.keyPressed(keyCode, scanCode, modifiers));
    }

    /** {@inheritDoc} */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!canFocus() || !editBox.isFocused()) {
            return false;
        }
        return editSession.edit(() -> editBox.charTyped(codePoint, modifiers));
    }

    /** {@inheritDoc} */
    @Override
    public boolean canFocus() {
        return isVisible() && isEnabled() && editable;
    }

    @Override
    public void focusChanged(boolean focused) {
        editBox.setFocused(focused);
        if (focused) focusedTime = Util.getMillis();
    }

    /** {@inheritDoc} */
    @Override
    public boolean activate() {
        if (!canFocus() || onCommit == null) {
            return false;
        }
        onCommit.accept(editBox.getValue());
        return true;
    }

    @Override
    public Component accessibleName() {
        return accessibleName;
    }

    private void backfill(String value) {
        editSession.backfill(value);
    }

    /** 单行的视觉顺序、光标和指针共用同一水平像素窗口。 */
    private final class SingleLineView {
        private String text;
        private Language language;
        private List<TextRun> runs = List.of();
        private int[] caretX = {0};
        private int width;
        private int pixelOffset;

        private void sync() {
            String value = value();
            Language current = Language.getInstance();
            if (!value.equals(text) || language != current) {
                text = value;
                language = current;
                layout();
            }
            int available = Math.max(0, bounds().width() - PADDING_X * 2 - 1);
            int cursor = caretX[editBox.getCursorPosition()];
            if (cursor < pixelOffset) pixelOffset = cursor;
            else if (cursor > pixelOffset + available) pixelOffset = cursor - available;
            pixelOffset = Math.clamp(pixelOffset, 0, Math.max(0, width - available));
        }

        private void layout() {
            caretX = new int[text.length() + 1];
            width = 0;
            if (text.isEmpty()) { runs = List.of(); return; }
            Bidi bidi = new Bidi(text, language.isDefaultRightToLeft()
                    ? Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT : Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            TextRun[] ordered = new TextRun[bidi.getRunCount()];
            byte[] levels = new byte[ordered.length];
            for (int index = 0; index < ordered.length; index++) {
                levels[index] = (byte) bidi.getRunLevel(index);
                int start = bidi.getRunStart(index);
                int end = bidi.getRunLimit(index);
                boolean reversed = (levels[index] & 1) != 0;
                ordered[index] = new TextRun(start, end, reversed,
                        FormattedBidiReorder.reorder(Component.literal(text.substring(start, end)), reversed));
            }
            Bidi.reorderVisually(levels, 0, ordered, 0, ordered.length);
            List<TextRun> result = new ArrayList<>();
            for (TextRun run : ordered) {
                int runWidth = font.width(run.visual());
                for (int index = run.start(); index <= run.end();) {
                    int prefix = font.width(FormattedBidiReorder.reorder(
                            Component.literal(text.substring(run.start(), index)), run.reversed()));
                    caretX[index] = width + (run.reversed() ? runWidth - prefix : prefix);
                    if (index == run.end()) break;
                    index = text.offsetByCodePoints(index, 1);
                }
                result.add(run);
                width += runWidth;
            }
            runs = List.copyOf(result);
        }

        private int indexAt(double x) {
            double desired = x + pixelOffset;
            int closest = 0;
            double distance = Double.POSITIVE_INFINITY;
            for (int index = 0; index <= text.length();) {
                double next = Math.abs(caretX[index] - desired);
                if (next < distance) { distance = next; closest = index; }
                if (index == text.length()) break;
                index = text.offsetByCodePoints(index, 1);
            }
            return closest;
        }

        private void render(GuiGraphics graphics) {
            sync();
            UiRect clip = new UiRect(editBox.getX(), bounds().y(),
                    Math.max(0, bounds().width() - PADDING_X * 2), bounds().height());
            UiRenderLayers.enableScissor(graphics, clip);
            try {
                int x = editBox.getX() - pixelOffset;
                int y = editBox.getY();
                int color = isEnabled() && editable ? theme.get().surfaces().inputText()
                        : theme.get().surfaces().disabledText();
                if (text.isEmpty()) {
                    if (hint != null) graphics.drawString(font, hint.getVisualOrderText(), editBox.getX(), y,
                            theme.get().surfaces().disabledText(), false);
                } else {
                    int selectedStart = Math.min(editBox.anchor(), editBox.getCursorPosition());
                    int selectedEnd = Math.max(editBox.anchor(), editBox.getCursorPosition());
                    int runX = x;
                    for (TextRun run : runs) {
                        int start = Math.max(selectedStart, run.start());
                        int end = Math.min(selectedEnd, run.end());
                        if (start < end) {
                            int left = runX + runPosition(run, start);
                            int right = runX + runPosition(run, end);
                            graphics.fill(Math.min(left, right), y - 1, Math.max(left, right), y + font.lineHeight,
                                    theme.get().surfaces().selectionFill());
                        }
                        graphics.drawString(font, run.visual(), runX, y, color, false);
                        runX += font.width(run.visual());
                    }
                }
                if (isFocused() && (Util.getMillis() - focusedTime) / BLINK_INTERVAL_MILLIS % 2 == 0) {
                    int cursor = x + caretX[editBox.getCursorPosition()];
                    graphics.fill(cursor, y - 1, cursor + 1, y + font.lineHeight, color);
                }
            } finally {
                UiRenderLayers.disableScissor(graphics);
            }
        }

        private int runPosition(TextRun run, int index) {
            int prefix = font.width(FormattedBidiReorder.reorder(
                    Component.literal(text.substring(run.start(), index)), run.reversed()));
            return run.reversed() ? font.width(run.visual()) - prefix : prefix;
        }
    }

    private record TextRun(int start, int end, boolean reversed, FormattedCharSequence visual) { }

    // 原版未暴露选择锚点；记录公开设置入口，避免用重复文本猜测选择方向。
    private static final class TrackedEditBox extends EditBox {
        private int anchor;

        private TrackedEditBox(Font font, int rowHeight) {
            super(font, 0, 0, MIN_WIDTH, rowHeight, Component.empty());
        }

        @Override
        public void setHighlightPos(int position) {
            super.setHighlightPos(position);
            anchor = Math.clamp(position, 0, getValue().length());
        }

        private int anchor() {
            return anchor;
        }
    }

    public UiTextInput setAccessibleName(Component name) {
        this.accessibleName = name;
        return this;
    }
    /** 文本选择器与可见输入框共用指针归属。 */
    @Override
    public @Nullable UiPointerHit hitTest(double x, double y) {
        UiPointerHit hit = UiSingleLineEditor.super.hitTest(x, y);
        return hit == null ? null : editSession.includeSelectionOwner(hit);
    }
}
