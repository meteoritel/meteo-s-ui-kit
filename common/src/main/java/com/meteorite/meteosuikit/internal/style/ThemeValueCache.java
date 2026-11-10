package com.meteorite.meteosuikit.internal.style;

import com.meteorite.meteosuikit.api.style.UiStyleSource;
import java.util.function.BiFunction;

/** 控件实例私有的单项主题解析缓存；布局与局部覆盖由控件维护。 */
public final class ThemeValueCache<R, V> {
    private final BiFunction<UiStyleSource, R, V> resolver;
    private UiStyleSource source;
    private R role;
    private long revision;
    private V value;

    /** 解析器在创建缓存时绑定，稳定帧只比较键。 */
    public ThemeValueCache(BiFunction<UiStyleSource, R, V> resolver) { this.resolver = resolver; }

    /** 来源使用身份比较；角色由所属控件的稳定角色对象提供。 */
    public V resolve(UiStyleSource source, R role) {
        long revision = source.revision();
        if (this.source != source || this.role != role || this.revision != revision) {
            value = resolver.apply(source, role);
            this.source = source;
            this.role = role;
            this.revision = revision;
        }
        return value;
    }

    /** 局部配置重新绑定时强制下次解析。 */
    public void invalidate() { source = null; }
}
