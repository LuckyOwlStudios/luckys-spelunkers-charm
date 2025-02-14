package net.luckystudio.splelunkers_charm.worldgen;

import net.luckystudio.splelunkers_charm.ModConfig;
import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

// CF -> PF -> BM
public class ModBiomeModifier {

    public static final ResourceKey<BiomeModifier> OVERWORLD_ROCKS = registerKey("overworld_rocks");
    public static final ResourceKey<BiomeModifier> NETHER_ROCKS = registerKey("nether_rocks");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        // We register all rocks to spawn in everywhere in the overworld because we want our rocks to be compatible with modded biomes, the ConfiguredFeature will handle the filtering
        if (ModConfig.rocks) {
            context.register(OVERWORLD_ROCKS, new BiomeModifiers.AddFeaturesBiomeModifier(
                    biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                    HolderSet.direct(List.of(
                            placedFeatures.getOrThrow(ModPlacedFeature.CLAY_PLACED),
                            placedFeatures.getOrThrow(ModPlacedFeature.ROCK_PLACED),
                            placedFeatures.getOrThrow(ModPlacedFeature.DEEPSLATE_ROCK_PLACED),
                            placedFeatures.getOrThrow(ModPlacedFeature.DRIPSTONE_ROCK_PLACED),
                            placedFeatures.getOrThrow(ModPlacedFeature.CAVE_MUSHROOM_PLACED)
                    )),
                    GenerationStep.Decoration.VEGETAL_DECORATION));
        }

        context.register(NETHER_ROCKS, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(List.of(
                        placedFeatures.getOrThrow(ModPlacedFeature.BASALT_ROCK_PLACED)
                )),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SpelunkersCharm.id(name));
    }
}
