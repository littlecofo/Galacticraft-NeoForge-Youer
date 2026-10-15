# 适配与自定义 / Port notes and customization

此分支当前服务于幻影服务器，尚未恢复全部 GC4 内容。可按自己的服务器修改配置或源码。

This fork currently targets the Phantom server. GC4 feature parity is incomplete. Configuration and source can be adapted to your own server.

## 世界规则 / World rules

陨石规则位于 [`MeteorWorldSafety.java`](../fabric/src/main/java/dev/galacticraft/mod/world/dimension/meteor/MeteorWorldSafety.java)：主世界及 `world`、`sc`、`sc2` 禁止实体陨石；`zysj`、`zysj2` 被识别为资源世界。这些名称是幻影服务器的约定，修改后需重新构建。

Meteor rules live in [`MeteorWorldSafety.java`](../fabric/src/main/java/dev/galacticraft/mod/world/dimension/meteor/MeteorWorldSafety.java): physical meteors are blocked in the Overworld and `world`, `sc`, `sc2`; `zysj` and `zysj2` are recognized as resource worlds. These names follow Phantom's setup. Rebuild after changing them.

## 发电设备 / Energy devices

| 设备 / Device | 使用方式 / Behavior |
| --- | --- |
| 地热发电机 / Geothermal generator | 放在喷气孔上方，下方 2–19 格内须有硫酸，竖井保持通畅；默认输出约 30–200/t。岩浆和水不可替代。 / Place above a vapor spout with a clear shaft to sulfuric acid 2–19 blocks below; default output fluctuates around 30–200/t. Lava and water do not work. |
| 太阳能阵列 / Solar array | 同高度水平连接模块与控制器；每组阵列选出一个输出控制器，每秒扫描一次，上限 128 个节点。光照、遮挡、天气与火星沙尘暴影响发电。 / Connect modules and controllers horizontally at the same height. Each array elects one output controller and scans once per second, up to 128 nodes. Light, shade, weather and Martian dust storms affect output. |

两种设备均可为原生电池充电，默认从背面输出能量；支持侧面配置、权限和红石控制。新增设备不会主动强制加载邻接区块。

Both devices charge native batteries and output energy from the back by default. Side configuration, security and redstone controls are supported. New devices do not force-load neighboring chunks.

在 `config/galacticraft.json` 调整以下参数，范围为 `0–100000`，`0` 表示关闭对应发电：

Adjust these keys in `config/galacticraft.json`. Values range from `0–100000`; `0` disables the respective generation:

| 参数 / Key | 默认值 / Default |
| --- | --- |
| `geothermal_generator_max_production_rate` | `200` |
| `solar_array_max_production_rate` | `1000` |

## 验证范围 / Validation

公开源码目录已独立构建，并通过全部 22 项游戏测试。另有本地 Youer 发电、充电、配置保留与客户端资源加载检查。完整联机流程、长期存档及所有 Boss 尚未逐项验收；太阳能阵列也未完全复刻 GC4 外观及所有行星倍率。

The published source layout builds independently and passes all 22 game tests. Additional local checks cover Youer generation, charging, config preservation and client resource loading. Full multiplayer progression, long-term saves and every boss have not been individually accepted; the solar array does not yet reproduce all GC4 visuals or planetary multipliers.

开发测试命令 / Development test command:

```powershell
./build-neoforge.ps1 -Project Galacticraft -Tasks ':neoforge:runStationTests'
```

`galacticraft.stationTests` 与 `galacticraft.restorationTests` 仅用于开发测试；正式服不应启用。运行包已剔除测试类。

`galacticraft.stationTests` and `galacticraft.restorationTests` are development-only switches. Leave them off on live servers. Runtime packages omit test classes.
