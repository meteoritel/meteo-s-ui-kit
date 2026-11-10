# 接入与资源所有权

普通页面使用 `UiScreen` 或 `UiThemedScreen`，原版容器组合唯一的 router 与 `UiInputBridge`。输入前准备、布局和选域，返回未消费时才回退原版父类。

宿主稳定持有主题 Context、输入根、业务内容与场景所有者。`PreviewPlatforms.geometry()` 在库客户端初始化完成后提供默认平台能力；宿主仍注入 executor、时钟、诊断和预算，并关闭自己的 `PreviewScenes`。

库平台入口负责 Geometry、Display Mixin 与共享模型工作区的世代观察及终止释放。根卸载仍释放自己的资源。业务资源世代、缓存、线程池与文案继续归宿主。

有效公开面以 JavaDoc、`tools/ui-kit-interface/api.txt` 与 `package-info.java` 为准；宿主只引用 api。基础能力不反向依赖 preview，common 不导入加载器类型。

## 固定版本消费

在用户级 Gradle 属性配置 `meteos_ui_kit_maven_dir`，项目 `gradle.properties` 配置 `meteos_ui_kit_version`。仓库用 exclusiveContent 限定 group `com.meteorite.meteosuikit`；缺少本机属性时给出配置错误。

- common：compileOnly `meteos-ui-kit-common-1.21.1`。
- Fabric：modImplementation + include `meteos-ui-kit-fabric-1.21.1`。
- NeoForge：implementation + jarJar `meteos-ui-kit-neoforge-1.21.1`。

group 为 `com.meteorite.meteosuikit`，编译和内嵌选择项目配置的精确版本。宿主元数据声明库 Mod ID `meteos_ui_kit` 的最低能力版本与下一 minor 上限；0.1 系列上限是 0.2.0。NeoForge jarJar 使用严格兼容区间并 prefer 精确版本，真实内嵌版本由宿主产物检查核对。common 编译 JAR 只用于编译，不随平台 JAR 再次内嵌。

0.x 同 minor 维持源码、二进制和行为兼容；破坏性修改提升 minor 并提供迁移说明。开发验证版本采用不可覆盖的 `0.1.0-dev.N`。发布与消费命令见 [工具入口](../tools/README.md)。

## 文档动作与排版几何

`UiDocument.targets()` 从排版缓存返回 `UiDocumentTarget` 快照。行、前置图标和尾随图标保留各自 `UiDocumentHit`，宿主按 `hit().action()` 选择动作，按 `payload()` 解释业务语义。同一动作出现多次时保留每次出现，`path()` 表示当前内容结构中的出现位置；跨内容版本的身份由宿主 payload 决定。

`hit().rect()` 位于叶子文档的内容空间，`contentBounds()` 位于查询文档的内容空间，`screenBounds()` 位于调用方 GUI 空间。Frame 子文档保留 `document()` 归属，两个投影包含相应的嵌套变换。结果包含视口外节点，宿主负责滚动定位与裁剪；外部 pose 仍由宿主转换。内容、字体、尺寸或资源失效后重新读取，缩放和平移后重新取得屏幕几何。

灯箱内容实现 `UiFocusTarget` 或贡献子焦点目标时，`UiLightbox` 将其加入同一作用域的 Tab 序列。内容持焦时先接收普通按键与字符，配对释放和退出通知沿内容传递；Escape 仍由灯箱关闭入口处理。
