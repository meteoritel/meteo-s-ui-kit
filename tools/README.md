# 库工具入口

- 源码检查：`python -B tools/check-all.py`。
- 完整构建：`./gradlew.bat build`，包含 Interface、JavaDoc 与独立样例编译。
- Interface：`:common:exportUiKitInterface`；批准变化后加 `-PkitInterfaceMode=write` 更新基线。
- 样例：`:neoforge:runSamplesClient`，仅开发源集带有 F8 入口与演示文案。

构建与发布串行执行。游戏行为由用户验收，源码与产物检查分别记录。

Interface 门禁核对有效公开声明及源码引用；仅共享 internal preview Mixin 允许使用对应 Sponge 注解，公开依赖白名单保持独立。dev 兼容与迁移约定见 [接入说明](../docs/integration.md#固定版本消费)。

## 本地发布和消费校验

用户级 `gradle.properties` 设置 `meteos_ui_kit_maven_dir`，指向专用 Maven 仓库。普通库构建无需此属性；发布和消费校验使用该属性。

```powershell
./gradlew.bat publishLibrary
python -B tools/check-publication.py --repository <仓库路径>
./gradlew.bat -p tools/consumer-check compileJava
```

`publishLibrary` 先执行完整构建；每个坐标的版本目录已存在时拒绝覆盖。发布输入改变时提升 `gradle.properties` 的 version；部分发布失败时只补发尚未存在的子项目坐标，保留已发布目录。运行产物、sources、JavaDoc、POM 与 Gradle module metadata 一起发布。

`check-publication.py` 核对发布附件与构建产物一致、元数据摘要、依赖隔离及 Fabric intermediary/refmap。独立 `consumer-check` 只使用 Maven 坐标和各端游戏映射编译同一份纯样例，拒绝库工程输出进入 classpath。该工程不使用源码替代。

消费者 common 使用 common 编译产物；Fabric 使用 remap 后的平台产物，NeoForge 使用 Mojang 平台产物。平台产物已经包含通用实现。升级库时先发布新版本，再调整消费者精确版本并构建各自内嵌归档。

恢复本地仓库时，从对应版本的源码 checkout 构建并发布到空的目标坐标目录；其他坐标保留。
