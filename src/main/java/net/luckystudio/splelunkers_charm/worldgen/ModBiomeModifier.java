package net.luckystudio.splelunkers_charm.worldgen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.datagen.biomes.biomeTags.ModBiomeTags;
import net.luckystudio.splelunkers_charm.worldgen.feature.placement.ModCavePlacements;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * Spelunker's Charm overrides a huge chunk of minecraft's cave generation.
 * We completely replace stone with biome specific blocks,
 * and we even replace most ore generation with biome specific ores (of course, mimicking its vanilla counterparts).
 *
 * This could, in theory, cause issues with other mods that add new biomes, ore generation, and even vanilla minecraft's core worldgen functionality.
 * To combat this, we make the stone replacement happen after minecraft's stone and ore generation are done. This way, our stone replacement features will load after
 * and replace not only the stones, but the ores as well. Why? Because we then replace the ores with our own, which are biome specific ore counterparts.
 *
 * The ores we replace are in a tag. This way our mod only replaces the ores we have counterparts for, that way modded ores we don't have support for stay generated.
 * Sometimes my genius is almost... frightening.
 */

public class ModBiomeModifier {
    public static final ResourceKey<BiomeModifier> OVERWORLD_UNDERGROUND_DECORATION = registerKey("overworld_underground_decoration");

    public static final ResourceKey<BiomeModifier> ICE_CAVE_UNDERGROUND_DECORATION = registerKey("ice_cave_vegetal_decoration");

    public static final ResourceKey<BiomeModifier> DESERT_CAVE_UNDERGROUND_DECORATION = registerKey("desert_cave_vegetal_decoration");

    public static final ResourceKey<BiomeModifier> JUNGLE_CAVE_UNDERGROUND_DECORATION = registerKey("jungle_cave_vegetal_decoration");

    public static final ResourceKey<BiomeModifier> NETHER_UNDERGROUND_DECORATION = registerKey("nether_vegetal_decoration");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        // We register all rocks to spawn in everywhere in the overworld because we want our rocks to be compatible with modded biomes, the ConfiguredFeature will handle the filtering
        context.register(OVERWORLD_UNDERGROUND_DECORATION, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.DEEPSLATE_LAVA_GEYSER),
                        placedFeatures.getOrThrow(ModCavePlacements.CLAY_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.ROCK_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.DEEPSLATE_ROCK_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.DRIPSTONE_ROCK_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.PATCH_CAVE_MUSHROOM),
                        placedFeatures.getOrThrow(ModCavePlacements.PATCH_SHORT_UNDERGROWTH),
                        placedFeatures.getOrThrow(ModCavePlacements.PATCH_TALL_UNDERGROWTH),
                        placedFeatures.getOrThrow(ModCavePlacements.BOULDERS)
                )), GenerationStep.Decoration.UNDERGROUND_DECORATION));

        context.register(ICE_CAVE_UNDERGROUND_DECORATION, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(ModBiomeTags.IS_COLD_CAVE),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_COAL_UPPER),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_COAL_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_IRON_UPPER),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_IRON_MIDDLE),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_IRON_SMALL),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_GOLD),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_GOLD_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_REDSTONE),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_REDSTONE_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_DIAMOND),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_DIAMOND_MEDIUM),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_DIAMOND_LARGE),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_DIAMOND_BURIED),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_LAPIS),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_LAPIS_BURIED),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_EMERALD),
                        placedFeatures.getOrThrow(ModCavePlacements.PERMAFROST_ORE_COPPER),

                        placedFeatures.getOrThrow(ModCavePlacements.ORE_SILT),
                        placedFeatures.getOrThrow(ModCavePlacements.ICE_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.LARGE_ICICLE),
                        placedFeatures.getOrThrow(ModCavePlacements.ICICLE_CLUSTER),
                        placedFeatures.getOrThrow(ModCavePlacements.BOTTOM_ICE_SHEET)
                )), GenerationStep.Decoration.UNDERGROUND_DECORATION));

        context.register(DESERT_CAVE_UNDERGROUND_DECORATION, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(ModBiomeTags.IS_HOT_CAVE),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_COAL_UPPER),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_COAL_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_IRON_UPPER),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_IRON_MIDDLE),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_IRON_SMALL),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_GOLD),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_GOLD_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_REDSTONE),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_REDSTONE_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_DIAMOND),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_DIAMOND_MEDIUM),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_DIAMOND_LARGE),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_DIAMOND_BURIED),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_LAPIS),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_LAPIS_BURIED),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_EMERALD),
                        placedFeatures.getOrThrow(ModCavePlacements.DUNESTONE_ORE_COPPER)
                )), GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(JUNGLE_CAVE_UNDERGROUND_DECORATION, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(ModBiomeTags.IS_JUNGLE_CAVE),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_COAL_UPPER),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_COAL_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_IRON_UPPER),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_IRON_MIDDLE),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_IRON_SMALL),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_GOLD),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_GOLD_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_REDSTONE),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_REDSTONE_LOWER),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_DIAMOND),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_DIAMOND_MEDIUM),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_DIAMOND_LARGE),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_DIAMOND_BURIED),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_LAPIS),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_LAPIS_BURIED),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_EMERALD),
                        placedFeatures.getOrThrow(ModCavePlacements.WILDSTONE_ORE_COPPER),
                        placedFeatures.getOrThrow(ModCavePlacements.NEW_LUSH_VEGETATION),
                        placedFeatures.getOrThrow(CavePlacements.LUSH_CAVES_CEILING_VEGETATION)
                )), GenerationStep.Decoration.UNDERGROUND_DECORATION));

        context.register(NETHER_UNDERGROUND_DECORATION, new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.BASALT_ROCK_PILE)
                )), GenerationStep.Decoration.UNDERGROUND_DECORATION));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SpelunkersCharm.id(name));
    }
}
