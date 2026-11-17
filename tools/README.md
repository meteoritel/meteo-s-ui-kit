# 库工具入口

需要 Java 21 与 Python 3.11+。Gradle 默认调用 `python`，可用 `-Prepository_python=python3` 或可执行文件绝对路径覆盖。Gradle 经 `tools/run-check.py` 在载入脚本前检查版本；直接调用检查脚本也使用 Python 3.11+。

- 源码检查：`python -B tools/check-all.py`。
- 完整构建：`./gradlew.bat build`，包含 Interface、JavaDoc、公共行为 CLI 与独立样例编译。
- Interface：`:common:exportUiKitInterface`；批准变化后加 `-PkitInterfaceMode=write` 更新基线。
- 行为：`./gradlew.bat :common:checkUiKitBehavior`，编译 `common/src/contractTest/java` 的真实契约源集，验证主题、几何、输入、滚动、文本、提示、根管线与灯箱。用 `-PkitContract=tooltip` 单跑一个契约；资源正负例仍在系统临时目录生成。该源集不参与发布。
- 样例：`:neoforge:runSamplesClient`，仅开发源集带有 F8 入口与演示文案。

构建与发布串行执行。游戏行为由用户验收，源码与产物检查分别记录。

Interface 门禁核对有效公开声明及源码引用；仅共享 internal preview Mixin 允许使用对应 Sponge 注解，公开依赖白名单保持独立。dev 兼容与迁移约定见 [接入说明](../docs/integration.md#固定版本消费)。

## 本地发布和消费校验

用户级 `gradle.properties` 设置 `meteos_ui_kit_maven_dir`，指向专用 Maven 仓库。普通库构建无需此属性；发布和消费校验使用该属性。

```powershell
./gradlew.bat publishLibrary
python -B tools/check-publication.py --repository <仓库路径>
./gradlew.bat -p tools/consumer-check compileJava "-Pmeteos_ui_kit_version=<精确版本>"
```

`publishLibrary` 先执行完整构建；每个坐标的版本目录已存在时拒绝覆盖。发布输入改变时提升 `gradle.properties` 的 version；部分发布失败时只补发尚未存在的子项目坐标，保留已发布目录。运行产物、sources、JavaDoc、POM 与 Gradle module metadata 一起发布。

`check-publication.py` 核对发布附件与构建产物一致、元数据摘要、依赖隔离及 Fabric intermediary/refmap。独立 `consumer-check` 只使用 Maven 坐标和各端游戏映射编译同一份纯样例，拒绝库工程输出进入 classpath。该工程不使用源码替代。

消费者 common 使用 common 编译产物；Fabric 使用 remap 后的平台产物，NeoForge 使用 Mojang 平台产物。平台产物已经包含通用实现。升级库时先发布新版本，再调整消费者精确版本并构建各自内嵌归档。

恢复本地仓库时，从对应版本的源码 checkout 构建并发布到空的目标坐标目录；其他坐标保留。

## 本地供应恢复

源码版本、kit dev 坐标与宿主精确 pin 一起保存。发布前审阅公开面和迁移说明，运行源码检查；`publishLibrary` 内的完整构建校验行为、JavaDoc、样例和平台产物，然后再核对 publication 和独立消费。资源门禁与资源契约从 `ui-kit-interface/host-namespaces.txt` 读取已知宿主资源命名空间，作为单一事实源检查本地化 key 和资源构造。Java 包前缀规则独立表达类型依赖。

恢复时使用对应源码 checkout，保持版本不变，通过命令行覆盖本地 Maven 路径。以下 `D:/tmp/kit-maven` 为新的空仓库，`D:/tmp/kit-gradle` 为独立 Gradle 用户目录；替换为本机路径后串行执行：

```powershell
python -B tools/check-all.py
./gradlew.bat publishLibrary -Pmeteos_ui_kit_maven_dir=D:/tmp/kit-maven
python -B tools/check-publication.py --repository D:/tmp/kit-maven
./gradlew.bat -g D:/tmp/kit-gradle -p tools/consumer-check compileJava -Pmeteos_ui_kit_maven_dir=D:/tmp/kit-maven
```

独立消费只从指定 Maven 仓库解析 kit，Gradle 的 Wrapper 和插件仍按工程声明供应。以空 Maven 仓库和新的依赖缓存消费应因缺失坐标失败；同坐标再次 publish 应拒绝覆盖。首次供应流程变化执行这些负例，常规升级复用发布与消费门禁。

开发样例的 Models 页提供表单入口和受控行绘制异常入口；异常入口核对 pose 后在列表外绘制恢复提示，用户同时核对裁剪已恢复及页脚可见。

当前供应流程使用本地专用 Maven。远端源码和制品供应由消费者 CI 单独配置。
