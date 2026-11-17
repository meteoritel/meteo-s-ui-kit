package com.meteorite.meteosuikit.samples;

import java.util.function.Supplier;

import com.meteorite.meteosuikit.api.control.UiControlGroup;
import com.meteorite.meteosuikit.api.control.UiModal;
import com.meteorite.meteosuikit.api.control.UiModalStack;
import com.meteorite.meteosuikit.api.control.UiScreenBridge;
import com.meteorite.meteosuikit.api.geometry.UiViewport;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.layout.UiLayoutSource;
import com.meteorite.meteosuikit.api.render.UiLayer;
import com.meteorite.meteosuikit.api.render.UiLayers;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** 保留原版背包 Menu 与槽位的组合接入；kit 只消费工具栏和覆盖层的输入。 */
public final class KitContainerExample extends AbstractContainerScreen<InventoryMenu> implements UiLayoutSource {
    private final Runnable back;
    private final Supplier<? extends UiStyleSource> theme;
    private final UiInputRouter input = new UiInputRouter();
    private final UiScreenBridge bridge = new UiScreenBridge(input, new UiScreenBridge.Host() {
        @Override public UiInputScope inputScope() { return layers.activeScope(); }
        @Override public UiViewport viewport() { return new UiViewport(1, width, height, new UiRect(0, 0, width, height)); }
        @Override public void prepareUi() { modals.prepareUi(); }
        @Override public void layoutUi() { layoutKit(); layers.setOverlays(modals.layers()); }
        @Override public void advanceUiFrame() { modals.tick(); }
    });
    private final UiControlGroup controls = new UiControlGroup();
    private final UiModalStack modals;
    private final UiInputScope base = new UiInputScope(controls);
    private UiLayers layers;
    private boolean mounted;

    /** 使用玩家实际的 InventoryMenu；打开入口要求玩家当前处于该 Menu。 */
    public KitContainerExample(InventoryMenu menu, Inventory inventory, Runnable back) {
        super(menu, inventory, text("title"));
        this.back = back;
        UiStyleSource selectedTheme = KitSkinSamples.themes().getFirst();
        theme = () -> selectedTheme;
        modals = new UiModalStack(theme);
        base.setEscape(this::onClose);
        modals.setOnScopeChanged(() -> { if (mounted) bridge.refresh(); });
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
        bridge.refresh();
    }
    private void layoutKit() {
        int toolbarY = Math.max(0, topPos - 24);
        for (int i = 0; i < controls.size(); i++) controls.controls().get(i).setBounds(leftPos + i * 88, toolbarY, 84, 20);
        modals.setBounds(0, 0, width, height);
        modals.refreshLayout();
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
    /** 原版绘制槽位，覆盖层活动时只绘制底页静态内容。 */
    @Override public void render(@NotNull GuiGraphics graphics, int x, int y, float delta) {
        bridge.beginFrame(Util.getMillis());
        boolean blocked = overlayActive();
        super.render(graphics, blocked ? -1000 : x, blocked ? -1000 : y, delta);
        if (!blocked) renderTooltip(graphics, x, y);
        layers.render(graphics, font, x, y);
    }
    /** 默认主题绘制真实原版槽位几何，物品与拖放仍由父类处理。 */
    @Override protected void renderBg(@NotNull GuiGraphics graphics, float delta, int x, int y) {
        theme.get().drawInset(graphics, new UiRect(leftPos, topPos, imageWidth, imageHeight));
        for (var slot : menu.slots) theme.get().drawInset(graphics, new UiRect(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18));
    }
    /** {@inheritDoc} */
    @Override public boolean mouseClicked(double x, double y, int button) {
        return bridge.mousePressed(x, y, button) || super.mouseClicked(x, y, button);
    }
    /** {@inheritDoc} */
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return bridge.mouseDragged(x, y, button, dx, dy) || super.mouseDragged(x, y, button, dx, dy);
    }
    /** {@inheritDoc} */
    @Override public boolean mouseReleased(double x, double y, int button) {
        return bridge.mouseReleased(x, y, button) || super.mouseReleased(x, y, button);
    }
    /** {@inheritDoc} */
    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        return bridge.mouseScrolled(x, y, sx, sy) || super.mouseScrolled(x, y, sx, sy);
    }
    /** {@inheritDoc} */
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        return bridge.keyPressed(key, scan, modifiers) || super.keyPressed(key, scan, modifiers);
    }
    /** {@inheritDoc} */
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        return bridge.keyReleased(key, scan, modifiers) || super.keyReleased(key, scan, modifiers);
    }
    /** {@inheritDoc} */
    @Override public boolean charTyped(char value, int modifiers) {
        return bridge.charTyped(value, modifiers) || super.charTyped(value, modifiers);
    }
    /** {@inheritDoc} */
    @Override public List<UiLayoutNode> layoutNodes() { return layers == null ? List.of() : layers.layoutNodes(); }
    /** 卸载结束本根捕获和弹窗；Menu 生命周期仍由原版父类处理。 */
    @Override public void removed() {
        mounted = false;
        bridge.close(); modals.clear();
        super.removed();
    }
    /** {@inheritDoc} */
    @Override public void onClose() { super.onClose(); back.run(); }
    private static Component text(String key) { return Component.translatable(KitShowcaseContent.UI + "container." + key); }
}
