# Galacticraft — Unofficial NeoForge / Youer Branch

**非官方分支 · Minecraft 1.21.1 · Java 21 · NeoForge / Youer**

基于 [Team Galacticraft](https://github.com/TeamGalacticraft/Galacticraft) 和
[Colin Vaughn 的社区多加载器分支](https://github.com/ColinVaughn/Galacticraft-1.21.1)
继续适配 NeoForge 与 Youer，并逐步恢复 GC4 本体缺少的玩法。
本项目不属于官方发行，也未获得上述团队的官方背书；保留原作者署名与 MIT 许可证。

This is an **unofficial** continuation of the community Galacticraft 1.21.1 port,
focused on NeoForge and Youer compatibility. It is not an official Galacticraft release.

## 当前状态

- 当前候选：`5.4.15-prealpha-youer.11`，仍处于预发布阶段。
- 编译与游戏测试环境：NeoForge `21.1.255`；本地 Youer 环境使用 NeoForge `21.1.252`。
- 保留空间站零重力、返航货物、天体存档映射、火箭保护和发动机声音等适配修复。
- 第一批 GC4 内容恢复：地热发电机、太阳能阵列模块与控制器。
- 原有月球/火星/金星 Boss、月球村庄、氧气系统等仍在，不能将“已有源码”视为完整玩法验收。
- 发射控制器/自动航线、传送台、能量束、抓钩等仍待恢复；详见 [GC4 对照清单](docs/GC4对照与恢复次序.txt)。

截至 2026-10-15，本地通过 22 项游戏测试、Youer 实际发电/充电测试与隔离客户端资源加载检查。
这些结果不代表所有服务器插件、地形、联机战斗和长期存档都已完整验收。
新发电设备与区块边缘输出均检查加载状态，不主动生成或强制加载邻接区块。
记录见 [本地验证摘要](docs/LOCAL-VALIDATION-20261015.json)。整理后的独立发布目录也已完成三工程源码构建与全部 22 项游戏回归，见 [独立构建验证](docs/PUBLICATION-VALIDATION.json)；云端构建结果另见 GitHub Actions。

## 安装

客户端与服务端安装同版 Galacticraft 主 Jar，移除重复旧版。
外部依赖为 Architectury `13.0.6` 与 BadPackets NeoForge `0.8.2`。
适配后的 MachineLib 与 DynamicDimensions 已内嵌；其修改后的源码也在本仓库 `vendor/` 中。

此 Mod 放在 `mods/`，不适用于纯 Paper 的 `plugins/`。
升级前备份存档，并在独立实例测试。不要将开发测试开关用于正式服务器。

## 从源码构建

需要 Java 21、Python 3 与 PowerShell 7。三个适配工程的源码均已包含，不需要访问维护者的私人磁盘或服务器。

```powershell
./build-neoforge.ps1
python scripts/package_release.py
```

构建依次处理 `vendor/DynamicDimensions`、`vendor/MachineLib` 和主 Mod。
最终运行文件与校验值在 `dist/`。Gradle 使用固定版本的项目 wrapper。
公开依赖 BadPackets 会从原发布者的 Modrinth 文件下载并核对 SHA-512，缓存不进入版本库。

运行空间站、陨石保护及本批恢复回归：

```powershell
./build-neoforge.ps1 -Project Galacticraft -Tasks ':neoforge:runStationTests'
```

只对 NeoForge / Youer 路线提供本分支构建验证；保留的 Fabric 代码不代表同步验收。
更多限制、配置与内容恢复范围见 [适配说明](docs/PORT-NOTES.md)。

## 来源与许可证

- Galacticraft：Team Galacticraft；社区分支：Colin Vaughn。
- MachineLib、DynamicDimensions：各自原作者，许可证保留在对应 `vendor/` 目录。
- 本次恢复没有直接复制 GC4 的旧 GPL 源码或旧模型；GC4 仅用作玩法对照参考。
- 来源提交记录：[SOURCE-PROVENANCE.json](SOURCE-PROVENANCE.json)。
- [MIT LICENSE](LICENSE) 与 [署名说明](docs/ATTRIBUTION.md)。
- 上游原说明保留在 [UPSTREAM_README.md](docs/UPSTREAM_README.md)。

本仓库不包含正式服务器世界、玩家数据、账户凭据或服务器插件配置。
