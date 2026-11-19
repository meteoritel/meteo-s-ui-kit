package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiControl;
import com.meteorite.meteosuikit.api.control.UiDocumentLightboxContent;
import com.meteorite.meteosuikit.api.control.UiDocumentView;
import com.meteorite.meteosuikit.api.control.UiFormBody;
import com.meteorite.meteosuikit.api.control.UiOverlay;
import com.meteorite.meteosuikit.api.control.UiTextArea;
import com.meteorite.meteosuikit.api.control.UiTextInput;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.geometry.UiTransform;
import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiInputBridge;
import com.meteorite.meteosuikit.api.input.UiInputContainer;
import com.meteorite.meteosuikit.api.input.UiInputContext;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import com.meteorite.meteosuikit.api.input.UiNavigationHistory;
import com.meteorite.meteosuikit.api.layout.UiFormLayout;
import com.meteorite.meteosuikit.api.layout.UiNode;
import com.meteorite.meteosuikit.api.style.UiMinecraftTheme;
import com.meteorite.meteosuikit.api.text.TextMeasurer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class InputContract extends ContractSupport {
    static void input() {
        MetricFont font = new MetricFont(); AtomicInteger runs = new AtomicInteger();
        UiTextInput text = new UiTextInput(font,Component.empty()).setValue("draft");
        text.setBounds(0,0,100,20); UiInputScope textScope=new UiInputScope(text); UiInputRouter router=new UiInputRouter(); router.select(textScope); textScope.refreshFocus(); textScope.focus().focusOn(text);
        textScope.interaction().begin(text,UiInteractionSession.Source.TEXT,0); text.setEnabled(false); check(textScope.interaction().target()==null,"disable ends text selector");
        check(!text.canFocus()&&!text.charTyped('x',0)&&!text.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0),"disabled direct input blocked");
        check(text.value().equals("draft"),"disable preserves draft"); text.setEnabled(true); check(text.canFocus(),"enable restores eligibility");
        UiTextArea area=new UiTextArea(font,Component.empty()).setValue("draft"); area.setBounds(0,0,100,50); area.setEditable(false); area.setSelectableWhenReadOnly(true);
        UiInputScope areaScope=new UiInputScope(area); router.select(areaScope); areaScope.refreshFocus(); areaScope.focus().focusOn(area);
        check(area.canFocus()&&!area.charTyped('x',0),"readonly can select without editing"); area.setEnabled(false);check(!area.canFocus()&&!area.keyPressed(GLFW.GLFW_KEY_A,0,GLFW.GLFW_MOD_CONTROL),"disabled readonly exits input");
        UiDocumentView view=new UiDocumentView(new CountingMeasurer(),()->UiMinecraftTheme.INSTANCE,Component.literal("reader")); view.setBounds(10,10,80,30); view.setContent(List.of(row("action",runs::incrementAndGet),new UiNode.Gap(300)));
        UiInputScope scope=new UiInputScope(view); router.select(scope);scope.refreshFocus();
        router.mousePressed(at(scope,13,13)); router.mouseReleased(at(scope,13,13));check(runs.get()==1,"reader paired activation");
        router.mousePressed(at(scope,13,13)); router.mouseDragged(at(scope,20,13),7,0); router.mouseReleased(at(scope,13,13));check(runs.get()==1,"reader drag cancels action");
        router.mousePressed(at(scope,13,13)); view.setVisible(false);router.mouseReleased(at(scope,13,13));check(runs.get()==1&&scope.interaction().target()==null,"hidden reader cancels session");view.setVisible(true);
        scope.focus().focusOn(view);
        router.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);router.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);
        check(runs.get()==1,"key press waits for release");router.keyReleased(GLFW.GLFW_KEY_ENTER,0,0);check(runs.get()==2,"keyboard paired once without repeat");
        router.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);view.setBounds(10,10,70,30);router.keyReleased(GLFW.GLFW_KEY_ENTER,0,0);check(runs.get()==2,"reflow cancels keyboard press");
        view.setContent(List.of(row("plain text",null),new UiNode.Gap(300)));scope.focus().focusOn(view);
        check(view.keyPressed(GLFW.GLFW_KEY_END,0,0)&&view.offset()>0,"no-action End scrolls");view.keyPressed(GLFW.GLFW_KEY_HOME,0,0);check(view.offset()==0,"no-action Home scrolls");
        UiDocumentLightboxContent lightbox=new UiDocumentLightboxContent(new CountingMeasurer(),()->UiMinecraftTheme.INSTANCE,Component.literal("lightbox"));lightbox.setContent(List.of(row("action",runs::incrementAndGet),new UiNode.Gap(300)));lightbox.setViewport(new UiRect(0,0,80,60));
        UiInputScope lightScope=new UiInputScope(lightbox);router.select(lightScope);lightScope.refreshFocus();lightbox.zoomBy(2);int zoom=lightbox.zoomPercent();lightbox.setViewport(new UiRect(0,0,100,65));check(lightbox.zoomPercent()==zoom,"resize preserves manual zoom");
        lightbox.setContent(List.of(row("small",runs::incrementAndGet)));lightbox.fit();check(lightbox.zoomPercent()<=100,"fit never enlarges short text");
        router.mousePressed(at(lightScope,3,3));lightbox.setEnabled(false);router.mouseReleased(at(lightScope,3,3));check(runs.get()==2,"disabled lightbox cancels action");
        lightbox.setEnabled(true);router.mousePressed(at(lightScope,3,3));lightbox.setPanLocked(true);router.mouseDragged(at(lightScope,20,3),17,0);router.mouseReleased(at(lightScope,20,3));check(runs.get()==2,"locked lightbox drag cancels content action");
        view.setContent(List.of(new UiNode.Frame(new UiNode.FrameSpec(40,5,-1,new UiTransform()),List.of(row("framed long action text",runs::incrementAndGet)))));
        view.setOffset(0);
        var frameNode=view.layoutNodes().getFirst().children().getFirst();
        check(frameNode.children().getFirst().visibleRect().bottom()<=frameNode.rect().bottom(),"inline snapshot retains nested Frame clipping");
        AtomicInteger viewportActivations=new AtomicInteger();
        UiDocumentLightboxContent stableBox=new UiDocumentLightboxContent(new CountingMeasurer(),()->UiMinecraftTheme.INSTANCE,Component.literal("viewport"));
        stableBox.setContent(List.of(row("action",viewportActivations::incrementAndGet)));
        UiRect viewport=new UiRect(0,0,80,40);stableBox.setViewport(viewport);
        UiInputScope stableScope=new UiInputScope(stableBox);router.select(stableScope);stableScope.refreshFocus();
        router.mousePressed(at(stableScope,3,3));stableBox.setViewport(viewport);router.mouseReleased(at(stableScope,3,3));
        check(viewportActivations.get()==1,"identical viewport preserves active press");
        stableBox.zoomBy(1);router.mousePressed(at(stableScope,3,3));stableBox.setViewport(new UiRect(0,0,80,41));router.mouseReleased(at(stableScope,3,3));
        check(viewportActivations.get()==1,"height-only resize cancels manual-zoom action press");
        router.close();
    }
    static void routing() {
        UiInputRouter router = new UiInputRouter();
        AtomicInteger middleExits = new AtomicInteger();
        UiInputScope last = new UiInputScope(new UiInputTarget() {});
        UiInputScope middle = new UiInputScope(new UiInputTarget() {
            @Override public void scopeExited(UiInteractionSession.EndReason reason) { middleExits.incrementAndGet(); }
        });
        UiInputScope first = new UiInputScope(new UiInputTarget() {
            @Override public void scopeExited(UiInteractionSession.EndReason reason) { router.select(last); }
        });
        router.select(first); router.select(middle);
        check(router.active()==last && last.isActive() && !middle.isActive() && !first.isActive(), "exit callback chooses only final scope");
        check(middleExits.get()==0, "unactivated intermediate scope has no exit callback");
        router.close();
        UiInputScope closing = new UiInputScope(new UiInputTarget() {
            @Override public void scopeExited(UiInteractionSession.EndReason reason) { router.close(); }
        });
        router.select(closing); router.select(middle);
        check(router.active()==null && !middle.isActive(), "exit callback close prevents stale activation");
        UiControl button = new UiControl(); button.setBounds(0,0,20,20);
        AtomicInteger activations = new AtomicInteger(); button.configure(new MetricFont(), Component.literal("button"), null, List.of(), activations::incrementAndGet);
        UiInputScope buttons = new UiInputScope(button); router.select(buttons); buttons.refreshFocus();
        router.mousePressed(at(buttons,3,3)); router.select(buttons); router.mouseReleased(at(buttons,3,3)); router.mouseReleased(at(buttons,3,3));
        check(activations.get()==1, "same scope preserves press and duplicate release is ignored");
        router.mousePressed(at(buttons,3,3)); router.close(); router.select(buttons); router.mouseReleased(at(buttons,3,3));
        check(activations.get()==1, "close cancels old release"); router.close();
    }
    static void memberTree() {
        UiTextInput lower = new UiTextInput(new MetricFont(),Component.literal("lower"));
        UiTextInput upper = new UiTextInput(new MetricFont(),Component.literal("upper"));
        lower.setBounds(0,0,100,20);upper.setBounds(0,0,100,20);
        lower.setLayoutKey("lower");upper.setLayoutKey("upper");
        UiChildren<UiInputTarget> children = new UiChildren<>();children.addAll(List.of(lower,upper));
        children.setTabOrder(upper,-1);
        UiInputContainer root = () -> children;
        check(root.hitTest(5,5).owns(upper),"last drawn member wins hit");
        UiInputScope scope = new UiInputScope(root); UiInputRouter router = new UiInputRouter();
        router.select(scope);scope.refreshFocus();
        check(scope.focus().targets().getFirst()==upper,"tab order is independent of occlusion");
        scope.focus().focusOn(lower);router.charTyped('a',0);
        check(lower.value().equals("a")&&upper.value().isEmpty(),"characters follow only focused member");
        scope.interaction().begin(lower,UiInteractionSession.Source.TEXT,0);children.remove(lower);scope.refreshFocus();
        check(scope.interaction().target()==null&&!lower.hasFocus(),"removed focused subtree loses active input");
        check(root.layoutNodes().size()==1&&root.layoutNodes().getFirst().target()==upper,"layout follows actual membership");
        UiOverlay overlay = new UiOverlay(root);
        router.select(overlay.inputScope()); overlay.inputScope().refreshFocus();
        check(overlay.layoutNodes().getFirst().target()==upper && overlay.hitTest(5,5).owns(upper),"overlay layout and hit share actual leaf");
        check(overlay.inputScope().focus().targets().contains(upper),"overlay focus uses same content");
        children.clear(); check(overlay.layoutNodes().isEmpty(),"overlay layout tracks removed content");
        check(new UiOverlay(new UiInputTarget() {}).layoutNodes().isEmpty(),"input-only overlay has empty layout"); router.close();
    }
    static void formBody() {
        MetricFont font = new MetricFont();
        UiFormBody body = new UiFormBody(() -> UiMinecraftTheme.INSTANCE, TextMeasurer.of(font));
        UiTextInput first = new UiTextInput(font, Component.literal("first")).setValue("draft");
        UiTextArea last = new UiTextArea(font, Component.literal("last")).setValue("notes");
        first.setLayoutKey("first"); last.setLayoutKey("last");
        AtomicInteger measures = new AtomicInteger();
        UiFormLayout.Control measure = new UiFormLayout.Control() {
            public int preferredWidth(int width) { return width; }
            public int measureHeight(int width) { measures.incrementAndGet(); return 20; }
        };
        UiControl action = new UiControl(); action.setFocusable(true);
        action.configure(font,Component.literal("action with long label"),null,List.of(),()->{});
        body.add(Component.literal("First"),first,measure,List.of(action));
        body.add(Component.literal("Last"),last,90); body.setBounds(10,20,130,65);
        int measured=measures.get(); int fieldWidth=first.bounds().width();
        UiInputScope scope = new UiInputScope(body); UiInputRouter router = new UiInputRouter();
        scope.focus().setListener((previous,current)->body.reveal(current));
        router.select(scope);scope.refreshFocus();body.advanceFrame(0);
        int fieldY=first.bounds().y();
        check(router.mouseScrolled(at(scope,11,22),0,-1),"form accepts wheel target");
        body.setBounds(10,20,130,65);body.layoutNodes();body.advanceFrame(16);
        check(body.offset()>0&&first.bounds().y()==fieldY-body.offset(),"form animation projects real field coordinates");
        check(body.layoutNodes().stream().anyMatch(node->node.target()==first&&node.rect().equals(first.bounds())),"form snapshot uses animated field geometry");
        body.setOffset(0);scope.focus().focusOn(last);
        check(body.offset()>0 && last.bounds().y()<body.bounds().bottom(),"focus reveals offscreen multiline field");
        body.setBounds(10,20,130,65);body.layoutNodes();body.setOffset(0);
        check(measures.get()==measured,"same geometry and scrolling reuse measurement");
        first.setEnabled(false);body.layoutNodes();check(first.bounds().width()==fieldWidth,"disabled field keeps layout");first.setEnabled(true);
        first.setVisible(false);scope.refreshFocus();body.layoutNodes();
        check(scope.focus().targets().stream().noneMatch(target->target==first||target==action),"hidden row removes its actions from focus");
        first.setVisible(true);body.setOffset(0);scope.refreshFocus();scope.focus().focusOn(first);
        scope.interaction().begin(first,UiInteractionSession.Source.TEXT,0);
        body.remove(first);scope.refreshFocus();check(scope.interaction().target()==null && !first.hasFocus(),"removed editor cancels session");
        check(!body.charTyped('x',0)&&first.value().equals("draft"),"removed editor receives no character");
        body.setBounds(0,0,0,0);check(body.layoutNodes().stream().allMatch(node->node.visibleRect().width()==0),"zero width is clipped");
        body.remove(last);check(body.offset()==0,"shortened body clamps offset");router.close();
    }
    static void reentrantFocus() {
        UiInputRouter router = new UiInputRouter();
        UiInputScope[] returning = new UiInputScope[1];
        UiControl button = new UiControl(); button.setFocusable(true);
        boolean[] returnOnce = {true};
        UiChildren<UiInputTarget> members = new UiChildren<>(); members.add(button);
        returning[0] = new UiInputScope(new UiInputContainer() {
            @Override public UiChildren<UiInputTarget> inputChildren() { return members; }
            @Override public void scopeExited(UiInteractionSession.EndReason reason) {
                if (returnOnce[0]) { returnOnce[0] = false; router.select(returning[0]); }
            }
        });
        router.select(returning[0]); returning[0].refreshFocus(); returning[0].focus().focusOn(button);
        router.select(null);
        check(router.active()==returning[0] && returning[0].isActive() && returning[0].focus().focused()==button,
                "reselected scope resumes focus after exit callback");
        router.close();
    }
    static void navigation() {
        UiInputRouter router = new UiInputRouter();
        AtomicInteger backs = new AtomicInteger(), presses = new AtomicInteger(), releases = new AtomicInteger();
        boolean[] consumesBackspace = {false};
        UiInputScope base = new UiInputScope(new UiInputTarget() {
            @Override public boolean mousePressed(UiInputContext context) { presses.incrementAndGet(); return true; }
            @Override public boolean mouseReleased(UiInputContext context) { releases.incrementAndGet(); return true; }
            @Override public boolean keyPressed(int key, int scan, int modifiers) {
                return consumesBackspace[0] && key == GLFW.GLFW_KEY_BACKSPACE;
            }
        });
        router.select(base);
        router.mousePressed(backAt(base)); router.mouseReleased(backAt(base));
        check(presses.get()==1 && releases.get()==1, "unconfigured scope preserves side-button content input");
        base.setNavigationBack(backs::incrementAndGet);
        check(router.mousePressed(backAt(base)), "configured back press is consumed");
        check(router.mousePressed(backAt(base)) && backs.get()==1, "duplicate press waits for paired release");
        check(router.mouseDragged(backAt(base),4,4) && router.mouseReleased(backAt(base)), "back drag and release are consumed");
        router.mouseReleased(backAt(base));
        check(presses.get()==1 && releases.get()==1 && backs.get()==1, "back never reaches content or activates on release");
        UiInputContext forward = UiInputContext.pointer(base,3,3,GLFW.GLFW_MOUSE_BUTTON_5,0);
        router.mousePressed(forward);router.mouseReleased(forward);
        check(presses.get()==2 && releases.get()==2 && backs.get()==1, "forward button keeps ordinary dispatch");
        consumesBackspace[0]=true;router.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0);
        check(backs.get()==1, "content consumes Backspace before navigation");
        consumesBackspace[0]=false;router.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0);
        check(backs.get()==2, "unconsumed Backspace shares navigation callback");
        UiInputScope modal = new UiInputScope(new UiInputTarget() {
            @Override public boolean blocksInput() { return true; }
        });
        base.setNavigationBack(() -> { backs.incrementAndGet();router.select(modal); });
        router.mousePressed(backAt(base));
        check(router.active()==modal && router.mouseDragged(backAt(modal),4,4) && router.mouseReleased(backAt(modal)),
                "back release stays claimed when callback changes scope");
        router.mousePressed(backAt(modal));router.mouseReleased(backAt(modal));router.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0);
        check(backs.get()==3, "modal without callback never searches underlying navigation");
        router.select(base);base.setNavigationBack(backs::incrementAndGet);
        UiInteractionSession.EndReason[] ended = {null};
        UiInteractionSession.Target changing = reason -> { ended[0]=reason;router.select(modal); };
        base.interaction().begin(changing,UiInteractionSession.Source.POINTER,0);
        router.mousePressed(backAt(base));
        check(ended[0]==UiInteractionSession.EndReason.CANCEL && backs.get()==3 && router.active()==modal,
                "cancellation changing scope skips stale navigation callback");
        router.mouseReleased(backAt(modal));router.select(base);
        base.interaction().begin(reason -> router.close(),UiInteractionSession.Source.KEY,GLFW.GLFW_KEY_ENTER);
        router.mousePressed(backAt(base));
        check(router.active()==null && backs.get()==3 && !router.mouseReleased(backAt(base)),
                "cancellation closing root clears back pairing without invoking stale callback");
        router.select(base);base.setNavigationBack(router::close);
        check(router.mousePressed(backAt(base)) && router.active()==null, "callback can close root while press stays consumed");
        router.select(base);
        check(!router.mouseReleased(backAt(base)), "root close discards old back release");
        base.setNavigationBack(null);router.mousePressed(backAt(base));router.mouseReleased(backAt(base));
        check(presses.get()==3 && releases.get()==3, "callback can be removed");
        router.close();

        UiControl button = new UiControl();button.setBounds(0,0,20,20);
        AtomicInteger actions = new AtomicInteger();
        button.configure(new MetricFont(),Component.literal("action"),null,List.of(),actions::incrementAndGet);
        UiInputScope buttons = new UiInputScope(button);buttons.setNavigationBack(backs::incrementAndGet);
        router.select(buttons);buttons.refreshFocus();
        router.mousePressed(at(buttons,3,3));router.mousePressed(backAt(buttons));
        router.mouseReleased(at(buttons,3,3));router.mouseReleased(backAt(buttons));
        check(actions.get()==0 && buttons.interaction().target()==null, "back cancels held primary button and late activation");
        router.close();

        UiInputBridge bridge = new UiInputBridge(router);router.select(buttons);buttons.refreshFocus();
        buttons.setNavigationBack(() -> router.select(modal));
        bridge.mousePressed(at(buttons,3,3));bridge.mousePressed(backAt(buttons));
        check(bridge.mouseReleased(at(modal,3,3)) && bridge.mouseReleased(backAt(modal)) && actions.get()==0,
                "container bridge consumes late primary and back releases after navigation changes scope");
        bridge.close();

        UiDocumentView reader = new UiDocumentView(new CountingMeasurer(),()->UiMinecraftTheme.INSTANCE,Component.literal("reader"));
        reader.setBounds(0,0,80,40);reader.setContent(List.of(row("action",actions::incrementAndGet)));
        UiInputScope reading = new UiInputScope(reader);reading.setNavigationBack(backs::incrementAndGet);
        router.select(reading);reading.refreshFocus();reading.focus().focusOn(reader);
        router.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);router.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0);router.keyReleased(GLFW.GLFW_KEY_ENTER,0,0);
        check(actions.get()==0 && reading.interaction().target()==null, "Backspace cancels pending keyboard document activation");
        router.close();

        UiTextInput text = new UiTextInput(new MetricFont(),Component.literal("draft"));text.setBounds(0,0,100,20);
        UiInputScope editing = new UiInputScope(text);editing.setNavigationBack(backs::incrementAndGet);
        router.select(editing);editing.refreshFocus();editing.focus().focusOn(text);
        router.charTyped('x',0);int beforeUndo=backs.get();
        router.keyPressed(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL);
        check(text.value().isEmpty() && backs.get()==beforeUndo, "text undo remains separate from page back");
        router.keyPressed(GLFW.GLFW_KEY_Y,0,GLFW.GLFW_MOD_CONTROL);
        check(text.value().equals("x") && backs.get()==beforeUndo, "text redo remains local");
        router.mousePressed(backAt(editing));router.mouseReleased(backAt(editing));
        check(text.value().equals("x") && backs.get()==beforeUndo+1, "side button navigates without undoing text");
        router.close();
    }
    static void modalPointerOwnership() {
        UiInputRouter router = new UiInputRouter();
        UiInputBridge bridge = new UiInputBridge(router);
        UiInputScope base = new UiInputScope(new UiInputTarget() {});
        UiInputScope modal = new UiOverlay(new UiInputTarget() {}).inputScope();
        router.select(modal);
        UiInputContext original = at(modal, 3, 3);
        router.select(base);
        check(bridge.mousePressed(original), "a press admitted by a modal stays consumed after preparation closes it");
        UiInputContext lower = at(base, 3, 3);
        check(bridge.mouseDragged(lower, 2, 0), "late modal drag stays consumed");
        check(bridge.mouseReleased(lower), "late modal release stays consumed");
        check(!bridge.mouseReleased(lower) && !bridge.mousePressed(lower), "ordinary input can fall back again");
        check(bridge.mouseScrolled(original, 0, 1), "a modal scroll cannot fall back after a scope change");
        bridge.close();
    }

    static void navigationLifecycle() {
        ClosingScreen screen = new ClosingScreen();
        check(screen.keyPressed(GLFW.GLFW_KEY_BACKSPACE,0,0), "screen consumes empty-history close action");
        check(!screen.hasActiveScope() && !screen.scope.isActive() && screen.preparations==1,
                "event completion does not prepare or reactivate removed screen");
        screen.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);
        check(!screen.hasActiveScope() && !screen.scope.isActive(), "reused screen closes in its new lifecycle");
    }
    static void navigationHistory() {
        UiNavigationHistory<Integer> history = new UiNavigationHistory<>(2);
        List<Integer> restored = new ArrayList<>();
        history.push(1);history.push(1);history.push(2);history.push(3);
        check(history.back(value -> value<3, value -> { restored.add(value);history.push(9); }), "back skips invalid newest entry");
        check(restored.equals(List.of(2)) && !history.back(value -> true,restored::add), "bounded history and restore-time recording suppression");
        history.push(4);history.push(5);
        check(!history.canGoBack(value -> value<4) && !history.back(value -> true,restored::add), "invalid history is exhausted");
        history.push(6);history.clear();check(!history.canGoBack(value -> true), "clear removes navigation history");
    }
    static void run() { input();routing();memberTree();formBody();reentrantFocus();navigation();modalPointerOwnership();navigationLifecycle();navigationHistory(); }
}
