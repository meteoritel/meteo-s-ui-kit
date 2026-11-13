# dev.12 → dev.13

`UiListView` 实现 `UiLayoutSource`，根节点使用 `Kind.LIST`，可见行保留完整矩形、视口裁剪和列表输入归属。使用 `setLayoutKey` 标记列表，使用 `setItemKey` 提供业务身份；未配置的行 key 为空。行 target 是列表，外部操作继续核对真实命中。列表主题与行回调异常通过 finally 恢复 pose 和 scissor，异常继续传播。

`UiFormBody` 组合已有排版、滚动容器和焦点跟随。稳定装配字段、标签及尾随动作后，只需设置正文 bounds 并 render；宿主把 `body.reveal(current)` 接入已有焦点 listener。普通字段使用固定高度入口，说明块通过 `UiFormLayout.Control` 测量；内容或字体资源改变时调用 `invalidateLayout()`。它不创建 scope，也不持有保存、权限或草稿规则。

正文固定预留滚动条带宽，短正文也保留轨道留白；轨道只在溢出时绘制。字段使用屏幕坐标，宿主删除重复的行索引、二次测量、偏移 pose 和裁剪。隐藏行退出布局和焦点，禁用行保留布局；移除字段通过 `remove` 结清交互。

`UiFormLayout` 对窄窗堆叠标签按完整正文宽度布局，无标签正文不占标签行。作用域退出回调再次切域时只激活最终选择；重新选择自身恢复焦点。

编译、内嵌与运行元数据同步锁定 dev.13。本地发布和隔离恢复见[工具入口](../../tools/README.md)。
