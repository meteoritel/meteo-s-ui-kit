# 接入与资源所有权

普通页面使用 `UiScreen` 或 `UiThemedScreen`，原版容器稳定组合唯一的 router 与 `UiScreenBridge`。共同根实现负责准备、布局、刷新焦点、选域和事件后刷新；bridge 返回 false 时回退原版父类。

## 最小接入

- 普通页面：[KitMinimalScreen](../common/src/dev/java/com/meteorite/meteosuikit/samples/KitMinimalScreen.java) 继承 `UiThemedScreen`，只提供内容、作用域与布局；完整页面见 [KitShowcaseScreen](../common/src/dev/java/com/meteorite/meteosuikit/samples/KitShowcaseScreen.java)。
- 原版容器：[KitContainerExample](../common/src/dev/java/com/meteorite/meteosuikit/samples/KitContainerExample.java) 保留 `AbstractContainerScreen` 与 Menu / Slot，由 Host 提供当前作用域、恒等 viewport、准备与布局钩子。init 调用 `bridge.attach(this)` 和 `bridge.refresh()`，render 在父类前调用 `bridge.beginFrame(Util.getMillis())`，removed 调用 `bridge.close()`。

固定按钮由页面稳定持有具名 `UiControl` 引用，显式设置主题与焦点能力，配置动作后将实际控件注册到输入树。布局、业务可用性和特殊皮肤通过该引用更新；成员集合供绘制、命中和导航共用。操作行可按用途保存布局列表，同一列表同时决定几何分配与实际摆放。

动态内容使用 `UiControlGroup` 按稳定键复用。`obtain(Object)` 接受枚举、资源标识等业务身份并返回实际控件；更新区间内未再取得的成员在结束更新时移除，之后以同键加入会创建新实例。`controls()` 返回绘制层序，已有成员保持首次创建顺序，改变 `obtain` 调用顺序只复用实例。业务身份通过稳定键和控件引用表达，均匀排布可遍历布局列表；横纵分配与换行几何分别由 `UiLinearLayout` 和 `UiFlowLayout` 提供。

容器事件覆写使用同一种回退方式：

```java
@Override public boolean mouseClicked(double x, double y, int button) {
    return bridge.mousePressed(x, y, button) || super.mouseClicked(x, y, button);
}
```

拖动、释放、滚轮、按键和字符沿样例转发给同一个 bridge。其返回值包含消费、覆盖阻断和迟到释放配对；宿主无需另建配对表。bridge 只处理 kit 内容，原版槽位继续由父类驱动。

挂载后，阻断下层的作用域在平台前置事件中优先接收鼠标，根持有的按下继续接收配对拖动和释放。Fabric 的拖动通过原版屏幕入口进入同一 bridge。宿主对第三方覆层的展示和业务命中使用自己的活动覆盖层状态。

## 坐标、提示与资源


`UiInputScope.setNavigationBack` 绑定当前页面的回退动作：第四个鼠标键按下时执行，或在内容未消费 Backspace 时执行。回退先取消持续交互与旧指针按压，再调用宿主动作；侧键的拖动和释放由根配对消费，切换作用域后仍接住释放。未绑定动作时沿既有输入规则派发，顶层作用域只查询自己的动作。

页面历史通过 `UiNavigationHistory` 保存宿主快照，快照有效性、恢复与空栈行为由宿主决定。文本框的 Ctrl+Z / Ctrl+Y 保持局部编辑历史。宿主在事件结束时记录导航，需要跳过本次回退，避免将刚离开的页面重新入栈。`UiScreen` 在回调卸载根后跳过事件结束刷新；同一实例重新打开后按新的生命周期处理输入。接入说明见 [dev.15 → dev.16](migrations/dev.15-to-dev.16.md)。

固定画布在 `UiScreen` 声明最小尺寸，由根统一缩放绘制、输入与布局快照。根按实际 framebuffer 选择能容纳画布的整数物理像素倍率，原版 GUI 倍率为放大上限；不足 `1×` 时连续缩小以保留访问能力。无需缩小时保留原版 GUI 尺寸及取整语义。宿主迁移见 [dev.8 → dev.9](migrations/dev.8-to-dev.9.md)。

`UiTooltipPresenter.render` 接收当前轴对齐二维 pose 内的指针，在原版 GUI 空间呈现普通与富提示，保留当前作用域基线和主题。普通提示整体在该基线加 400，默认覆盖原版槽位物品，并保持在下一层模态遮罩之下。富提示回调收到 GUI 坐标，直接调用原版 Tooltip 自带的 400；presenter 不额外抬层。宿主调用 presenter 时使用当前作用域基线。作用域门禁仍由调用方检查。逻辑内容裁剪使用 `UiRenderLayers.enableScissor/disableScissor` 在 `finally` 中配对；提示在内容裁剪退出后呈现。

自绘内容从实际输入成员贡献稳定身份和裁剪几何；按需帧末 PNG 通过 `UiFrameCapture` 异步采集。布局与采样生命周期见 [dev.7 → dev.8](migrations/dev.7-to-dev.8.md)。

