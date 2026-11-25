# NerfLens

**NerfLens 0.3.1 正式版**，用于 GregTech: New Horizons 2.9.0 / 2.9.1 的客户端显示辅助模组。作者：ochizakura。

在物品提示与 NEI 配方页中显示 ExtraNerf 的玩法改动、投入与产物对照、材料标记以及认证状态。

## 安装

1. 保存并关闭 Minecraft 客户端。
2. 从更新包取出 `NerfLens-0.3.1.jar`；GitHub 发布列表见 [Releases](https://github.com/Ochizakura/NerfLens/releases)。
3. 升级时移出旧 NerfLens，将新 JAR 放进正在使用的 GTNH 实例 `mods` 目录，每种模组保留一个版本。
4. 重新启动，Mods 列表应显示 NerfLens 0.3.0。

Forge 随游戏加载 JAR。服务器无需安装 NerfLens。

| 文件 | 用途 |
| --- | --- |
| `NerfLens-0.3.1.jar` | 玩家安装使用。 |
| `NerfLens-0.3.1-source.zip` | 源码、许可证及验证脚本。 |
| `NerfLens-0.3.1-release.zip` | JAR、源码、说明与验证记录的完整离线包。 |

## 使用

- 背包中悬停受影响物品查看说明；Shift 展开细节。
- NEI 默认用 R 查看配方、U 查看用途。黄色框标记本配方改动的投入，金色框标记受影响主产物或明确改动的副产物。
- 只有特定路线变化的公共材料使用配方局部标记，未变化的其他路线不会继承金框。
- 悬停适用机器或证书查看认证状态；远程服务器未提供状态时显示“未知”。

例如太阳能级硅的氢气／三氯硅烷路线，悬停右侧氢氯酸可查看副产量 `3,000 → 2,000 mB`，硅粉产量保留。

## 兼容范围

环境：GTNH 2.9.0 / 2.9.1，Minecraft 1.7.10 / Forge 1614（ExtraNerf 1.3.0 声明支持 2.9.1）。已核对 NEI 2.8.171 / 2.8.175 和 Chromatic Tooltips 1.0.36 / 1.0.38（可选）的显示接口。

| ExtraNerf | 提示记录 | 配方对照 |
| --- | ---: | ---: |
| 1.0.1 | 71 | 123 |
| 1.1.0 | 91 | 140 |
| 1.1.1 | 91 | 143 |
| 1.2.0 | 100 | 176 |
| 1.2.1 | 102 | 177 |
| 1.3.0 | 104 | 178 |

1.2.1 档案增加 QFT 的 64 凯芙拉蜂窝投入，将生物培养基的额外奇异粉改为小撮（1/4 份），同步凯芙拉蜂、铁浴端口和无线能源覆盖板认证说明。1.3.0 档案再增加 QFT 两份 0 数量催化（拉多X聚合物粉、凯芙拉粉）、铁浴四金属的完整 GT 部件与分批浇铸、兰波顿超级电容库的激光仓认证，以及任务完成奖励对全队生效。1.2.0、1.2.1 及更早档案逐字节保留。

版本由 Forge 读取并自动选择；未知或未安装 ExtraNerf 时仅使用用户自定义显示数据。配置位于 `config/nerflens/user_nerfs.json`，详见[兼容与配置](docs/COMPATIBILITY.md)。

已有模组、TX Loader、资源包和动态注入的译文优先；补缺只用于缺译且当前英文参考一致的文本。

NerfLens 提供显示辅助。机器行为、加工、考试、评分与认证规则由 ExtraNerf 控制；认证状态读取不创建或保存世界记录。

维护和发布前参照[开发与发布注意表](docs/MAINTENANCE_RULES.md)。表中的验收要求需按每次修改的实际环境执行。

## 构建与验证

使用 Java 25 运行 Gradle，并提供 GTNH 构建约定需要的 Java 8/17/21/25 工具链：

    gradlew.bat build --no-configuration-cache

Unix 使用 `./gradlew`。离线构建需要已缓存依赖。源码 ZIP 无需 Git 元数据即可构建。完整检查脚本见 [verification/README.md](verification/README.md)，结果与边界见[验证说明](docs/VALIDATION.md)。

发布变化见[版本说明](docs/RELEASE_NOTES.md)，已覆盖玩法见[规则目录](docs/NERFS.md)。问题反馈：[GitHub Issues](https://github.com/Ochizakura/NerfLens/issues)。

## English

NerfLens 0.3.1 is a stable client-side display companion for ExtraNerf with verified display APIs in GTNH 2.9.0 and 2.9.1. Use the runtime JAR from the update package; published versions are listed under [Releases](https://github.com/Ochizakura/NerfLens/releases), replace older NerfLens JARs with the client closed, and keep one version in the active instance's mods directory.

Hover items for changes and certificate status; hold Shift for details. NEI recipe pages show changed-input frames, affected-output frames and comparisons. One JAR selects ExtraNerf 1.0.1/1.1.0/1.1.1/1.2.0/1.2.1/1.3.0 data automatically. Existing translations take precedence over fallbacks. Unknown versions use custom display data only.

## 许可与来源

代码 MIT；ExtraNerf 衍生显示数据与文档 LGPL-3.0-or-later。源包和 JAR 保留完整许可与来源，见[许可说明](docs/LICENSING.md)。依赖和游戏资源由玩家的整合包提供。

上游：[GTNH Extra Nerf](https://github.com/kuba6000/GTNH-Extra-Nerf)。译名参考：[Translation-of-GTNH](https://github.com/Kiwi233/Translation-of-GTNH) 与已安装模组汉化。
