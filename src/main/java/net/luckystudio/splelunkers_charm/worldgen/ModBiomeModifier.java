package net.luckystudio.splelunkers_charm.worldgen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.datagen.biomeTags.ModBiomeTags;
import net.luckystudio.splelunkers_charm.worldgen.feature.placement.ModCavePlacements;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

// This class is responsible for where the features are placed in the world.

// CF -> PF -> BM
public class ModBiomeModifier {
    public static final ResourceKey<BiomeModifier> OVERWORLD_CAVE_MODIFIER = registerKey("overworld_cave_modifier");
    public static final ResourceKey<BiomeModifier> ICE_CAVE_MODIFIER = registerKey("ice_cave_modifier");
    public static final ResourceKey<BiomeModifier> DESERT_CAVE_MODIFIER = registerKey("desert_cave_modifier");
    public static final ResourceKey<BiomeModifier> NETHER_CAVE_MODIFIER = registerKey("nether_cave_modifier");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        // We register all rocks to spawn in everywhere in the overworld because we want our rocks to be compatible with modded biomes, the ConfiguredFeature will handle the filtering
        context.register(OVERWORLD_CAVE_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.CLAY_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.ROCK_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.DEEPSLATE_ROCK_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.DRIPSTONE_ROCK_PILE),
                        placedFeatures.getOrThrow(ModCavePlacements.PATCH_CAVE_MUSHROOM),
                        placedFeatures.getOrThrow(ModCavePlacements.PATCH_SHORT_UNDERGROWTH),
                        placedFeatures.getOrThrow(ModCavePlacements.PATCH_TALL_UNDERGROWTH)
                )),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(ICE_CAVE_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(ModBiomeTags.IS_COLD_CAVE),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.COLD_STONE),
                        placedFeatures.getOrThrow(ModCavePlacements.ORE_SILT),
                        placedFeatures.getOrThrow(ModCavePlacements.LARGE_ICICLE),
                        placedFeatures.getOrThrow(ModCavePlacements.ICICLE_CLUSTER)
                )),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(DESERT_CAVE_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(ModBiomeTags.IS_HOT_CAVE),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.HOT_STONE),
                        placedFeatures.getOrThrow(CavePlacements.DRIPSTONE_CLUSTER),
                        placedFeatures.getOrThrow(CavePlacements.LARGE_DRIPSTONE)
                )),
                GenerationStep.Decoration.VEGETAL_DECORATION));

        context.register(NETHER_CAVE_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModCavePlacements.BASALT_ROCK_PILE)
                )),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SpelunkersCharm.id(name));
    }
}
