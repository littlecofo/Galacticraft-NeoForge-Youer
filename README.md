# Galacticraft · 幻影服务器适配版

**当前版本为幻影服务器定制的适配预览版，尚非正式版。计划在适配完善后发布通用正式版；本项目始终是 Galacticraft 的非官方分支。**

**This is a preview tailored to the Phantom server, not a stable release. A general-purpose stable release is planned as the port matures. This project remains an unofficial Galacticraft fork.**

**服务器交流 QQ 群 / Server community QQ group：938200184**

[下载 Mod / Download](https://github.com/littlecofo/Galacticraft-NeoForge-Youer/releases) · [适配详情 / Port notes](docs/PORT-NOTES.md) · [GC4 恢复进度 / GC4 roadmap](docs/GC4对照与恢复次序.txt)

## 项目介绍 / About

基于 Team Galacticraft 与 Colin Vaughn 的社区移植分支，为 Minecraft 1.21.1 的 NeoForge / Youer 环境继续适配星系玩法，重点处理幻影服务器的多世界、空间站、火箭返航和建筑保护，并逐步恢复 GC4 内容。

Built on Team Galacticraft's work and Colin Vaughn's community port, this fork brings Galacticraft to Minecraft 1.21.1 on NeoForge / Youer. It focuses on Phantom's multiple worlds, space stations, rocket return trips and build protection, while gradually restoring GC4 features.

## 特别适配 / Server adaptations

| 中文 | English |
| --- | --- |
| **NeoForge / Youer**：适配加载器接口及机器能量、流体交互；随包内嵌适配后的 MachineLib 与 DynamicDimensions。 | **NeoForge / Youer:** adapts loader interfaces and machine energy/fluid interaction, with adapted MachineLib and DynamicDimensions bundled. |
| **多世界与空间站**：修正天体世界与存档的对应关系，保留空间站迁移与零重力修复。 | **Worlds and stations:** fixes celestial world/save mapping and preserves station migration and zero-gravity fixes. |
| **火箭与返航**：保留火箭保护、发动机声音，以及返航货物、燃料和发射台回收修复。 | **Rockets and return trips:** preserves rocket protection, engine audio, and fixes for returning cargo, fuel and launch-pad recovery. |
| **建筑世界保护**：禁止陨石进入主世界及 `world`、`sc`、`sc2`；按 `zysj`、`zysj2` 识别资源世界。 | **Build-world protection:** blocks physical meteors in the Overworld and worlds named `world`, `sc` and `sc2`; recognizes `zysj` and `zysj2` as resource worlds. |
| **GC4 发电设备**：恢复地热发电机、太阳能阵列模块与控制器，支持电池充电、侧面配置及红石控制。 | **GC4 energy devices:** restores the geothermal generator, solar array modules and controller, with battery charging, side configuration and redstone control. |
| **服务器负载**：新增发电设备只检查已加载区块；阵列限制扫描规模，不主动强制加载邻接区块。 | **Server load:** new generators inspect loaded chunks only; array scans are bounded and do not force-load neighboring chunks. |

**可以自行修改。** 发电上限等参数可在 `config/galacticraft.json` 调整；世界名称和陨石保护名单目前写在源码中，可按自己的服务器修改后重新构建。修改位置见[适配详情](docs/PORT-NOTES.md)。源码按 MIT 许可证开放，允许修改与再发布，须保留许可证和版权声明。

**You can customize this fork.** Adjust generation limits in `config/galacticraft.json`. World names and meteor protection rules currently live in source and can be changed before rebuilding. See the [port notes](docs/PORT-NOTES.md) for locations. The MIT license permits modification and redistribution with the license and copyright notices retained.

## 安装 / Installation

| 项目 / Item | 要求 / Requirement |
| --- | --- |
| Minecraft / Java | `1.21.1` / `21` |
| 加载器 / Loader | NeoForge；构建测试 `21.1.255`，本地 Youer 验证 `21.1.252` / NeoForge; built and tested on `21.1.255`, locally checked on Youer with `21.1.252` |
| 外部依赖 / Dependencies | Architectury `13.0.6`、BadPackets NeoForge `0.8.2` |

从 Releases 下载主 Jar，客户端与服务端均放入 `mods/`，并移除旧版。更新前备份存档。

Download the main Jar from Releases and place the same version in both client and server `mods/` folders. Remove older versions and back up your world before updating.

## 构建 / Build

需要 Java 21、Python 3 与 PowerShell 7。主 Mod 和两个适配依赖的源码均已包含。

Requires Java 21, Python 3 and PowerShell 7. Sources for the mod and both adapted dependencies are included.

```powershell
./build-neoforge.ps1
python scripts/package_release.py
```

成品与 SHA-256 校验文件位于 `dist/`。/ The packaged Jar and SHA-256 checksums are written to `dist/`.

本地已通过 22 项游戏测试、Youer 发电与充电检查及客户端资源加载检查。GC4 内容尚未全部恢复；验证记录见[独立构建验证](docs/PUBLICATION-VALIDATION.json)与[本地验证](docs/LOCAL-VALIDATION-20261015.json)。

Local validation passed 22 game tests, Youer generation/charging checks and client resource loading. GC4 feature parity is incomplete. See the [publication build](docs/PUBLICATION-VALIDATION.json) and [local validation](docs/LOCAL-VALIDATION-20261015.json) records.

## 来源 / Credits

原项目 / Original project：[Team Galacticraft](https://github.com/TeamGalacticraft/Galacticraft)；社区移植 / Community port：[Colin Vaughn](https://github.com/ColinVaughn/Galacticraft-1.21.1)；本分支适配 / Fork adaptation：littlecofo。

[MIT License](LICENSE) · [署名与依赖许可 / Attribution](docs/ATTRIBUTION.md) · [源码来源 / Source provenance](SOURCE-PROVENANCE.json) · [上游说明 / Upstream README](docs/UPSTREAM_README.md)
