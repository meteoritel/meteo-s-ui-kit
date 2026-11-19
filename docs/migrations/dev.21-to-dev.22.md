# dev.21 → dev.22

## 屏幕背景

`UiThemedScreen` 在逻辑缩放前调用 `renderGuiBackground`，参数为 Minecraft 原版 GUI 鼠标坐标。旧的 `renderThemedBackground` 覆写只负责主题或宿主底板。若覆写原先自己调用 `renderBackground`，删除该调用及逆缩放，保留逻辑坐标的绘制。管理页仅使用原版背景时，保留空的主题底板覆写。

继承 `UiScreen` 的自绘页面可选择覆写 `renderGuiBackground`。原版容器继续通过 `UiScreenBridge` 转发并保留 `super` 回退。

## 实际成员组合

- `UiWidgetRow` 按顺序登记 `UiWidget` 与 `UiLinearLayout.Child`，布局阶段给整行逻辑矩形，绘制阶段调用行的 `render`。行管理子控件的摆放、命中、焦点和快照。
- 同一作用域内替换已有分支时使用 `UiInputRouter.replaceChildren`。移除分支收到离场，仍登记的实例及 Tab 次序继续有效。
- 用 `UiOverlay.layer` 挂载场景浮层，将宿主遮罩矩形、颜色和 painter 注入实际内容的层。将普通覆盖层和 `UiModalStack.layers()` 按底到顶登记到 `UiLayers`。

## 滚动

`UiScrollContainer` 可为屏幕坐标内容集中裁剪与绘制轨道。真实子控件可传入投影动作，滚动推进后更新位置。自绘目录可用 `forPixelWheel` 保留原始滚轮小数，再用 `UiGridMetrics.visibleCells` 查询可见格索引。推荐正文可通过 `UiDocumentView` 提供折行、滚动并设置实际需求的滚轮像素步长。
