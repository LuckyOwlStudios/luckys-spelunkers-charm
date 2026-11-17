# Changelog

All notable changes to Lucky's Spelunker's Charm are listed here.
Every pull request adds its entry under **Work In Progress**. When a version is released,
that section becomes the new version (with a row in the table below), and a new empty
Work In Progress section is started.

# Navigation
(Click on version number to jump to its changelog.)

| Version                               | Release Date |
|---------------------------------------|--------------|
| [Work In Progress](#work-in-progress) | `n/a`        |
| [5.1.1](#511)                         | `2026-11-08` |
| [5.0.6](#506)                         | `2025-11-25` |

# Work In Progress

**Added:**
- Create and Supplementaries are now listed as optional dependencies. Create adds zinc boulders; Supplementaries lets you tie a boulder to a rope and suspend it in the air, just like a chain. Other rope blocks can be added with the `spelunkers_charm:boulder_ropes` block tag.
- Tremors shake your camera when [Camera Overhaul](https://modrinth.com/mod/cameraoverhaul) is installed. The shake builds and fades with the tremor, is stronger for bigger tremors and weaker the farther away you are. Camera Overhaul is optional. It can be turned off or scaled with `tremorCameraShake` and `tremorCameraShakeStrength` in the client config.

**Changed:**
- Spelunker's Charm now requires [Sable](https://modrinth.com/mod/sable) 2.0.6 or newer and NeoForge 21.1.228 or newer.
- Lifts are now a single solid block you can stand and build on (3×3, 5×5 or 7×7), drawn with the lift model. The lift and anything built on it ride the lift tracks as one Sable physics structure. Right-click with an empty hand to start or stop it, and sneak-right-click to reverse. Use planks or a mineral on it to change its wood or metal. The gears still turn and spark while it moves. Breaking any part breaks the whole lift and gives the lift item back. It stops when a block is in the way of the lift or anything built on it.
- Lifts can be placed without tracks: on the floor, or against a single lift track (the lift goes on the side the track faces). While holding a lift, its spot and track spots are outlined. While holding a lift or a lift track, placed lifts outline their missing track spots (green: track in place, yellow: track missing or facing the wrong way). A lift starts running as soon as all its tracks are in.
- A lift that runs off the end of its tracks, or loses them, falls and tumbles like any other loose structure.

**Development:**
- Camera Overhaul (with Cloth Config, which it needs) now loads in dev runs (`runClient`), so tremor camera shake can be tested without installing it by hand. It isn't bundled in the mod jar and stays an optional dependency.

# 5.1.1

**Added:**
- Boulders for modded ores: zinc (Create), osmium (Mekanism), and tin and lead (Mekanism, Thermal, Immersive Engineering, Oritech and others). They only generate when a mod that adds the ore is installed, and are hidden from the creative tab otherwise. Mining one with silk touch drops the raw ore block. ([#28](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/28))
- Config toggles for each modded ore boulder under `worldGeneration.moddedBoulders`. ([#28](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/28))
- A `tremors` config section: how often tremors are checked for, spawn chance near the surface and deep down, length, strength, warning and ramp time, effect radius, falling rock chance and limits, and explosion settings. ([#26](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/26))
- An `activeGeysers` config option. ([#27](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/27))
- A proper README with the mod's features, requirements and full config reference. ([#31](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/31))

**Changed:**
- Tremors are rarer and depend on depth: a 1% chance at Y=64 and above, rising to 15% at the bottom of the world, checked every 10 minutes (was a flat 20% every 5 minutes). Natural tremors always give a warning rumble. ([#26](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/26))
- Tremors now build up and fade out instead of starting and stopping at full strength. ([#26](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/26))
- Overlapping tremors keep the stronger one instead of adding together, and are capped in strength and length. ([#26](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/26))
- Active tremors are saved and carry on after leaving and rejoining the world. ([#26](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/26))
- Geysers are much cheaper on performance: they no longer update their block every tick while erupting, and their particles are spawned on the client. Ambient steam respects the particle setting. ([#27](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/27))
- The candle helmet's flame and smoke now come from the candle wick and follow the head when looking around or crouching. ([#29](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/29))
- Refreshed the mining helmet and candle helmet textures, both the item icons and the worn armor. ([#30](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/30))
- Generated boulders keep the natural cave floor around them instead of painting a 5x5 patch of moss, cobblestone or ice. They only fill gaps underneath so they don't hang over holes. ([#28](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/28))
- Player-placed and falling boulders now have the same shape as naturally generated ones. ([#28](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/28))
- Permafrost, Dunestone and Wildstone (experimental, `stoneReplacers`) now generate as part of the terrain in cold, hot and jungle biomes, replacing stone cleanly instead of in patches. ([#32](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/32), [#36](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/36))
- Modded ore boulders now show the actual raw ore block texture from the mod that adds the ore (for example Create's raw zinc block) on their cut face, so the inside matches the block in the middle. Our own painted texture is used when that texture can't be found. ([#37](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/37))

**Fixed:**
- The `boulders` and `rocks` config options did nothing. Boulders shared the geysers setting, and rock piles ignored their toggle. ([#24](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/24), [#23](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/issues/23))
- Spider caves next to water dropped hundreds of floating string items. Web veins can now be waterlogged and are no longer washed away. ([#24](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/24))
- Web veins spreading placed sculk veins instead of web veins. ([#24](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/24))
- Spider egg model showing stray pixels and see-through faces. ([#24](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/24))
- Log spam from leftover debug messages ("Tick", "Placing trunk"). ([#25](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/25), [#19](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/issues/19))
- Tremors dropping far too many rocks. Rocks now fall less often, are limited per tremor and nearby, and only fall near players. ([#26](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/26), [#21](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/issues/21))
- The icy tremor sound never playing at its intended interval. ([#26](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/26))
- Tremors not setting off dormant floor geysers. ([#27](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/27))
- Flint and steel and fire charges being used up on a geyser that couldn't erupt. ([#27](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/27))
- Water geysers not pushing players. ([#27](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/27))
- Copper and gold boulders saving with the wrong type name. ([#28](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/28))
- Falling boulders turning into plain stone boulders after the world was reloaded. ([#28](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/28))

**Removed:**
- The geyser's `duration` block state. Existing worlds load fine. ([#27](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/27))
- The per-type boulder structure files, now replaced by code. ([#28](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/28))
- The old patch-based Permafrost, Dunestone and Wildstone generation, replaced by the terrain version above. ([#32](https://github.com/LuckyOwlStudios/luckys-spelunkers-charm/pull/32))

# 5.0.6
- Last release before this changelog was started.