宿主稳定持有主题 Context、输入根、业务内容与场景所有者。`PreviewPlatforms.geometry()` 的安装时机为 Fabric 客户端初始化、NeoForge client setup 的 `enqueueWork`。宿主在首次 Screen 或之后读取平台能力；宿主仍注入 executor、时钟、诊断和预算，并关闭自己的 `PreviewScenes`。

库平台入口安装默认 Geometry，并接入共享客户端生命周期对模型工作区的世代观察及终止释放。Display accessor 与 client Mixin 配置使用 common 共享源码和资源，各端保留自己的 block 渲染路径与事件相位。模型共享 GPU 工作区由 kit 的客户端世代观察与终止事件独占释放；关闭屏幕只释放根自己的资源。根收到 `invalidateUiResources` 时刷新其文字和业务缓存；业务场景、线程池与文案继续归宿主。

有效公开面以 JavaDoc、`tools/ui-kit-interface/api.txt` 与 `package-info.java` 为准；宿主只引用 api。基础能力不反向依赖 preview，common 不导入加载器类型。

## 固定版本消费

在用户级 Gradle 属性配置 `meteos_ui_kit_maven_dir`，消费者项目的 `gradle.properties` 配置 `meteos_ui_kit_version` 并由其构建脚本读取。kit 自身的发布坐标由 `version` 决定；独立 consumer-check 默认消费该值，可用 `-Pmeteos_ui_kit_version=<精确版本>` 覆盖。仓库用 exclusiveContent 限定 group `com.meteorite.meteosuikit`；缺少本机属性时给出配置错误。

- common：compileOnly `meteos-ui-kit-common-1.21.1`。
- Fabric：modImplementation + include `meteos-ui-kit-fabric-1.21.1`。
- NeoForge：implementation + jarJar `meteos-ui-kit-neoforge-1.21.1`。

group 为 `com.meteorite.meteosuikit`，编译和内嵌选择项目配置的精确版本。common 编译 JAR 只用于编译，不随平台 JAR 再次内嵌。

dev 序列允许源码、二进制与行为破坏，每个新 dev 坐标附迁移说明且不可覆盖。消费者的编译、内嵌和运行元数据锁定同一 dev：Fabric 依赖使用精确版本字符串，NeoForge 依赖和 jarJar 使用 `[版本]`，jarJar 同时 prefer 该版本。升级时成套替换宿主与库，避免旧宿主的宽区间接受新 dev。

正式 0.x 版本在同 minor 内维持源码、二进制与行为兼容；破坏性修改提升 minor 并提供迁移说明。正式版运行范围按该兼容承诺选择。dev 迁移见 [迁移索引](migrations/README.md)，发布与消费命令见 [工具入口](../tools/README.md)。

## 主题、状态与滚动内容

`UiScreen` 在宿主 `advanceUiFrame`、准备、布局和选域后，以 `Util.getMillis()` 推进活动 scope 的实际内容树，再解析命中和绘制。`UiChildren` / `UiInputContainer` 自动转发；私有组合包装器转发 `advanceFrame(long)` 与 `scopeExited`。同一滚动视图只由实际所有者推进。组合原版容器通过 `UiScreenBridge.beginFrame(nowMillis)` 复用同一管线；事件准备只派发输入，动画只在帧入口推进。

`UiScrollView` 统一持有实际偏移和阻尼目标；`scrollBy` 向目标提交方向整步，`scrollByPixels` 保留实际像素与小数累积，返回目标是否改变。`setOffset` / `ensureVisible` 立即定位并结清旧目标，`ensureVisibleSmoothly` 从待完成目标计算滚入位置。查询保持只读，重复布局保留目标，范围缩短同时钳制实际与目标。拖动接管、隐藏、禁用和切域停止旧动画。列表、正文、表单及多行输入沿成员树复用这一机制，详情见 [dev.16 → dev.17](migrations/dev.16-to-dev.17.md)。

`UiTextInput` 使用原版 `EditBox` 编辑模型和既有局部历史；私有单行视图共享文字、选区、光标与指针的水平窗口。正文和提示显式关闭阴影，文字及选区读取当前主题。语言资源更新后重建视觉顺序和字体度量，resize 保留编辑草稿并调整窗口。

`UiThemeContext` 持有当前主题与根发布版本。主题化控件接收 `Supplier<? extends UiStyleSource>`；根稳定持有 `context::current`，固定主题传固定 Supplier。控件缓存按实际样式身份、样式 revision 与角色失效；`UiThemePublication` 在手势结束后发布最后请求。主题切换保留控件实例及其编辑、焦点和会话状态。

`UiTextScheme` 提供共同文字语义及浅/暗底预设。`UiSurfaceStyle.withText(scheme)` 只映射正文、标签与弱化文字；业务专属语义和结构色由宿主配方维护。

