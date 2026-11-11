# 接入与资源所有权

普通页面使用 `UiScreen` 或 `UiThemedScreen`，原版容器组合唯一的 router 与 `UiInputBridge`。输入前准备、布局和选域，返回未消费时才回退原版父类。

宿主稳定持有主题 Context、输入根、业务内容与场景所有者。`PreviewPlatforms.geometry()` 在库客户端初始化完成后提供默认平台能力；宿主仍注入 executor、时钟、诊断和预算，并关闭自己的 `PreviewScenes`。

库平台入口安装默认 Geometry，并接入共享客户端生命周期对模型工作区的世代观察及终止释放。Display accessor 与 client Mixin 配置使用 common 共享源码和资源，各端保留自己的 block 渲染路径与事件相位。根卸载仍释放自己的资源。业务资源世代、缓存、线程池与文案继续归宿主。

有效公开面以 JavaDoc、`tools/ui-kit-interface/api.txt` 与 `package-info.java` 为准；宿主只引用 api。基础能力不反向依赖 preview，common 不导入加载器类型。

## 固定版本消费

在用户级 Gradle 属性配置 `meteos_ui_kit_maven_dir`，项目 `gradle.properties` 配置 `meteos_ui_kit_version`。仓库用 exclusiveContent 限定 group `com.meteorite.meteosuikit`；缺少本机属性时给出配置错误。

- common：compileOnly `meteos-ui-kit-common-1.21.1`。
- Fabric：modImplementation + include `meteos-ui-kit-fabric-1.21.1`。
- NeoForge：implementation + jarJar `meteos-ui-kit-neoforge-1.21.1`。

group 为 `com.meteorite.meteosuikit`，编译和内嵌选择项目配置的精确版本。common 编译 JAR 只用于编译，不随平台 JAR 再次内嵌。

dev 序列允许源码、二进制与行为破坏，每个新 dev 坐标附迁移说明且不可覆盖。消费者的编译、内嵌和运行元数据锁定同一 dev：Fabric 依赖使用精确版本字符串，NeoForge 依赖和 jarJar 使用 `[版本]`，jarJar 同时 prefer 该版本。升级时成套替换宿主与库，避免旧宿主的宽区间接受新 dev。

正式 0.x 版本在同 minor 内维持源码、二进制与行为兼容；破坏性修改提升 minor 并提供迁移说明。正式版运行范围按该兼容承诺选择。dev 迁移见 [dev.4 → dev.7](migrations/dev.4-to-dev.7.md)，发布与消费命令见 [工具入口](../tools/README.md)。

## 主题、状态与滚动内容

`UiThemeContext` 持有当前主题与根发布版本。主题化控件接收 `Supplier<? extends UiStyleSource>`；根稳定持有 `context::current`，固定主题传固定 Supplier。控件缓存按实际样式身份、样式 revision 与角色失效；`UiThemePublication` 在手势结束后发布最后请求。主题切换保留控件实例及其编辑、焦点和会话状态。

`UiTextScheme` 提供共同文字语义及浅/暗底预设。`UiSurfaceStyle.withText(scheme)` 只映射正文、标签与弱化文字；业务专属语义和结构色由宿主配方维护。

`UiShellTarget` 复用非渲染状态壳，`UiShellWidget` 在其上组合绘制契约。输入目标自行贡献可见焦点子树和资格；禁用目标保留登记，由 `canFocus` 控制导航。

`UiPaginationState` 将动态页数和页码归一化，读取后保留合法状态；内容、选择和翻页副作用由宿主解释。`UiScrollContainer` 组合既有滚动视图与屏幕坐标内容，集中处理视口命中、滚动条优先和子级未消费的滚轮回退。内容按偏移布局并裁剪，捕获由 scope 直达实际目标。`UiFormLayout` 的结果可同时驱动控件、标签、内容高度与焦点滚动。

`TextWrap.fit` 使用原版格式化逻辑折行，按宽度和行数裁剪后才执行视觉排序；超行时为末行预留 ASCII `...`。缓存与生命周期归调用方，普通原版折行可直接使用 `Font.split`。

## 文档动作与排版几何

`UiDocument.targets()` 从排版缓存返回 `UiDocumentTarget` 快照。行、前置图标和尾随图标保留各自 `UiDocumentHit`，宿主按 `hit().action()` 选择动作，按 `payload()` 解释业务语义。同一动作出现多次时保留每次出现，`path()` 表示当前内容结构中的出现位置；跨内容版本的身份由宿主 payload 决定。

`hit().rect()` 位于叶子文档的内容空间，`contentBounds()` 位于查询文档的内容空间，`screenBounds()` 位于调用方 GUI 空间。Frame 子文档保留 `document()` 归属，两个投影包含相应的嵌套变换。结果包含视口外节点，宿主负责滚动定位与裁剪；外部 pose 仍由宿主转换。内容、字体、尺寸或资源失效后重新读取，缩放和平移后重新取得屏幕几何。

灯箱内容实现 `UiFocusTarget` 或贡献子焦点目标时，`UiLightbox` 将其加入同一作用域的 Tab 序列。内容持焦时先接收普通按键与字符，配对释放和退出通知沿内容传递；Escape 仍由灯箱关闭入口处理。
