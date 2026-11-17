package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiScreen;
import com.meteorite.meteosuikit.api.control.UiScrollView;
import com.meteorite.meteosuikit.api.input.UiAction;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.layout.UiNode;
import com.meteorite.meteosuikit.api.text.TextMeasurer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

abstract class ContractSupport {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static final class MetricFont extends Font {
        private final StringSplitter metric = new StringSplitter((code, style) -> 6);
        MetricFont() { super(id -> null, false); }
        @Override public StringSplitter getSplitter() { return metric; }
        @Override public List<FormattedCharSequence> split(FormattedText text, int width) {
            return metric.splitLines(text, width, Style.EMPTY).stream()
                    .map(line -> FormattedCharSequence.forward(line.getString(), Style.EMPTY)).toList();
        }
        @Override public int width(String text) { return text.codePointCount(0, text.length()) * 6; }
        @Override public int width(FormattedText text) { return width(text.getString()); }
        @Override public int width(FormattedCharSequence text) { int[] size = {0}; text.accept((i,s,c) -> {size[0]++; return true;}); return size[0]*6; }
        @Override public String plainSubstrByWidth(String text, int width) { return text.substring(0, text.offsetByCodePoints(0, Math.min(text.codePointCount(0,text.length()), Math.max(0,width/6)))); }
        @Override public String plainSubstrByWidth(String text, int width, boolean reverse) {
            return reverse ? text.substring(text.offsetByCodePoints(text.length(), -Math.min(text.codePointCount(0,text.length()), Math.max(0,width/6)))) : plainSubstrByWidth(text,width);
        }
    }
    static final class CountingMeasurer implements TextMeasurer {
        final TextMeasurer delegate = TextMeasurer.of(new MetricFont());
        int wraps;
        public int width(Component text) { return delegate.width(text); }
        public int lineHeight() { return 9; }
        public List<Component> wrap(Component text,int width) { wraps++; return delegate.wrap(text,width); }
    }
    static UiNode.Row row(String text, UiAction action) { return new UiNode.Row(0,-1,Component.literal(text),-1,List.of(),List.of(),text,action); }
    static UiInputContext at(UiInputScope scope,double x,double y) { return UiInputContext.pointer(scope,x,y,0,0); }
    static UiInputContext backAt(UiInputScope scope) {
        return UiInputContext.pointer(scope, 3, 3, GLFW.GLFW_MOUSE_BUTTON_4, 0);
    }
    static final class ClosingScreen extends UiScreen {
        final UiInputScope scope = new UiInputScope(new UiInputTarget() {});
        int preparations;
        ClosingScreen() { super(Component.empty());scope.setNavigationBack(this::removed);scope.setEscape(this::removed); }
        @Override protected UiInputScope inputScope() { return scope; }
        @Override protected void prepareUi() { preparations++; }
        @Override protected void renderUi(GuiGraphics graphics,int x,int y,float delta) { }
        boolean hasActiveScope() { return inputRouter().active()!=null; }
    }
    static UiScrollView scrollView() {
        UiScrollView view=new UiScrollView();view.setViewport(0,0,100,40);view.setContentHeight(400);view.setScrollbarVisible(true);return view;
    }
    static void settle(UiInputTarget target,long from) { for(long time=from;time<=from+1000;time+=16) target.advanceFrame(time); }
    static Minecraft headlessMinecraft() {
        // 绘制记录器无需窗口和 GPU，跳过原版客户端构造，只供 GuiGraphics 读取空 sprite 槽。
        try {
            Class<?> unsafe=Class.forName("sun.misc.Unsafe");Field field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
            Method allocate=unsafe.getMethod("allocateInstance",Class.class);return (Minecraft)allocate.invoke(field.get(null),Minecraft.class);
        } catch(ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    static final class RecordingGraphics extends GuiGraphics {
        record Text(String text,int x,int y,int color,boolean shadow) {}
        final List<Text> texts=new ArrayList<>();
        final List<Float> fillDepths=new ArrayList<>();
        final List<Float> textDepths=new ArrayList<>();int clips;
        RecordingGraphics() { super(headlessMinecraft(),null); }
        @Override public void flush() {}
        @Override public void enableScissor(int left,int top,int right,int bottom) { clips++; }
        @Override public void disableScissor() { clips--; }
        @Override public int guiWidth() { return 320; }
        @Override public int guiHeight() { return 240; }
        @Override public void fill(int left,int top,int right,int bottom,int color) { fillDepths.add(pose().last().pose().m32()); }
        @Override public int drawString(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow) {
            StringBuilder value=new StringBuilder();text.accept((index,style,code)->{value.appendCodePoint(code);return true;});
            textDepths.add(pose().last().pose().m32());texts.add(new Text(value.toString(),x,y,color,shadow));return x+font.width(text);
        }
        void reset() { texts.clear();fillDepths.clear();textDepths.clear();check(clips==0,"clip stack restored"); }
    }
}
