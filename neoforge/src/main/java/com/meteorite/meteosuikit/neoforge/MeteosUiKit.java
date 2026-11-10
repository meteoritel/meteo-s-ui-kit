package com.meteorite.meteosuikit.neoforge;

import net.neoforged.fml.common.Mod;

/** 服务端可安全发现库身份，客户端能力由独立的客户端事件入口安装。 */
@Mod("meteos_ui_kit")
public final class MeteosUiKit {
    /** 引导不引用 GUI 类型，允许库随双端宿主内嵌。 */
    public MeteosUiKit() { }
}
