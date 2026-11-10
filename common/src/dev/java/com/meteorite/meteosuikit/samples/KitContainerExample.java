package com.meteorite.meteosuikit.samples;

import com.meteorite.meteosuikit.api.control.UiControlGroup;
import com.meteorite.meteosuikit.api.control.UiModal;
import com.meteorite.meteosuikit.api.control.UiModalStack;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputBridge;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.render.UiLayer;
import com.meteorite.meteosuikit.api.render.UiLayers;
import com.meteorite.meteosuikit.api.render.UiRenderLayers;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** 保留原版背包 Menu 与槽位的组合接入；kit 只消费工具栏和覆盖层的输入。 */
public final class KitContainerExample extends AbstractContainerScreen<InventoryMenu> implements UiLayoutSource {
    private final Runnable back;
    private final UiStyleSource theme;
    private final UiInputRouter input = new UiInputRouter();
    private final UiInputBridge bridge = new UiInputBridge(input);
    private final UiControlGroup controls = new UiControlGroup();
    private final UiModalStack modals;
    private final UiInputScope base = new UiInputScope(controls);
    private UiLayers layers;
    private boolean mounted;

    /** 使用玩家实际的 InventoryMenu；打开入口要求玩家当前处于该 Menu。 */
    public KitContainerExample(InventoryMenu menu, Inventory inventory, Runnable back) {
        super(menu, inventory, text("title"));
        this.back = back;
        theme = KitSkinSamples.themes().getFirst();
        modals = new UiModalStack(theme);
        base.setEscape(this::onClose);
        modals.setOnScopeChanged(this::select);
    }
    /** resize 保持控件和路由器实例，更新几何后重新提交焦点索引。 */
    @Override protected void init() {
        super.init();
        mounted = true;
        controls.setTheme(theme);
        controls.beginUpdate();
        controls.obtain("overlay").configure(font, text("overlay"), null, List.of(), this::openOverlay);
        controls.obtain("grid").configure(font, text("grid.title"), null, List.of(), this::openGrid);
        controls.obtain("back").configure(font, Component.translatable(KitShowcaseContent.UI + "back"), null, List.of(), this::onClose);
        controls.endUpdate();
        layers = new UiLayers(new UiLayer(base, (graphics, renderFont, pointer, depth) -> {
            controls.render(graphics, renderFont, pointer);
            controls.renderTooltip(graphics, renderFont, pointer);
        }, () -> controls.controls().stream().flatMap(control -> control.layoutNodes().stream()).toList()));
        layoutKit();
        select();
    }
    private void layoutKit() {
        modals.prepareUi();
        int toolbarY = Math.max(0, topPos - 24);
        for (int i = 0; i < controls.size(); i++) controls.controls().get(i).setBounds(leftPos + i * 88, toolbarY, 84, 20);
        modals.setBounds(0, 0, width, height);
        modals.refreshLayout();
    }
    private void select() {
        if (!mounted || layers == null) return;
        layers.setOverlays(modals.layers());
        UiInputScope scope = layers.activeScope();
        scope.refreshFocus();
        input.select(scope);
    }
    private void openOverlay() {
        modals.push(UiModal.create(theme, font).title(text("overlay"))
                .contentWidget(new KitTextModelExample(font), 24)
                .cancel(Component.translatable(KitShowcaseContent.UI + "back")));
    }
    private void openGrid() {
        modals.push(UiModal.create(theme, font).title(text("grid.title"))
                .contentWidget(new KitGridExample(theme), KitGridExample.CONTENT_HEIGHT)
                .cancel(Component.translatable(KitShowcaseContent.UI + "back")));
    }
    private boolean overlayActive() { return layers.activeScope() != base; }
    private UiInputContext pointer(double x, double y, int button) {
        layoutKit(); select();
        return UiInputContext.of(layers.activeScope(), x, y, button, hasShiftDown(), hasControlDown(), hasAltDown());
    }
    /** 原版绘制槽位，覆盖层活动时只绘制底页静态内容。 */
    @Override public void render(@NotNull GuiGraphics graphics, int x, int y, float delta) {
        layoutKit(); select(); modals.tick();
        boolean blocked = overlayActive();
        super.render(graphics, blocked ? -1000 : x, blocked ? -1000 : y, delta);
        if (!blocked) renderTooltip(graphics, x, y);
        layers.render(graphics, font, x, y);
    }
    /** 默认主题绘制真实原版槽位几何，物品与拖放仍由父类处理。 */
    @Override protected void renderBg(@NotNull GuiGraphics graphics, float delta, int x, int y) {
        theme.drawInset(graphics, new UiRect(leftPos, topPos, imageWidth, imageHeight));
        for (var slot : menu.slots) theme.drawInset(graphics, new UiRect(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18));
    }
    /** kit 消费或覆盖层阻断的按下，其迟到释放仍由 kit 接住。 */
    @Override public boolean mouseClicked(double x, double y, int button) {
        UiInputContext context = pointer(x, y, button);
        boolean consumed = bridge.mousePressed(context);
        select();
        return consumed || super.mouseClicked(x, y, button);
    }
    /** 捕获拖出视口时仍回到原所有者，不进入原版槽位派发。 */
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        UiInputContext context = pointer(x, y, button);
        boolean consumed = bridge.mouseDragged(context, dx, dy);
        select();
        return consumed || super.mouseDragged(x, y, button, dx, dy);
    }
    /** 原版只接收由它处理按下的释放；切域后的旧 kit 释放不会穿透。 */
    @Override public boolean mouseReleased(double x, double y, int button) {
        UiInputContext context = pointer(x, y, button);
        boolean consumed = bridge.mouseReleased(context);
        select();
        return consumed || super.mouseReleased(x, y, button);
    }
    /** {@inheritDoc} */
    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        UiInputContext context = pointer(x, y, -1);
        boolean consumed = bridge.mouseScrolled(context, sx, sy);
        select();
        return consumed || super.mouseScrolled(x, y, sx, sy);
    }
    /** 输入优先交给 kit；覆盖域拦住丢弃、热键换槽与原版关闭键。 */
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        layoutKit(); select();
        boolean consumed = bridge.keyPressed(key, scan, modifiers);
        select();
        return consumed || super.keyPressed(key, scan, modifiers);
    }
    /** {@inheritDoc} */
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        layoutKit(); select();
        boolean consumed = bridge.keyReleased(key, scan, modifiers);
        select();
        return consumed || super.keyReleased(key, scan, modifiers);
    }
    /** {@inheritDoc} */
    @Override public boolean charTyped(char value, int modifiers) {
        layoutKit(); select();
        boolean consumed = bridge.charTyped(value, modifiers);
        select();
        return consumed || super.charTyped(value, modifiers);
    }
    /** {@inheritDoc} */
    @Override public List<UiLayoutNode> layoutNodes() { return layers == null ? List.of() : layers.layoutNodes(); }
    /** 卸载结束捕获并释放共享工作区；Menu 生命周期仍由原版父类处理。 */
    @Override public void removed() {
        mounted = false;
        bridge.close(); modals.clear();
        UiRenderLayers.releaseModels();
        super.removed();
    }
    /** {@inheritDoc} */
    @Override public void onClose() { super.onClose(); back.run(); }
    private static Component text(String key) { return Component.translatable(KitShowcaseContent.UI + "container." + key); }
}
