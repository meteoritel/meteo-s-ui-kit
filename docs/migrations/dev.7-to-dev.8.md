# dev.7 → dev.8

`UiScreen` 统一接管根视口。固定画布覆盖 `minimumUiWidth/Height()`，布局读取 `viewport()` 逻辑尺寸；绘制钩子的鼠标参数已经换算，原版输入入口仍传 GUI 坐标。宿主移除外部 pose 缩放及重复输入转换。默认最小尺寸保持恒等变换，原版容器桥继续使用原生槽位空间。

`UiScreen.layoutNodes()` 为 final，只读取活动作用域的真实 `UiLayoutSource`。自绘容器从实际行几何贡献节点；`UiInputContainer` 默认聚合成员，`UiChildren` 和滚动容器传播布局。宿主通过 `setLayoutKey` 或节点 `identified` 赋稳定业务身份；仅展示节点保持空 key。`isLayoutTarget` 同时检查唯一身份、有效可见矩形及真实 hitTest 归属。

`UiLayoutNode` 增加身份、输入目标和有效裁剪几何，原构造方式仍可用于展示节点。子坐标均为根内容空间；根统一投影到 GUI 空间。查询不调用业务准备或模拟一次渲染；在完成绘制后获取快照。

`UiFrameCapture.capture(Path)` 在客户端主线程提交，异步返回后续完整帧的原尺寸 PNG 与采样身份。当前世界、Screen 和窗口尺寸需在采样前保持；调用方负责业务就绪。并发请求、上下文改变和 IO 失败明确报错，取消/客户端停止结清 future。路径须为新文件。NeoForge 使用帧末事件，Fabric 使用客户端整段 GameRenderer 绘制结束的窄 Mixin。

开发展示画布可在小窗口验证输入与绘制，进入世界后 F9 请求截图。发布与消费仍使用新坐标，旧 dev 坐标保留。
