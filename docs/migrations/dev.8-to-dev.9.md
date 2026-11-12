# dev.8 → dev.9

`UiViewport.fit` 改为接收实际 framebuffer 宽高、原版 GUI 倍率和最小画布宽高。直接调用者同步更新参数；`UiScreen` 宿主继续声明最小画布，由根采集窗口指标。

固定画布使用能容纳内容的整数物理像素倍率，原版 GUI 倍率决定放大上限。根比例为物理倍率除以 GUI 倍率；需要缩小时，逻辑尺寸由实际 framebuffer 除以物理倍率向下取整。无需缩小时沿用原版 GUI 尺寸，保留奇数 framebuffer 的取整语义。实际像素不足 `1×` 时仍连续缩小。

`UiPointerHit.renderTooltip` 的指针仍使用当前轴对齐二维 pose 的坐标；普通提示和富提示统一在原版 GUI 空间绘制，保持 GUI 字号、屏幕限位与当前 Z 层级。`Tooltip.render` 回调的坐标明确为 GUI 坐标，宿主移除自行叠加的根倒倍率。原版物品 Tooltip 回调继续直接调用 `GuiGraphics.renderTooltip`。

缩放内容的裁剪使用 `UiRenderLayers.enableScissor/disableScissor` 配对，提示在内容裁剪恢复后绘制。模态框的文字裁剪已采用该入口。
