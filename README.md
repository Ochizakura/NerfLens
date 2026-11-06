# NerfLens

NerfLens 是用于 **GregTech: New Horizons 2.9.0** 的客户端辅助模组，在物品提示和 NEI 配方页中显示 ExtraNerf 的改动、材料对照与认证状态。当前版本：**0.2.0-rc2（预发布）**。作者：**ochizakura**。

**[下载 NerfLens-0.2.0-rc2.jar](https://github.com/Ochizakura/NerfLens/releases/download/v0.2.0-rc2/NerfLens-0.2.0-rc2.jar)** · [发布页与其他文件](https://github.com/Ochizakura/NerfLens/releases/tag/v0.2.0-rc2)

## 安装

1. 关闭 Minecraft 客户端。
2. [下载 NerfLens-0.2.0-rc2.jar](https://github.com/Ochizakura/NerfLens/releases/download/v0.2.0-rc2/NerfLens-0.2.0-rc2.jar)。
3. 将 JAR 放进**正在使用的 GTNH 实例**的 `mods` 文件夹。使用启动器的版本隔离功能时，以该实例的游戏目录为准。
4. 升级时移出旧版 NerfLens，同一实例保留一个 NerfLens JAR。
5. 重新启动游戏；主菜单的 **Mods** 列表中应显示 NerfLens 0.2.0-rc2。

JAR 由 Forge 随游戏自动加载，无须单独运行。NerfLens 安装在客户端，服务器无需安装。

| 下载文件 | 用途 |
| --- | --- |
| `NerfLens-0.2.0-rc2.jar` | 玩家安装使用，将它放入实例的 `mods` 文件夹。 |
| 绿色 **Code → Download ZIP** | 下载源码仓库，供查看或自行编译。 |
| `NerfLens-0.2.0-rc2-source.zip` | 发布版源码、许可证及可复现检查脚本。 |
| `NerfLens-0.2.0-rc2-release.zip` | 离线完整包，包含 JAR、源码、发布说明、验证记录和许可证；安装时取出其中的 JAR。 |

## 在游戏中使用

- **查看改动提示：**进入游戏后，按 `E` 打开背包，将鼠标移到受 ExtraNerf 影响的机器或物品上，查看 `ExtraNerf` 提示。
- **展开详细说明：**保持鼠标悬停，按住 `Shift`，查看完整改动、原版与削弱后的说明及新增材料。
- **查看配方对照：**在 NEI 中搜索目标物品，打开配方页。默认 `R` 查看配方、`U` 查看用途，具体以游戏按键设置为准。已覆盖的配方会显示黄色投入框、金色产物框及页面底部的改动对照。
- **查看认证状态：**悬停适用的机器或证书，查看所需认证与状态；按住 `Shift` 查看各项前置认证。单机可显示已有认证状态，无法取得联机服务器状态时显示“未知”。

这些功能直接显示在背包、物品提示和 NEI 配方页中；当前版本没有独立的 NerfLens 窗口或打开快捷键。

## 支持范围

运行环境为 **GTNH 2.9.0 / Minecraft 1.7.10 / Forge 1614**。已检查的接口包括 NEI 2.8.171-GTNH 和 UniMixins / Mixin 0.8.7；Chromatic Tooltips 1.0.36-GTNH 为可选集成。

同一 JAR 根据 Forge 读取到的 ExtraNerf 版本自动选择说明数据：

| ExtraNerf 版本 | 说明记录 | 配方对照记录 |
| --- | ---: | ---: |
| 1.0.1 | 71 | 123 |
| 1.1.0 | 91 | 140 |
| 1.1.1 | 91 | 143 |

未知版本或未安装 ExtraNerf 时，仅使用用户自定义数据。详细规则见[兼容说明](docs/COMPATIBILITY.md)。

全局金框标记合成材料发生变化的产物，配方中的黄色框和金色框标记相应投入与产物。已有模组或资源包译文优先；仅在缺译且英文含义与参考一致时补充译文。

## 配置与反馈

自定义显示数据放在实例游戏目录的 `config/nerflens/user_nerfs.json` 中，格式和版本限制见[配置说明](docs/COMPATIBILITY.md#user-data)。启动日志中的 `[NerfLens] ExtraNerf=...; data profile=...` 可用于确认当前采用的数据版本。

本版本为预发布版本。自动化验证覆盖版本匹配、提示逻辑、接口兼容与 Mixin 注入；完整游戏画面、真实存档加工和蒸汽耗量读数仍需游戏内核对。若出现显示异常，请在 [Issues](https://github.com/Ochizakura/NerfLens/issues) 附上 GTNH、ExtraNerf 和 NerfLens 版本、目标机器或物品名称，以及截图或相关日志。

NerfLens 提供显示辅助，认证规则、考试、评分和机器实际行为由 ExtraNerf 控制。认证状态读取不创建或保存世界记录。发布说明见[版本说明](docs/RELEASE_NOTES.md)，检查范围见[验证说明](docs/VALIDATION.md)。

## English

NerfLens is a client-side display companion for ExtraNerf in GTNH 2.9.0. It provides version-aware tooltips, NEI recipe comparisons, material-change frames and certificate status. Author: **ochizakura**. Current release: **0.2.0-rc2**, a prerelease.

**[Download NerfLens-0.2.0-rc2.jar](https://github.com/Ochizakura/NerfLens/releases/download/v0.2.0-rc2/NerfLens-0.2.0-rc2.jar)** and place it in the active GTNH instance's `mods` directory with the client closed. Replace older NerfLens versions, keep one JAR and restart the game. Forge loads it automatically; the server does not need it. GitHub's **Code → Download ZIP** contains source code for developers.

Open the inventory (`E` by default) and hover an affected item or machine. Hold `Shift` for details. Use NEI recipe and usage pages (`R` / `U` by default) for supported material frames and comparisons. Hover a machine or certificate for certification requirements and status. These features appear in existing inventory and NEI screens; there is no separate NerfLens window or opening key.

GTNH 2.9.0 / Minecraft 1.7.10 / Forge 1614 is the supported environment. One JAR selects ExtraNerf 1.0.1, 1.1.0 or 1.1.1 data automatically. Unknown or absent ExtraNerf uses custom display data only. Remote or unavailable certificate state is shown as unknown. See [compatibility](docs/COMPATIBILITY.md), [release notes](docs/RELEASE_NOTES.md) and [validation](docs/VALIDATION.md).

## Source builds and checks

Run Gradle with Java 25 and provide the Java 8/17/21/25 toolchains used by the GTNH convention. Build with `gradlew.bat build --no-configuration-cache`, or `./gradlew build --no-configuration-cache` on Unix. Offline builds require cached dependencies.

The release source archive omits local absolute toolchain paths. Configure installed toolchains through Gradle properties or automatic discovery. The `check` task includes the dependency-free compatibility and certificate-state checks under `src/test/java`. The optional [offline integration harness](verification/README.md) explains how to generate a local classpath and use your own GTNH mods.

## Licensing and sources

程序代码使用 **MIT**，保留继承的模板许可声明；源自 ExtraNerf 的显示数据与文档使用 **LGPL-3.0-or-later**。完整许可证及来源说明见[许可说明](docs/LICENSING.md)。

Code: MIT, with the inherited template notice preserved. ExtraNerf-derived display data and documentation: LGPL-3.0-or-later. Dependency JARs and game assets are not redistributed.

Upstream: [GTNH Extra Nerf](https://github.com/kuba6000/GTNH-Extra-Nerf). Naming references include installed translations and [Translation-of-GTNH](https://github.com/Kiwi233/Translation-of-GTNH).
