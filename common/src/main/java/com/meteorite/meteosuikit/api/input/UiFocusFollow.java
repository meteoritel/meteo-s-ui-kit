package com.meteorite.meteosuikit.api.input;

import com.meteorite.meteosuikit.api.control.UiScrollView;
import com.meteorite.meteosuikit.api.geometry.UiRect;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

/** 焦点滚动效果；宿主只提供归属、内容坐标与滚动后的布局同步。 */
public final class UiFocusFollow implements UiFocusManager.Listener {
    private final UiScrollView scroll;
    private final Predicate<UiFocusTarget> contains;
    private final Function<UiFocusTarget, UiRect> contentBounds;
    private final Runnable layoutChanged;

    /** 嵌套视口按内到外组合，每层在执行时读取目标的最新边界。 */
    public UiFocusFollow(UiScrollView scroll, Predicate<UiFocusTarget> contains,
                         Function<UiFocusTarget, UiRect> contentBounds, Runnable layoutChanged) {
        this.scroll = scroll;
        this.contains = contains;
        this.contentBounds = contentBounds;
        this.layoutChanged = layoutChanged;
    }

    /** 让属于本视口的目标可见，已可见时保持视图位置。 */
    public void reveal(@Nullable UiFocusTarget target) {
        if (target == null || !contains.test(target)) return;
        int previous = scroll.offset();
        scroll.ensureVisible(contentBounds.apply(target));
        if (previous != scroll.offset()) layoutChanged.run();
    }

    @Override
    public void focusChanged(@Nullable UiFocusTarget previous, @Nullable UiFocusTarget current) {
        reveal(current);
    }
}
