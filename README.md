# NerfLens

NerfLens is a client-side display companion for ExtraNerf in GregTech: New Horizons 2.9.0. It adds version-aware tooltips, NEI comparisons and material-change frames. Author: **ochizakura**. Release candidate: **0.2.0-rc2**.

## Features

- Selects verified ExtraNerf 1.0.1, 1.1.0 or 1.1.1 data from Forge mod metadata.
- Shows English and Simplified Chinese descriptions, Shift details and NEI item/fluid comparisons.
- Yellow marks changed inputs; local gold marks changed outputs. Global gold follows products whose own material inputs changed.
- Handles exact item identities, cycling alternatives and NBT-sensitive catalysts.
- Integrates optionally with Chromatic Tooltips.
- Existing translations take precedence. Fallbacks apply to missing text only when its English meaning matches the verified reference.
- Supports user overrides in config/nerflens/user_nerfs.json.
- Restores read-only certificate status tooltips. Integrated-server snapshots show unlocked or missing prerequisites; unavailable remote state stays unknown.

NerfLens changes presentation only. Certificate state is read from existing memory on the integrated-server thread; no records are created or saved. Native certification rules, exams, scoring and certificate interfaces remain controlled by ExtraNerf.

## Compatibility

Use GTNH 2.9.0 on Minecraft 1.7.10 / Forge 1614. Tested interfaces are NEI 2.8.171-GTNH and UniMixins/Mixin 0.8.7. Chromatic Tooltips 1.0.36-GTNH is optional.

| ExtraNerf | Descriptions | Compare records |
| --- | ---: | ---: |
| 1.0.1 | 71 | 123 |
| 1.1.0 | 91 | 140 |
| 1.1.1 | 91 | 143 |

Unknown versions and absent ExtraNerf use user data only. See [compatibility](docs/COMPATIBILITY.md).

## Installation

Close the client and replace an older NerfLens JAR with NerfLens-0.2.0-rc2.jar in the client mods directory. Keep one NerfLens JAR. The startup log identifies the selected data version. The server does not need NerfLens.

## Source builds and checks

Run Gradle with Java 25 and provide the Java 8/17/21/25 toolchains used by the GTNH convention. Build with gradlew.bat build --no-configuration-cache, or ./gradlew on Unix. Offline builds require cached dependencies.

The release source archive omits local absolute toolchain paths. Configure installed toolchains through Gradle properties or automatic discovery. The check task includes the dependency-free compatibilityCheck under src/test/java. The offline integration harness under verification explains how to generate a local classpath and use your own GTNH mods.

## Release status

This is a release candidate. Complete client screenshots and real-save processing were not exercised. The reported steam 5:1 display issue was deferred and is not claimed fixed. Certificate status is available in singleplayer; remote servers without a state sync show unknown instead of a false locked state.

## Licensing and sources

Code: MIT, with the inherited template notice preserved. Bundled ExtraNerf-derived data/documentation: LGPL-3.0-or-later. Notices, source and full license texts are included; see [licensing](docs/LICENSING.md). Native dependency JARs and game assets are not redistributed.

Upstream: [GTNH Extra Nerf](https://github.com/kuba6000/GTNH-Extra-Nerf). Naming references include installed translations and [Translation-of-GTNH](https://github.com/Kiwi233/Translation-of-GTNH).

## 中文说明

同一 JAR 自动选择 ExtraNerf 1.0.1、1.1.0 或 1.1.1 的说明。未知版本不套用已知削弱规则，用户配置仍可使用。全局金框只跟随合成材料有变化的产物；已有译文优先，缺译且英文含义一致时才补译。

本包为 0.2.0-rc2 发布候选包，作者 ochizakura。认证状态 tooltip 已恢复：单机读取已有服务器内存快照，无法取得远程状态时显示未知。蒸汽 5:1 显示问题按要求暂时略过，没有声明已修复。
