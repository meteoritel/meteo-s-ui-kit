# dev.18 → dev.19

本版本将原版容器接入的默认提示层、根准备时序和共享模型资源所有权集中到 kit。

## 提示呈现

将 `hit.renderTooltip(graphics, font, styleOrSkin, x, y)` 替换为 `UiTooltipPresenter.render(graphics, font, hit, styleOrSkin, x, y)`，导入 `api.render.UiTooltipPresenter`。`UiPointerHit` 保留命中、归属与提示来源。

普通提示的默认局部深度为 400，背景、描边和正文一起覆盖槽位物品。删除宿主在 presenter 外额外添加的 TOOLTIP 偏移，使用当前作用域基线，并在内容裁剪退出后呈现。原版富提示回调继续直接调用 `GuiGraphics.renderTooltip`，其原生 400 只应用一次。提示资格仍由当前作用域门禁决定。

`UiPointer.resolve` 无命中时返回 `UiPointerHit.EMPTY`。原版容器回退槽位提示时检查 `hit == null || hit.owners().isEmpty()`；真实 kit 命中即使没有文案，也应保留该位置的遮挡关系。

## 根接入与资源

继承 `UiScreen` 的页面沿用根钩子。组合原版容器使用 `UiScreenBridge`，由 Host 提供当前 scope、viewport 和准备/布局钩子，在绘制帧调用 `beginFrame(nowMillis)`。事件转发给 bridge，返回 false 才调用父类。init 或事件之外改变覆盖面后调用 `refresh()`，removed 调用 `close()`。完整用法见 [接入说明](../integration.md#最小接入)。

根统一准备、布局、刷新焦点和选域，事件后再次刷新；活动内容的帧推进只发生在 beginFrame。关闭回调卸载根时，旧事件跳过完成刷新。同一实例重新挂载继续使用稳定控件。

删除 `UiRenderLayers.releaseModels()` 调用。共享 GPU 工作区由 kit 的客户端世代与终止事件管理，屏幕关闭只释放自己持有的业务资源。根的 `invalidateUiResources` 用于刷新语言、字体和模型相关缓存。

`PreviewPlatforms.geometry()` 在 Fabric client initialize / NeoForge client setup enqueueWork 安装，首次 Screen 或之后读取。

## 灯箱与工具

`UiLightbox.Labels` 实现必须提供本地化 `panLock()` 和 `panUnlock()`；缩放读数保留默认百分比格式。已有 surface、icons、text、barHint 等外观 setter 继续可用。

维护工具需要 Python 3.11+；Gradle 可用 `-Prepository_python=python3` 或绝对路径选择解释器。行为契约位于非发布的 `common/src/contractTest/java`，用 `-PkitContract=tooltip` 等契约名单跑。独立 consumer-check 可用 `-Pmeteos_ui_kit_version=<精确版本>` 指定消费坐标；该属性由消费者读取，kit 发布版本仍由根 `version` 决定。
