package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiControl;
import com.meteorite.meteosuikit.api.control.UiControlGroup;
import com.meteorite.meteosuikit.api.style.UiControlRole;
import com.meteorite.meteosuikit.api.style.UiControlStyle;
import com.meteorite.meteosuikit.api.style.UiMinecraftTheme;
import com.meteorite.meteosuikit.api.style.UiStyleSource;
import com.meteorite.meteosuikit.api.style.UiThemes;

final class ThemeContract extends ContractSupport {
    static void theme() {
        UiControlGroup group = new UiControlGroup();
        UiControlStyle a = new UiControlStyle(1,2,3,4,5,6,7,8);
        UiControlStyle b = new UiControlStyle(11,12,13,14,15,16,17,18);
        UiStyleSource theme = UiThemes.builder(UiMinecraftTheme.INSTANCE).control(UiControlRole.SECONDARY, skin -> skin.withStyle(b)).build();
        group.beginUpdate(); UiControl old = group.obtain("old"); group.endUpdate();
        group.setStyle(a); group.setTheme(() -> theme);
        group.beginUpdate(); check(group.obtain("old")==old,"key reuse"); UiControl fresh = group.obtain("new"); group.endUpdate();
        check(group.style()==a && old.style()==a && fresh.style()==a,"theme preserves group override for old/new/reused");
        old.setSkin(theme.controlSkin(UiControlRole.SECONDARY));
        old.setStyle(a); old.clearStyleOverride(); check(old.style().equals(b),"clear style preserves explicit skin");
        group.setStyle(a); group.clearStyleOverride();
        check(group.style().equals(b) && old.style().equals(b) && fresh.style().equals(b),"group clear broadcasts and clears defaults");
        old.clearSkinOverride(); check(old.style().equals(b),"clear skin returns to root");
    }
    static void run() { theme(); }
}
