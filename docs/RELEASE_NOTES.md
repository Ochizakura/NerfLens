# NerfLens 0.2.0-rc3

修复部分自定义配方没有有效物品投入时的 NEI 对照异常，并让源码 ZIP 能生成正确的模组版本号。用于 GTNH 2.9.0，作者：**ochizakura**。本版本为预发布版本。

**[下载 NerfLens-0.2.0-rc3.jar](https://github.com/Ochizakura/NerfLens/releases/download/v0.2.0-rc3/NerfLens-0.2.0-rc3.jar)**

## 本版改动

- 自定义配方启用全部投入变化标记、却没有有效物品投入时，使用配方路线对照卡展示信息。空投入列表、纯流体投入、零数量工具和空槽位均可安全处理。
- 普通物品投入的数量对照继续使用实际配方的副本生成。
- 发布版本在构建脚本中明确声明，源码 ZIP 自行构建时无需 Git 标签，生成的版本号为 `0.2.0-rc3`。
- 验证脚本增加上述边界场景，并检查 NEI 配方页的完整对照注入。

## 安装与使用

1. 关闭客户端，将下载的 JAR 放入正在使用的 GTNH 实例的 `mods` 文件夹。
2. 升级时移出旧版 NerfLens，保留一个 JAR，重新启动游戏。
3. 进入游戏，按 `E` 打开背包，悬停受影响的物品或机器；按住 `Shift` 查看详细说明。
4. 在 NEI 中打开配方或用途页（默认 `R` / `U`），查看已覆盖的配方改动和材料对照。

NerfLens 由 Forge 自动加载，功能显示在物品提示和 NEI 中。绿色 **Code → Download ZIP** 下载的是源码仓库。完整离线包中包含可安装的 JAR、源码、说明与许可证。更多安装、配置和反馈方式见[仓库 README](https://github.com/Ochizakura/NerfLens)。

## 支持与验证

支持 GTNH 2.9.0 / Minecraft 1.7.10 / Forge 1614，根据 ExtraNerf 1.0.1、1.1.0、1.1.1 自动选择相应说明。未知版本使用用户自定义显示数据。提供中英文说明、NEI 配方和材料对照、认证状态提示；Chromatic Tooltips 为可选集成。

构建、格式和 Checkstyle 检查通过。源码附带 35 项版本兼容检查与 16 项认证状态检查；离线集成检查在带 Chromatic 和不带 Chromatic 的环境中分别通过 324 和 308 项断言，其中包含 10 项对照卡边界检查。四个生产 Mixin 的注入、实际模组的数据字段和调用接口已核对，源码 ZIP 在无 Git 元数据的目录中完成构建。

本版本保留预发布标记。完整游戏画面、真实存档中的机器加工和蒸汽耗量读数仍需游戏内验收。

## 许可

代码使用 MIT；源自 ExtraNerf 的显示数据与文档使用 LGPL-3.0-or-later。发布包包含完整许可证和来源说明。

## English

NerfLens 0.2.0-rc3 fixes NEI comparisons for custom display records without effective item inputs, and gives source ZIP builds the correct release version. Author: **ochizakura**. This is a prerelease for GTNH 2.9.0.

For `allInputsChanged` records with empty, fluid-only, zero-count or null inputs, comparison generation provides a route card instead of returning an empty list or throwing. Regular item quantity comparisons use copies of the native ingredients. The build declares its version explicitly and does not require Git tags.

[Download the JAR](https://github.com/Ochizakura/NerfLens/releases/download/v0.2.0-rc3/NerfLens-0.2.0-rc3.jar), replace the older NerfLens JAR in the active client's `mods` directory with the game closed, and restart. Hover affected items in the inventory, hold `Shift` for details, and open NEI recipes or usages (`R` / `U` by default). The server does not need NerfLens.

ExtraNerf 1.0.1, 1.1.0 and 1.1.1 profiles are selected automatically. Unknown versions use custom display data only. Unavailable remote certification state is shown as unknown.

Build, formatting and Checkstyle passed. The shipped checks cover 35 version compatibility and 16 certificate-state assertions. The offline harness passes 324 assertions with Chromatic Tooltips and 308 without it, including 10 comparison-card boundary checks. All four production client Mixins, installed mod interfaces and a build from the source ZIP were checked. Full in-game visuals, real-save machine processing and steam-consumption readouts remain outside these checks.

Code is MIT licensed. ExtraNerf-derived display data and documentation is LGPL-3.0-or-later. Complete license texts and upstream notices are included.
