package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.geometry.UiViewport;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.input.UiPointerHit;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * 原版 Screen 的 kit 接入：统一事件桥接、作用域刷新和卸载；内容与模态优先级由根选择。
 * resize 沿用原版 init，控件由子类稳定持有；removed 结束捕获并释放根拥有的资源，再次打开可重新使用。
 */
public abstract class UiScreen extends Screen implements UiLayoutSource {
    private final UiInputRouter input = new UiInputRouter();
    private Language resources;

    /** 创建原版屏幕，标题由宿主本地化。 */
    protected UiScreen(Component title) { super(title); }
    /** 最小内容宽度；默认自然 GUI 空间。 */
    protected int minimumUiWidth() { return 1; }
    /** 最小内容高度；默认自然 GUI 空间。 */
    protected int minimumUiHeight() { return 1; }
    /** 始终按原版当前尺寸计算，首次 init 与 resize 无需等待绘制。 */
    public final UiViewport viewport() {
        Window window = minecraft.getWindow();
        return UiViewport.fit(window.getWidth(), window.getHeight(), window.getGuiScale(),
                minimumUiWidth(), minimumUiHeight());
    }

    /** 只读当前活动输入层；调用方在实际绘制后读取，不触发业务准备。 */
    @Override public final List<UiLayoutNode> layoutNodes() {
        if (!(inputScope().content() instanceof UiLayoutSource source)) return List.of();
        UiViewport viewport = viewport();
        return source.layoutNodes().stream().map(node -> node.projected(viewport)).toList();
    }

    /** 唯一业务键、裁剪资格与真实 hitTest 共同确认点击点；无副作用。 */
    public final boolean isLayoutTarget(String key, double x, double y) {
        if (key.isEmpty()) return false;
        List<UiLayoutNode> matches = new ArrayList<>();
        collectLayoutMatches(layoutNodes(), key, matches);
        if (matches.size() != 1) return false;
        UiLayoutNode node = matches.getFirst();
        if (!node.enabled() || !node.visible() || node.target() == null || !node.visibleRect().contains(x, y)) return false;
        UiViewport viewport = viewport();
        UiPointerHit hit = inputScope().content().hitTest(viewport.toLogical(x), viewport.toLogical(y));
        return hit != null && hit.owns(node.target());
    }

    private static void collectLayoutMatches(List<UiLayoutNode> nodes, String key, List<UiLayoutNode> matches) {
        for (UiLayoutNode node : nodes) {
            if (key.equals(node.key())) matches.add(node);
            collectLayoutMatches(node.children(), key, matches);
        }
    }
    /** 选出当前页或最上层模态的作用域。 */
    protected abstract UiInputScope inputScope();
    /** 业务路由可共享本根的唯一输入路由器。 */
    protected final UiInputRouter inputRouter() { return input; }
    /** 输入派发已结束，在下一次布局和命中前应用根拥有的待发布状态。 */
    protected void prepareUi() { }
    /** 每个绘制帧只推进一次动画；事件触发的准备和布局不调用。 */
    protected void advanceUiFrame() { }
    /** 内容布局钩子；稳定控件只更新几何，事件与绘制读取同一布局。 */
    protected void layoutUi() { }
    /** 在准备完布局、资源和焦点后绘制内容。 */
    protected abstract void renderUi(GuiGraphics graphics, int x, int y, float delta);
    /** 原版绘制入口统一准备，子类无需记忆调用顺序。 */
    @Override public final void render(@NotNull GuiGraphics graphics, int x, int y, float delta) {
        Language current = Language.getInstance();
        if (resources != current) {
            UiRenderLayers.releaseModels();
            invalidateUiResources();
            resources = current;
        }
        advanceUiFrame();
        prepareUi();
        layoutUi();
        refreshUiState();
        UiViewport viewport = viewport();
        graphics.pose().pushPose();
        graphics.pose().scale((float) viewport.scale(), (float) viewport.scale(), 1);
        try { renderUi(graphics, (int) viewport.toLogical(x), (int) viewport.toLogical(y), delta); }
        finally { graphics.pose().popPose(); }
    }
    /** 结构或覆盖面回调后立即应用宿主的选择，旧事件由路由版本失效。 */
    protected final void refreshUiState() { selectScope(); }
    /** 原版资源重载后使根拥有的资源和文本缓存失效。 */
    protected void invalidateUiResources() { }
    /** removed 时释放根拥有的资源；可重复调用。 */
    protected void releaseUiResources() { }
    private UiInputScope selectScope() {
        UiInputScope scope = inputScope();
        scope.refreshFocus();
        input.select(scope);
        return scope;
    }
    private boolean finish(boolean consumed) {
        prepareUi();
        layoutUi();
        refreshUiState();
        return consumed;
    }
    private UiInputContext pointer(double x, double y, int button) {
        prepareUi();
        layoutUi();
        UiViewport viewport = viewport();
        return UiInputContext.of(selectScope(), viewport.toLogical(x), viewport.toLogical(y), button,
                hasShiftDown(), hasControlDown(), hasAltDown());
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        UiInputContext context = pointer(x, y, button);
        return finish(input.mousePressed(context));
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        UiInputContext context = pointer(x, y, button);
        return finish(input.mouseReleased(context));
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        UiInputContext context = pointer(x, y, button);
        UiViewport viewport = viewport();
        return finish(input.mouseDragged(context, viewport.toLogical(dx), viewport.toLogical(dy)));
    }
    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        UiInputContext context = pointer(x, y, -1);
        return finish(input.mouseScrolled(context, sx, sy));
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        prepareUi();
        layoutUi();
        selectScope();
        return finish(input.keyPressed(key, scan, modifiers));
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        prepareUi();
        layoutUi();
        selectScope();
        return finish(input.keyReleased(key, scan, modifiers));
    }
    @Override public boolean charTyped(char value, int modifiers) {
        prepareUi();
        layoutUi();
        selectScope();
        return finish(input.charTyped(value, modifiers));
    }
    @Override public void removed() {
        input.close();
        try { releaseUiResources(); }
        finally {
            UiRenderLayers.releaseModels();
            resources = null;
            super.removed();
        }
    }
}
