package com.meteorite.meteosuikit.api.control;

import java.util.Objects;
import java.util.function.IntSupplier;

/** 页码与动态页数的共同状态；数据收缩后立即保留合法页码。 */
public final class UiPaginationState {
    private final IntSupplier pageCount;
    private int page;

    /** 页数由内容所有者提供，空内容仍视为一页。 */
    public UiPaginationState(IntSupplier pageCount) {
        this.pageCount = Objects.requireNonNull(pageCount, "pageCount");
    }

    /** 读取页数并归一化存储状态，后续增长不会复活越界的旧页。 */
    public int pageCount() {
        int count = Math.max(1, pageCount.getAsInt());
        page = Math.clamp(page, 0, count - 1);
        return count;
    }

    /** 当前合法页码，从零开始。 */
    public int getPage() { pageCount(); return page; }

    /** 设置合法页码，返回相对归一化后当前页是否变化。 */
    public boolean setPage(int next) {
        int count = pageCount();
        int previous = page;
        page = Math.clamp(next, 0, count - 1);
        return previous != page;
    }

    /** 相对当前合法页前后移动，返回是否发生翻页。 */
    public boolean changePage(int delta) {
        int count = pageCount();
        return setPage((int) Math.clamp((long) page + delta, 0L, count - 1L));
    }

    /** 回到首页，内容与选择由调用方维护。 */
    public void reset() { page = 0; }
}
