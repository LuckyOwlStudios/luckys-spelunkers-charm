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
- Locomotive: a new minecart with an engine and a lever on its side, crafted like a minecart with a blast furnace in the middle. Right-click it to open its fuel slot (anything that burns in a furnace works, and lasts twice as long). Shift-right-click to pull the lever and reverse its direction. It drives the way it was facing when placed, is faster than a furnace minecart, and pulls any carts coupled to it.
- Furnace minecarts now have a screen showing how much fuel they have left. Right-click one with an empty hand to see it.
- Minecart with Jukebox: shift-right-click a jukebox onto a minecart. Put a music disc in and the music follows the cart. Right-click again to take the disc out. Detector rails give the same comparator signal as a jukebox.
- Cart Coupler: click one minecart, then another, to couple them with a chain. Coupled carts stay together, so a locomotive can pull a whole train. Each cart can couple to two others, one in front and one behind. Sneak-click a cart to uncouple it. Crafted from two iron ingots and a chain.
- Lanterns on minecarts: right-click any minecart with a lantern or soul lantern to hang it on the cart. The cart lights up the tunnel as it rides, and glows smoothly with LambDynamicLights installed. Use shears to take the lantern off. Sneak-clicking still puts lanterns inside the cart as blocks.
- Rail crossings: right-click a straight rail with another rail while facing across it to make a crossing. Carts go straight through in either direction. Breaking it gives both rails back.
- Ejector Rail: when powered, throws riders out of passing minecarts. Put a slime block under it to launch them into the air instead. Crafted like an activator rail, with a slime ball in the middle and redstone at the bottom.
- Wooden Rail: a cheap early rail made from sticks and planks. Carts are much slower on it, and a locomotive is too heavy and breaks it.
- Create and Supplementaries are now listed as optional dependencies. Create adds zinc boulders; Supplementaries lets you tie a boulder to a rope and suspend it in the air, just like a chain. Other rope blocks can be added with the `spelunkers_charm:boulder_ropes` block tag.
- Tremors shake your camera when [Camera Overhaul](https://modrinth.com/mod/cameraoverhaul) is installed. The shake builds and fades with the tremor, is stronger for bigger tremors and weaker the farther away you are. Camera Overhaul is optional. It can be turned off or scaled with `tremorCameraShake` and `tremorCameraShakeStrength` in the client config.

**Fixed:**
- Putting a chest, hopper, furnace or TNT into a minecart now uses up the block. It used to be free.

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
