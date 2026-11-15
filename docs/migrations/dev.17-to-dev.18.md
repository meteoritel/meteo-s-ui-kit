# dev.17 → dev.18：灯箱拖拽锁定

`UiLightbox` 新增 `setPanLocked(boolean)`、`isPanLocked()` 和 `setPanLockControlVisible(boolean)`。宿主可以选择显示工具栏锁定按钮，锁定只阻止鼠标拖拽平移；滚轮缩放、复位和内容点击仍按原契约工作。锁定状态同步给内容；自定义 `Content` 可覆盖默认 setter 以消费锁定期间的拖动。默认值为 `false`，已有调用方保持原行为。

固定消费 `0.1.0-dev.18` 并同步更新编译、内嵌与运行元数据；发布及独立 Maven 消费按[工具入口](../../tools/README.md)执行。
