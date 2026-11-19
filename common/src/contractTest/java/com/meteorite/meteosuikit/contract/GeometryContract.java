package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiButtonWidget;
import com.meteorite.meteosuikit.api.control.UiControl;
import com.meteorite.meteosuikit.api.control.UiWidgetRow;
import com.meteorite.meteosuikit.api.control.UiListView;
import com.meteorite.meteosuikit.api.control.UiModal;
import com.meteorite.meteosuikit.api.control.UiModalStack;
import com.meteorite.meteosuikit.api.geometry.UiTransform;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiAction;
import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.layout.UiDocument;
import com.meteorite.meteosuikit.api.layout.UiGridMetrics;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.layout.UiLinearLayout;
import com.meteorite.meteosuikit.api.layout.UiNode;
import com.meteorite.meteosuikit.api.render.UiIcon;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

final class GeometryContract extends ContractSupport {
    static void layout() {
        CountingMeasurer measurer = new CountingMeasurer();
        Component styled = Component.literal("abcdefghij").withStyle(Style.EMPTY.withBold(true).withColor(0x123456));
        var wrapped = measurer.wrap(styled,18);
        check(wrapped.size()>1,"wrap long text");
        for (Component line:wrapped) line.visit((style,text)-> { check(style.isBold()&&style.getColor().getValue()==0x123456,"styles preserved");return Optional.empty();},Style.EMPTY);
        AtomicInteger runs = new AtomicInteger(); UiAction action = runs::incrementAndGet;
        UiNode.InlineIcon icon = new UiNode.InlineIcon(new UiIcon.Rendered(8,8,(g,r)->{}),List.of(),"icon",action);
        UiDocument document = new UiDocument(measurer,new UiTransform(),-1,false);
        document.setTextOverflow(UiDocument.TextOverflow.WRAP); document.setViewport(0,0,75,800);
        document.setContent(List.of(new UiNode.Row(0,-1,icon,styled,-1,List.of(icon),List.of(),"parent",action),
                new UiNode.Row(12,0,Component.literal("child with long words"),-1,List.of(),List.of(),"child",action),
                new UiNode.Frame(new UiNode.FrameSpec(40,15,-1,new UiTransform()),List.of(row("frame row",action)))));
        var targets = document.targets();
        check(targets.stream().filter(t->Objects.equals(t.hit().payload(),"parent")).count()==1,"one row action per logical occurrence");
        check(targets.stream().filter(t->Objects.equals(t.hit().payload(),"icon")).count()==2,"icons appear once each");
        check(targets.stream().filter(t->t.hit().action()==action).count()==5,"same callback keeps separate occurrences including frame");
        check(targets.getLast().contentClip()!=null && targets.getFirst().contentClip()==null,"Frame targets carry ancestor clip");
        var parent = targets.getFirst(); int measured = measurer.wraps;
        document.setViewport(8,9,75,900); document.transform().zoomBy(-1,8,9); document.targets(); document.layoutNodes();
        check(measurer.wraps==measured,"pan/zoom/height reuse layout");
        UiDocument hitDocument = new UiDocument(measurer,false); hitDocument.setTextOverflow(UiDocument.TextOverflow.WRAP); hitDocument.setViewport(0,0,40,200);
        hitDocument.setContent(List.of(row("abcdefghi",action))); var target = hitDocument.targets().getFirst();
        check(hitDocument.hit(3,16)==target.hit(),"continuation keeps logical hit identity");
        check(hitDocument.hit(35,16)==null,"continuation bounding whitespace does not hit");
        UiDocument single = new UiDocument(measurer,false); single.setViewport(0,0,40,100); single.setContent(List.of(row("very long single line",action)));
        check(single.layoutNodes().getFirst().clipped(),"hover mode keeps natural width diagnostic");
        single.setTextOverflow(UiDocument.TextOverflow.WRAP); single.setViewport(0,0,1,1000); check(single.contentHeight()>9,"narrow layout advances");
    }
    static void modalViewport() {
        UiModalStack stack = new UiModalStack(); stack.setBounds(0,0,140,90);
        for (int depth=0; depth<3; depth++) {
            UiModal modal = UiModal.create(new MetricFont()).preferredWidth(340)
                    .content((graphics,font,rect,pointer) -> {}, 180)
                    .confirm(Component.literal("Done"), () -> {}).cancel(Component.literal("Cancel"));
            stack.push(modal);
            check(modal.bounds().x()>=0 && modal.bounds().right()<=140 && modal.bounds().bottom()<=90,"stacked modal fits viewport");
            for (UiButtonWidget button : modal.buttons())
                check(button.bounds().x()>=0 && button.bounds().right()<=140 && button.bounds().bottom()<=90,"modal footer stays reachable");
        }
        stack.setBounds(0,0,100,80);
        check(stack.modals().stream().allMatch(modal -> modal.bounds().right()<=100 && modal.bounds().bottom()<=80),"resize reclamps every modal");
        stack.clear();
    }
    static void listGeometry() {
        UiListView<String> list = new UiListView<>((g,f,item,index,rect,selected,hovered,focused) -> {});
        list.setLayoutKey("list"); list.setItemKey(item -> "item/" + item);
        list.setRowHeight(20).setItems(List.of("a","b","c","d")); list.setChipRows(true);
        list.setBounds(10,20,100,35);
        UiChildren<UiInputTarget> root = new UiChildren<>(); root.add(list);
        UiLayoutNode node = root.layoutNodes().getFirst();
        check(node.kind()==UiLayoutNode.Kind.LIST && node.target()==list && node.key().equals("list"),"list is an actual layout member");
        check(node.children().getFirst().rect().equals(list.rowBounds(0)),"snapshot uses rendered row");
        check(list.itemIndexAt(12,38)==-1,"chip gap is excluded");
        check(list.itemIndexAt(108,25)==-1,"scrollbar is excluded");
        list.setSelectedIndex(3); int selection=list.selectedIndex();
        var rows=list.layoutNodes().getFirst().children();
        check(list.selectedIndex()==selection && rows.getLast().key().equals("item/d"),"query preserves selection and key follows item");
        check(rows.stream().allMatch(row -> row.visibleRect().bottom()<=54 && row.visibleRect().y()>=21),"rows clipped to viewport");
        list.setEnabled(false);check(!list.layoutNodes().getFirst().enabled()&&list.hitTest(12,25)==null,"disabled layout remains diagnostic");
        list.setVisible(false);check(list.layoutNodes().isEmpty(),"hidden list absent");
        list.setVisible(true);list.setBounds(0,0,0,0);check(list.layoutNodes().getFirst().children().isEmpty(),"zero-sized list has no visible rows");
        list.setItems(List.of());check(list.layoutNodes().getFirst().children().isEmpty(),"empty list keeps root");
    }
    private static void widgetRow() {
        UiControl first = new UiControl();
        UiControl second = new UiControl();
        first.setLayoutKey("first");
        second.setLayoutKey("second");
        first.setFocusable(true);
        second.setFocusable(true);
        MetricFont font = new MetricFont();
        first.configure(font, Component.literal("first"), null, List.of(), () -> { });
        second.configure(font, Component.literal("second"), null, List.of(), () -> { });
        UiWidgetRow footer = new UiWidgetRow(4, UiWidgetRow.Alignment.CENTER);
        footer.add(first, UiLinearLayout.Child.remain(0, 30));
        footer.add(second, UiLinearLayout.Child.remain(0, 30));
        footer.setBounds(10, 5, 101, 20);
        check(first.bounds().equals(new UiRect(28, 5, 30, 20)), "limited footer centers its actual members");
        check(footer.hitTest(65, 8).owns(second), "row selects the same member it places");
        check(footer.layoutNodes().getFirst().rect().equals(first.bounds()), "row snapshot uses placed bounds");
        UiInputScope scope = new UiInputScope(footer);
        new UiInputRouter().select(scope);
        scope.refreshFocus();
        check(scope.focus().focusOn(second), "row registers actual focus target");
        footer.setBounds(10, 5, 57, 20);
        scope.refreshFocus();
        check(first.bounds().width() == 26 && second.isFocused(), "odd width preserves focus and shared allocation");
        first.setVisible(false);
        footer.setBounds(10, 5, 57, 20);
        check(second.bounds().equals(new UiRect(23, 5, 30, 20)), "hidden member releases row space");
        UiWidgetRow search = new UiWidgetRow(4, UiWidgetRow.Alignment.START);
        first.setVisible(true);
        search.add(first, UiLinearLayout.Child.remain());
        search.add(second, UiLinearLayout.Child.fixed(24));
        search.setBounds(0, 0, 80, 20);
        check(first.bounds().width() == 52 && second.bounds().x() == 56, "fixed button leaves remainder to search");
        search.setBounds(0, 0, 0, 0);
        check(first.bounds().width() == 0 && search.hitTest(4, 0) == null, "empty row has no hit");
    }

    private static void visibleGridCells() {
        UiGridMetrics grid = new UiGridMetrics(2, 15, 10, 3, 4);
        check(grid.visibleCells(9, 6, 5).equals(new UiGridMetrics.VisibleCells(0, 4)),
                "viewport intersects part of two rows");
        check(grid.visibleCells(10, 4, 5).equals(new UiGridMetrics.VisibleCells(2, 2)),
                "viewport confined to the gap has no visible cells");
        check(grid.visibleCells(27, 10, 5).equals(new UiGridMetrics.VisibleCells(4, 5)),
                "clipped final row excludes its empty cell");
        check(grid.visibleCells(14, 0, 5).equals(new UiGridMetrics.VisibleCells(2, 2)),
                "zero-height viewport has no visible cells");
        check(grid.indexAt(16, 15, 5) == -1 && grid.indexAt(18, 15, 5) == 3,
                "hit and display use the same row and column geometry");
    }

    static void run() { layout(); listGeometry(); modalViewport(); widgetRow(); visibleGridCells(); }
}
