# dev.16 → dev.17：统一阻尼滚动与单行文字呈现

`UiScrollView.scrollBy` 与 `scrollByPixels` 将请求累积到阻尼目标，返回目标是否接受位移。前者保留方向整步，后者保留实际像素量及小数。读取 `offset()` 仍取得当前可见整数位置；同方向目标已到极限时返回 false，父级可按自己的政策接收滚轮。

立即恢复阅读位置使用 `setOffset`，立即露出光标或字段使用 `ensureVisible`；两者同步结清旧动画。平滑键盘定位使用 `ensureVisibleSmoothly`。范围更新独立钳制当前状态和目标，重复 `setViewport` / `setContentHeight` / `setPolicy` 保留动画。

`UiInputTarget.advanceFrame(long nowMillis)` 默认空实现。`UiScreen` 在准备、布局和选域后自动推进活动 scope 的内容树；`UiChildren`、`UiInputContainer`、覆盖面与通用滚动消费者负责转发。自定义包装器沿实际成员转发该入口及 `scopeExited`，删除宿主自行维护的目标、指数公式与专用帧推进。同一视图保留一个所有者，帧时间由根统一采样。

保留原版容器的组合宿主，在布局与选域后、解析命中与绘制前调用：

```java
layers.activeScope().content().advanceFrame(Util.getMillis());
```

滚动条拖动或轨道按下立即接管；退出作用域、隐藏和禁用后停在当前可见位置。表单在推进后重投影真实字段，输入命中、快照和绘制读取相同坐标。分页状态与灯箱的滚轮缩放沿各自原契约工作。

`UiTextInput` 的构造与编辑入口保持原用法，`EditBox` 和 `UiTextEditSession` 保留模型与历史。私有视图改为无阴影绘制主题正文、提示、选区与光标，鼠标寻址与绘制共用水平窗口，按码点边界保留 UTF-16 模型索引。宿主直接升级，无需 Mixin 或自建输入框。

固定消费 `0.1.0-dev.17` 并同步更新编译、内嵌与运行元数据；发布及独立 Maven 消费按[工具入口](../../tools/README.md)执行。
