package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiTextInput;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.input.UiPointer;
import com.meteorite.meteosuikit.api.style.UiMinecraftTheme;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.style.UiThemeContext;
import com.meteorite.meteosuikit.api.style.UiThemes;
import com.meteorite.meteosuikit.api.text.UiTextEditSession;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class TextContract extends ContractSupport {
    static void flatSingleLine() {
        MetricFont font=new MetricFont();RecordingGraphics graphics=new RecordingGraphics();
        UiThemeContext<UiStyleSource> context=new UiThemeContext<>(UiMinecraftTheme.INSTANCE);
        UiTextInput input=new UiTextInput(context::current,font,Component.literal("hint"));input.setBounds(10,10,48,20);
        input.render(graphics,font,UiPointer.at(0,0));check(graphics.texts.size()==1&&!graphics.texts.getFirst().shadow(),"placeholder is flat");
        check(graphics.texts.getFirst().color()==context.current().surfaces().disabledText(),"placeholder uses theme");graphics.reset();
        input.setValue("abcdefghijklmnop");input.moveCursorToStart();input.render(graphics,font,UiPointer.at(0,0));
        check(graphics.texts.getFirst().x()==13&&graphics.texts.stream().noneMatch(RecordingGraphics.Text::shadow),"flat text starts at shared viewport");graphics.reset();
        UiInputScope scope=new UiInputScope(input);UiInputRouter router=new UiInputRouter();router.select(scope);scope.refreshFocus();
        router.mousePressed(at(scope,25,18));router.mouseReleased(at(scope,25,18));input.charTyped('X',0);check(input.value().equals("abXcdefghijklmnop"),"pointer position edits visible character");
        input.keyPressed(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL);check(input.value().equals("abcdefghijklmnop"),"local undo survives renderer");
        input.keyPressed(GLFW.GLFW_KEY_Y,0,GLFW.GLFW_MOD_CONTROL);check(input.value().equals("abXcdefghijklmnop"),"local redo survives renderer");
        input.selectAll();input.charTyped('Q',0);check(input.value().equals("Q"),"selection replacement uses original model");
        input.setValue("a\uD83D\uDE00b").moveCursorToStart();router.mousePressed(at(scope,25,18));router.mouseReleased(at(scope,25,18));input.charTyped('X',0);
        check(input.value().equals("a\uD83D\uDE00Xb"),"pointer preserves surrogate boundary");
        input.setValue("\u05D0\u05D1\u05D2").moveCursorToStart();graphics.reset();input.render(graphics,font,UiPointer.at(0,0));
        check(graphics.texts.getFirst().text().equals("\u05D2\u05D1\u05D0"),"RTL visual order");
        router.mousePressed(at(scope,13,18));router.mouseReleased(at(scope,13,18));input.charTyped('X',0);check(input.value().equals("\u05D0\u05D1\u05D2X"),"RTL left edge seeks logical end");
        input.setValue("abcdefghijklmnop");input.selectAll();graphics.reset();input.render(graphics,font,UiPointer.at(0,0));check(graphics.texts.getFirst().x()<13,"long text scrolls to visible caret");
        input.setBounds(10,10,200,20);graphics.reset();input.render(graphics,font,UiPointer.at(0,0));check(graphics.texts.getFirst().x()==13,"resize reveals full text");
        UiStyleSource recolored=UiThemes.builder(UiMinecraftTheme.INSTANCE).colors(colors->colors.withInput(colors.inputFill(),colors.inputBorder(),0xFF123456)).build();
        context.setTheme(recolored);graphics.reset();input.render(graphics,font,UiPointer.at(0,0));check(graphics.texts.getFirst().color()==0xFF123456&&input.value().equals("abcdefghijklmnop"),"theme updates color without losing draft");
        input.setEnabled(false);graphics.reset();input.render(graphics,font,UiPointer.at(0,0));check(graphics.texts.getFirst().color()==recolored.surfaces().disabledText(),"disabled text follows theme");graphics.reset();router.close();
    }
    static void run() {
        flatSingleLine();
        var mixed = new UiTextEditSession.Snapshot("a\uD83D\uDE00\u05D0", 2, 4);
        check(mixed.cursor() == 1 && mixed.anchor() == 4, "saved UTF-16 selection snaps inside a surrogate pair");
    }
}
