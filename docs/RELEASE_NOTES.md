# NerfLens 0.2.0-rc2

用于 GTNH 2.9.0 的 ExtraNerf 客户端显示辅助，提供物品改动说明、NEI 配方对照、材料变化标记和认证状态提示。作者：**ochizakura**。本版本为预发布版本。

**[下载 NerfLens-0.2.0-rc2.jar](https://github.com/Ochizakura/NerfLens/releases/download/v0.2.0-rc2/NerfLens-0.2.0-rc2.jar)**

## 安装与使用

1. 关闭客户端，将下载的 JAR 放入正在使用的 GTNH 实例的 `mods` 文件夹。
2. 升级时移出旧版 NerfLens，保留一个 JAR，重新启动游戏。
3. 进入游戏，按 `E` 打开背包，悬停受影响的物品或机器；按住 `Shift` 查看详细说明。
4. 在 NEI 中打开配方或用途页（默认 `R` / `U`），查看已覆盖的配方改动和材料对照。

NerfLens 由 Forge 自动加载，功能显示在物品提示和 NEI 中。绿色 **Code → Download ZIP** 下载的是源码仓库。完整离线包中包含可安装的 JAR、源码、说明与许可证。更多安装、配置和反馈方式见[仓库 README](https://github.com/Ochizakura/NerfLens)。

## 本版功能

- 机器提示显示所需认证及已解锁、缺少或未知状态；`Shift` 显示每项前置认证。
- 证书物品显示该证书自身的认证状态。
- 单机读取已有认证状态；联机状态无法取得时显示“未知”。切换存档或服务器时清理旧状态。
- 根据 ExtraNerf 1.0.1、1.1.0、1.1.1 自动选择相应说明和配方数据。
- 提供中英文说明、材料变化边框及 NEI 对照；已有译文优先。Chromatic Tooltips 为可选集成。

## 支持与验证范围

支持 GTNH 2.9.0 / Minecraft 1.7.10 / Forge 1614。未知 ExtraNerf 版本使用用户自定义数据。

该版本的构建、格式和 Checkstyle 检查通过。源码附带 35 项兼容检查与 16 项认证状态检查；离线集成检查在带 Chromatic 和不带 Chromatic 的环境中分别通过 314 和 298 项断言。完整游戏画面、真实存档加工和蒸汽耗量读数仍需游戏内核对。

## 许可

代码使用 MIT；源自 ExtraNerf 的显示数据与文档使用 LGPL-3.0-or-later。发布包包含完整许可证和来源说明。

## English

NerfLens 0.2.0-rc2 provides version-aware ExtraNerf tooltips, NEI comparisons, material frames and certificate status in GTNH 2.9.0. Author: **ochizakura**. This is a prerelease.

[Download the JAR](https://github.com/Ochizakura/NerfLens/releases/download/v0.2.0-rc2/NerfLens-0.2.0-rc2.jar), place it in the active client's `mods` directory with the game closed, replace older NerfLens versions and restart. Open the inventory and hover affected items; hold `Shift` for details. Open NEI recipes or usages (`R` / `U` by default) for supported comparisons. The server does not need NerfLens.

Machine tooltips show required certificates and their state. Individual certificates show their own status. Singleplayer reads existing certification data; unavailable remote state is shown as unknown, and changing worlds or servers clears stale state. ExtraNerf 1.0.1, 1.1.0 and 1.1.1 profiles are selected automatically. Unknown versions use custom display data only.

Build, formatting and Checkstyle passed for this release. The shipped checks cover 35 compatibility and 16 certificate-state assertions; the offline harness passes 314 assertions with Chromatic Tooltips and 298 without it. Full in-game visuals, real-save processing and steam-consumption readouts remain outside the automated checks.

Program code is MIT licensed. ExtraNerf-derived display data and documentation is LGPL-3.0-or-later. Complete license texts and upstream notices are included.