`UiStatefulTarget` 表达几何、可见性、可用性和布局身份，`UiStatefulWidget` 组合绘制契约。普通消费调用控件 setter，由控件同步原版模型、焦点和子会话。自定义控件私有组合 `UiWidgetShell`；包装现有 control 时转发语义操作并共享核心的 `focusState()`。扩展与迁移示例见 [dev.9 → dev.10](migrations/dev.9-to-dev.10.md)，Frame 裁剪几何修正见 [dev.10 → dev.11](migrations/dev.10-to-dev.11.md)，视口幂等与 resize 按压政策见 [dev.11 → dev.12](migrations/dev.11-to-dev.12.md)。

根主题切换保留成员显式结构、完整 skin 与独立文字覆盖。`UiControlGroup.setStyle` 和 `clearStyleOverride` 广播到当前成员并更新后续成员默认值；`style()` 返回后续新成员的默认结构。控件清除结构覆盖后使用完整 skin 或根主题，`clearSkinOverride` 同时清除结构和完整 skin，文字覆盖保持独立。

`UiPaginationState` 将动态页数和页码归一化，读取后保留合法状态；内容、选择和翻页副作用由宿主解释。`UiScrollContainer` 组合既有滚动视图与屏幕坐标内容，集中处理视口命中、滚动条优先和子级未消费的滚轮回退。内容按偏移布局并裁剪，捕获由 scope 直达实际目标。`UiFormLayout` 提供相对行几何；`UiFormBody` 持有真实字段和统一结果，负责标签、字段、尾随动作、裁剪与焦点滚入。正文固定预留轨道带宽，字段按屏幕坐标放置；宿主通过已有 listener 调用 `reveal(current)`，内容或字体变化后调用 `invalidateLayout()`。

`TextWrap.fit` 使用原版格式化逻辑折行，按宽度和行数裁剪后才执行视觉排序；超行时为末行预留 ASCII `...`。缓存与生命周期归调用方，普通原版折行可直接使用 `Font.split`。

## 文档动作与排版几何

`UiDocument.targets()` 从排版缓存返回 `UiDocumentTarget` 快照。行、前置图标和尾随图标保留各自 `UiDocumentHit`，宿主按 `hit().action()` 选择动作，按 `payload()` 解释业务语义。同一动作出现多次时保留每次出现，`path()` 表示当前内容结构中的出现位置；跨内容版本的身份由宿主 payload 决定。

`hit().rect()` 位于叶子文档的内容空间，`contentBounds()` 位于查询文档的内容空间，`screenBounds()` 位于调用方 GUI 空间。Frame 子文档保留 `document()` 归属，两个投影包含相应的嵌套变换，`contentClip()` 提供祖先 Frame 的内容空间裁剪。结果包含视口外节点，宿主负责滚动定位与裁剪；外部 pose 仍由宿主转换。内容、字体、尺寸或资源失效后重新读取，缩放和平移后重新取得屏幕几何。

`UiDocument.TextOverflow.WRAP` 将带样式文本折为视觉片段，逻辑父行、行/图标动作身份保持稳定；默认 `HOVER_SCROLL` 保留自然单行。`TextMeasurer.wrap` 是分行 seam，原版 adapter 使用字体 splitter。布局只随内容版本、宽度或显式失效重建，移动视口、滚动和变换复用排版。

页内使用 `UiDocumentView`，其 bounds 是正文内矩形，业务边框在外部绘制；通过 offset 读写阅读位置。灯箱使用 `UiDocumentLightboxContent`，独立持有文档和变换，可从稳定内容 Supplier 读取业务节点。两者统一持有动作导航、鼠标与键盘配对激活及失效取消，Frame 背景和正文使用同一裁剪。业务 Background 回调只解释 payload 和选中提示，在文档内容坐标绘制。语言或字体资源变化时调用阅读入口的布局失效方法。

文档灯箱 fit 不放大小内容并左上对齐；手动缩放后 resize 保留档位并钳制平移，fit 状态 resize 重新适配。关闭灯箱丢弃自己的阅读变换，页内 offset 由其控件保留。

灯箱全部按钮文案由 `UiLightbox.Labels` 提供，含 `panLock/panUnlock`。surface、icons、文字和提示保持独立配置，控制栏在窄窗优先保留关闭和 fit，空间不足时隐藏读数及次要控件。

灯箱内容实现 `UiFocusTarget` 或贡献子焦点目标时，`UiLightbox` 将其加入同一作用域的 Tab 序列。内容持焦时先接收普通按键与字符，配对释放和退出通知沿内容传递；Escape 仍由灯箱关闭入口处理。

列表快照和表单正文接入见 [dev.12 → dev.13](migrations/dev.12-to-dev.13.md)。`UiOverlay` 从同一实际内容转发布局、命中与焦点；活动模态使用该布局参与根快照与业务 key 定位，见 [dev.13 → dev.14](migrations/dev.13-to-dev.14.md)。模态栈复用弹窗的屏内布局，窄窗及层叠偏移保持页脚可达，见 [dev.14 → dev.15](migrations/dev.14-to-dev.15.md)。
