package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiListView;
import com.meteorite.meteosuikit.api.control.UiModal;
import com.meteorite.meteosuikit.api.control.UiOverlay;
import com.meteorite.meteosuikit.api.control.UiScrollView;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import com.meteorite.meteosuikit.api.input.UiChildren;
import com.meteorite.meteosuikit.api.input.UiInputRouter;
import com.meteorite.meteosuikit.api.input.UiInputScope;
import com.meteorite.meteosuikit.api.input.UiInputTarget;
import com.meteorite.meteosuikit.api.input.UiInteractionSession;
import java.util.List;
import net.minecraft.network.chat.Component;

final class ScrollContract extends ContractSupport {
    static void scrollMotion() {
        UiScrollView view=scrollView();view.advanceFrame(0);
        check(view.scrollByPixels(-100)&&view.offset()==0,"wheel accepts target before visible motion");
        view.advanceFrame(16);int first=view.offset();check(first>0&&first<100,"damped intermediate position");
        view.advanceFrame(16);check(view.offset()==first,"same frame is idempotent");
        for(int time=32;time<=160;time+=16) {
            view.setViewport(0,0,100,40);view.setContentHeight(400);view.setPolicy(new UiScrollView.Policy(1,8,0,false,false));
            view.advanceFrame(time);check(view.offset()>=first&&view.offset()<=100,"range updates preserve forward motion");first=view.offset();
        }
        settle(view,176);check(view.offset()==100,"animation converges exactly");
        UiScrollView a=scrollView(),b=scrollView();a.advanceFrame(0);b.advanceFrame(0);a.scrollByPixels(-100);b.scrollByPixels(-100);
        a.advanceFrame(16);a.advanceFrame(32);b.advanceFrame(32);check(Math.abs(a.offset()-b.offset())<=1,"elapsed time independent of short frame partition");
        view.scrollByPixels(-100);view.scrollByPixels(50);settle(view,1200);check(view.offset()==150,"events accumulate from pending target");
        view.setOffset(0);view.advanceFrame(2500);for(int i=0;i<8;i++)view.scrollByPixels(-0.125);settle(view,2516);check(view.offset()==1,"subpixel wheel accumulates");
        view.scrollByPixels(-100);view.setOffset(20);settle(view,3700);check(view.offset()==20,"immediate offset cancels old target");
        view.ensureVisibleSmoothly(new UiRect(0,200,10,10));check(view.offset()==20,"smooth reveal preserves current position");settle(view,4800);check(view.offset()==170,"smooth reveal uses viewport bottom");
        view.scrollByPixels(-100);view.ensureVisible(new UiRect(0,0,10,10));settle(view,5900);check(view.offset()==0,"immediate reveal cancels animation");
        view.scrollByPixels(-500);check(!view.scrollByPixels(-1),"target edge bubbles");view.setContentHeight(45);settle(view,7000);check(view.offset()==5,"shrinking clamps actual and pending motion");
        view.setContentHeight(10);check(view.offset()==0&&!view.scrollBy(-1),"short content has no motion");
        view=scrollView();view.advanceFrame(0);view.scrollByPixels(-100);view.advanceFrame(16);int stopped=view.offset();view.scopeExited(UiInteractionSession.EndReason.SCOPE_CHANGED);settle(view,32);check(view.offset()==stopped,"scope exit cancels pending motion");
        view.setPolicy(new UiScrollView.Policy(10,8,0,false,false));view.setOffset(13);check(view.offset()==10,"immediate position preserves alignment");view.scrollByPixels(-21);settle(view,1200);check(view.offset()%10==0,"animated position preserves alignment");
        UiScrollView dragged=scrollView();UiInputScope scope=new UiInputScope(dragged);UiInputRouter router=new UiInputRouter();router.select(scope);
        dragged.advanceFrame(0);dragged.scrollByPixels(-100);dragged.advanceFrame(16);int beforeDrag=dragged.offset();
        router.mousePressed(at(scope,98,dragged.scrollbarThumb().y()+1));settle(dragged,32);check(dragged.offset()==beforeDrag,"thumb press takes over pending motion");
        router.mouseDragged(at(scope,98,30),0,29);int end=dragged.offset();router.mouseReleased(at(scope,98,30));settle(dragged,1200);check(dragged.offset()==end,"drag release keeps final position");router.close();
    }
    static void frameTree() {
        UiListView<String> list=new UiListView<>((g,f,item,index,rect,selected,hovered,focused)->{});
        list.setBounds(0,0,100,40);list.setItems(List.of("a","b","c","d","e","f"));
        UiChildren<UiInputTarget> members=new UiChildren<>();members.add(list);UiOverlay overlay=new UiOverlay(members);
        UiInputScope scope=overlay.inputScope();UiInputRouter router=new UiInputRouter();router.select(scope);scope.refreshFocus();
        overlay.advanceFrame(0);router.mouseScrolled(at(scope,5,5),0,-1);int initial=list.rowBounds(0).y();
        list.layoutNodes();list.hitTest(5,5);check(list.rowBounds(0).y()==initial,"read-only queries do not animate");
        overlay.advanceFrame(16);check(list.rowBounds(0).y()<initial,"overlay member tree advances list");
        int row=list.itemIndexAt(5,5);check(row>=0&&list.visibleRowBounds(row).contains(5,5),"animated hit matches projected row");
        list.setVisible(false);int stopped=list.rowBounds(0).y();settle(overlay,32);list.setVisible(true);settle(overlay,1200);check(list.rowBounds(0).y()==stopped,"hidden list clears pending target");router.close();
        UiModal modal=UiModal.create(new MetricFont()).contentWidget(list,40).cancel(Component.literal("Cancel"));
        modal.setBounds(0,0,160,100);scope=modal.inputScope();router.select(scope);scope.refreshFocus();
        scope.content().advanceFrame(0);int modalY=list.rowBounds(0).y();
        router.mouseScrolled(at(scope,list.bounds().x()+2,list.bounds().y()+2),0,-1);
        scope.content().advanceFrame(16);check(list.rowBounds(0).y()<modalY,"modal content wrapper forwards frame");
        router.select(new UiInputScope(new UiInputTarget() {}));stopped=list.rowBounds(0).y();
        settle(scope.content(),32);check(list.rowBounds(0).y()==stopped,"leaving modal cancels pending scroll");router.close();
    }
    static void run() { scrollMotion();frameTree(); }
}
