# NeoForge / Youer adaptation notes

This is a gradual adaptation, not complete GC4 feature parity.

## Restored energy devices

- `galacticraft:geothermal_generator`: place above a vapor spout, with a clear air shaft to sulfuric acid 2–19 blocks beneath the generator. Default fluctuating output is approximately 30–200 native energy units per tick. Lava and water do not substitute for acid.
- `galacticraft:solar_array_controller` and `galacticraft:solar_array_module`: same-height horizontal connections, one elected output controller per array, a 128-node bound and one-second rescans. Daylight, shade, weather and Martian dust storms affect output. GC4's original OBJ appearance and all planetary solar multipliers are not fully recreated.

Both generators charge native batteries and default to an energy output on the back face.
Native machine menus support side configuration, security and redstone controls.

`config/galacticraft.json` adds `geothermal_generator_max_production_rate` (200) and
`solar_array_max_production_rate` (1000). Zero disables the respective generation;
values are clamped to 0–100000. Existing settings are retained on load.

## Server-specific boundaries

The inherited deployment-specific meteor exclusions for `world`, `sc` and `sc2` remain.
Server owners must review these policies for their own world naming conventions.
Canonical celestial-world mapping, station migration/cargo and rocket protection fixes
remain in this branch. These policies are intentionally documented rather than silently
claiming universal server compatibility.

Existing automatic mining and terraforming need dedicated territory-permission testing.
Future telepad or autopilot ports must honor Bukkit cancellation and actual destination
world identity. No GC4 numeric-dimension-ID compatibility is promised.

## Validation scope

The recorded local candidate passed 22 NeoForge GameTests, an isolated Youer runtime
check, unchanged-existing-config checks and client resource loading. There has not been
complete human multiplayer acceptance of every boss, village, machine screen or long-term
progression path. The public source layout additionally vendors the adapted dependency
sources and changes their build paths. That independent publication layout was then
successfully built locally, including both vendored dependency projects, and passed all
22 GameTests again. See `PUBLICATION-VALIDATION.json`; this is separate from future CI results.

`galacticraft.stationTests` and `galacticraft.restorationTests` are opt-in development
switches. Runtime packaging omits their test classes. Never set them on a live server.
