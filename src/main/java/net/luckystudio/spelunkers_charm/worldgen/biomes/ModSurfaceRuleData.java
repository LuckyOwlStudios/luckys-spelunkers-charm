package net.luckystudio.spelunkers_charm.worldgen.biomes;

import net.luckystudio.spelunkers_charm.datagen.biomes.biomeTags.ModBiomeTags;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.worldgen.biomes.surface.BiomeTagCondition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class ModSurfaceRuleData {
    private static final SurfaceRules.RuleSource GRASS_BLOCK = makeStateRule(Blocks.GRASS_BLOCK);
    private static final SurfaceRules.RuleSource DIRT = makeStateRule(Blocks.DIRT);

    // STONE_BLOCKS
    private static final SurfaceRules.RuleSource STONE = makeStateRule(Blocks.STONE);

    public static SurfaceRules.RuleSource cave() {
        SurfaceRules.ConditionSource isAtOrAboveWaterLevel = SurfaceRules.waterBlockCheck(-1, 0);
        SurfaceRules.RuleSource grassSurface = SurfaceRules.sequence(SurfaceRules.ifTrue(isAtOrAboveWaterLevel, GRASS_BLOCK), DIRT);

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SPIDER_CAVE), STONE),

                // Default to a grass and dirt surface
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, grassSurface)
        );
    }

    /**
     * Biome stones (Permafrost, Dunestone, Wildstone).
     * These rules are added to the default overworld surface rules, so they run in the SURFACE step on every block of
     * base stone, before carvers, ores and features. That means the stone is part of the terrain itself instead of a
     * feature painting over it afterward.
     *
     * - Only below the preliminary surface, so the top layers (sand, grass, terracotta, exposed mountain stone) stay vanilla.
     * - Only where vanilla would NOT place deepslate. We use the same "deepslate" gradient as vanilla, so the
     *   stone to deepslate transition around y 0 to 8 looks exactly like the vanilla one.
     * - The biome is checked per block, so cave biomes (lush caves, dripstone caves, ...) under a desert keep their own stone.
     */
    public static SurfaceRules.RuleSource biomeStones() {
        SurfaceRules.ConditionSource belowSurface = SurfaceRules.not(SurfaceRules.abovePreliminarySurface());
        SurfaceRules.ConditionSource aboveDeepslate = SurfaceRules.not(
                SurfaceRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)));

        return SurfaceRules.ifTrue(belowSurface, SurfaceRules.ifTrue(aboveDeepslate, SurfaceRules.sequence(
                SurfaceRules.ifTrue(BiomeTagCondition.of(ModBiomeTags.IS_COLD_CAVE), makeStateRule(ModBlocks.PERMAFROST.get())),
                SurfaceRules.ifTrue(BiomeTagCondition.of(ModBiomeTags.IS_HOT_CAVE), makeStateRule(ModBlocks.DUNESTONE.get())),
                SurfaceRules.ifTrue(BiomeTagCondition.of(ModBiomeTags.IS_JUNGLE_CAVE), makeStateRule(ModBlocks.WILDSTONE.get()))
        )));
    }

    private static SurfaceRules.RuleSource makeStateRule(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
