## dev.15 → dev.16：作用域页面回退

`UiInputScope` 新增 `setNavigationBack(@Nullable Runnable)`。传入动作后，当前活动域响应第四个鼠标键的按下，以及内容未消费的 Backspace；传入 null 恢复原输入行为。

```java
UiInputScope scope = new UiInputScope(content);
scope.setNavigationBack(() -> {
    if (!history.back(this::isValid, this::restore)) onClose();
});
```

动作执行前取消持续交互并清理普通按压配对；侧键释放只消费一次，不重复调用动作。回调换域后的释放仍由同一根接住；根卸载清空配对。宿主根据页面规则决定空栈行为，并让事件外层的普通历史记录跳过回退事件。

文本编辑继续优先消费 Backspace，Ctrl+Z / Ctrl+Y 继续编辑局部历史。模态框或灯箱拥有独立作用域，按自己的输入规则工作；需要回退动作时显式绑定到对应 scope。

`UiScreen` 在回调卸载页面后保持路由关闭，不再执行该事件的准备和选域；同一 Screen 实例重新打开仍可接收输入。开发展示根使用侧键或 Backspace 返回宿主。

按钮编号使用 GLFW `GLFW_MOUSE_BUTTON_4`。驱动重映射后的按键以游戏实际收到的事件为准。
