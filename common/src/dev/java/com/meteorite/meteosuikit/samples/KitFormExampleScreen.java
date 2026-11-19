package com.meteorite.meteosuikit.samples;

import com.meteorite.meteosuikit.api.control.UiControl;
import com.meteorite.meteosuikit.api.control.UiWidget;
import com.meteorite.meteosuikit.api.control.UiWidgetRow;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiPointer;
import net.minecraft.client.gui.Font;
import com.meteorite.meteosuikit.api.control.UiFormBody;
import com.meteorite.meteosuikit.api.control.UiTextArea;
import com.meteorite.meteosuikit.api.control.UiTextInput;
import com.meteorite.meteosuikit.api.control.UiThemedScreen;
import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.style.UiControlRole;
import com.meteorite.meteosuikit.api.text.TextMeasurer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import com.meteorite.meteosuikit.api.layout.UiFormLayout;
import com.meteorite.meteosuikit.api.layout.UiLinearLayout;

/** 公共正文能力的独立消费者，验证固定底栏、尾随动作与内部编辑焦点。 */
final class KitFormExampleScreen extends UiThemedScreen {
    private final Screen parent;
    private final UiFormBody body;
    private final UiChildren<UiInputTarget> children = new UiChildren<>();
    private final UiInputScope scope = new UiInputScope(children);
    private final UiControl done = new UiControl();
    private final UiWidgetRow footer = new UiWidgetRow(8, UiWidgetRow.Alignment.CENTER);

    KitFormExampleScreen(Screen parent) {
        super(label("title"));
        this.parent = parent;
        var font = Minecraft.getInstance().font;
        body = new UiFormBody(theme(), TextMeasurer.of(font));
        Description description = new Description(font);
        body.add(Component.empty(), description, description, List.of());
        UiTextInput text = new UiTextInput(theme(), font, label("name"));
        UiControl reset = new UiControl(); reset.setTheme(theme(), UiControlRole.SECONDARY); reset.setFocusable(true);
        reset.configure(font, label("reset"), null, List.of(), () -> text.setValue(""));
        body.add(label("name"), text, new UiFormLayout.Control() {
            @Override public int preferredWidth(int width) { return width; }
            @Override public int measureHeight(int width) { return 20; }
        }, List.of(reset));
        for (int index = 0; index < 5; index++) {
            UiTextArea area = new UiTextArea(theme(), font, label("notes"));
            area.setValue("first line\nsecond line\nthird line\nfourth line\nfifth line\nsixth line");
            body.add(label("notes"), area, 65);
        }
        done.setTheme(theme(), UiControlRole.SECONDARY); done.setFocusable(true);
        done.configure(font, Component.translatable("gui.done"), null, List.of(), this::onClose);
        footer.add(done, UiLinearLayout.Child.remain(0, 100));
        children.addAll(List.of(body, footer));
        scope.setEscape(this::onClose);
        scope.focus().setListener((previous, current) -> body.reveal(current));
    }
    @Override protected void layoutUi() {
        int formWidth = Math.clamp(width - 24, 0, 380);
        body.setBounds((width - formWidth) / 2, 35, formWidth, Math.max(0, height - 75));
        footer.setBounds(0, height - 30, width, 20);
    }
    @Override protected UiInputScope inputScope() { return scope; }
    @Override protected void renderThemedUi(GuiGraphics graphics, int x, int y, float delta) {
        graphics.drawCenteredString(font, title, width / 2, 14, theme().get().surfaces().text());
        var pointer = scope.pointer(x, y).resolve(children);
        body.render(graphics, font, pointer);
        footer.render(graphics, font, pointer);
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    private static final class Description implements UiWidget, UiFormLayout.Control {
        private final TextMeasurer text;
        private List<Component> lines = List.of();
        private UiRect bounds = new UiRect(0, 0, 0, 0);
        private Description(Font font) { text = TextMeasurer.of(font); }
        @Override public int preferredWidth(int width) { return width; }
        @Override public int measureHeight(int width) {
            lines = text.wrap(label("description"), width);
            return lines.size() * (text.lineHeight() + 2);
        }
        @Override public UiRect bounds() { return bounds; }
        @Override public void setBounds(int x, int y, int width, int height) { bounds = new UiRect(x, y, width, height); }
        @Override public void render(GuiGraphics graphics, Font font, UiPointer pointer) {
            int y = bounds.y();
            for (Component line : lines) { graphics.drawString(font, line, bounds.x(), y, 0xFFAAAAAA, false); y += font.lineHeight + 2; }
        }
    }
    private static Component label(String key) {
        return Component.translatable(KitShowcaseContent.UI + "showcase.form." + key);
    }
}
