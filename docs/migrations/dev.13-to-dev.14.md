# dev.13 → dev.14

`UiOverlay` 实现 `UiLayoutSource`，从包装内容贡献当前布局；纯输入内容贡献空布局。命中、焦点与布局继续共享同一内容身份，宿主通过 `UiScreen.layoutNodes()` 和 `isLayoutTarget()` 可访问活动模态的真实字段及业务 key。

调用方源码保持原有装配方式。消费者编译、内嵌与运行元数据同步锁定 dev.14；本地供应和消费校验见[工具入口](../../tools/README.md)。
