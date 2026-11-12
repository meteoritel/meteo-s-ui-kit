package com.meteorite.meteosuikit.fabric.client;

import com.meteorite.meteosuikit.internal.client.UiFrameCaptureState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric HUD 事件早于 Screen；在整段 GameRenderer 绘制返回后采样。 */
@Mixin(GameRenderer.class)
abstract class FrameCaptureMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void frameEnded(CallbackInfo callback) { UiFrameCaptureState.frameEnded(); }
}
