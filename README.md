# Meteo's UI Kit

Minecraft 1.21.1 客户端 UI 库，Java 21，提供 Fabric 与 NeoForge 产物。公开入口为 `com.meteorite.meteosuikit.api`，内部实现为 `internal`。

## 安装

在 Gradle 仓库中配置 [远程 Maven](https://meteoritel.github.io/meteo-s-ui-kit/) 并限定 group `com.meteorite.meteosuikit`。编译、内嵌和加载器声明使用同一精确版本 `0.1.0-rc1`。

| 模块 | 坐标 | Gradle 配置 |
| --- | --- | --- |
| Common | `com.meteorite.meteosuikit:meteos-ui-kit-common-1.21.1:0.1.0-rc1` | `compileOnly` |
| Fabric | `com.meteorite.meteosuikit:meteos-ui-kit-fabric-1.21.1:0.1.0-rc1` | `modImplementation` 和 `include` |
| NeoForge | `com.meteorite.meteosuikit:meteos-ui-kit-neoforge-1.21.1:0.1.0-rc1` | `implementation` 和 `jarJar` |

- [Gradle 仓库与版本配置](docs/integration.md#固定版本消费)
- [集成与资源所有权](docs/integration.md)
- [版本迁移索引](docs/migrations/README.md)
- [工具与验证](tools/README.md)
- [MIT 许可](LICENSE.txt)

纯样例由 NeoForge 开发源集运行，按 F8 打开展示，Shift+F8 打开原版容器组合；正式产物只包含 main。
在展示页按 F9 截图、F6 换肤。展示控制区可见时，G 切换网格，R 重载资源。
