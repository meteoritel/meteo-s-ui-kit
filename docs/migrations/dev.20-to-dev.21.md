# dev.20 → dev.21

原版容器组合根在 `init` 调用 `UiScreenBridge.attach(this)` 后再 `refresh()`，并在 `removed` 调用 `close()`。挂载的组合屏幕在平台前置阶段接收模态鼠标事件。Fabric 的拖动仍由原版屏幕覆写转发。事件未消费时按正常顺序回退到父类。

`UiInputBridge.hasMousePress(int)` 可判断桥是否已接住配对按下。继续由屏幕组合桥统一管理拖动与迟到释放。
