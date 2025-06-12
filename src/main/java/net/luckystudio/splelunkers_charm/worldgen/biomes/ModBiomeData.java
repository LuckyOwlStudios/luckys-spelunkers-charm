package net.luckystudio.splelunkers_charm.worldgen.biomes;

import net.luckystudio.splelunkers_charm.init.ModSoundEvents;
import net.luckystudio.splelunkers_charm.worldgen.feature.placement.ModCavePlacements;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

// Copied from BiomeData.class in Minecraft's codebase
// It's used to add biomes into the game, specifically for the Spider Cave biome in this case.
public class ModBiomeData {
    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> holdergetter = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> holdergetter1 = context.lookup(Registries.CONFIGURED_CARVER);
        context.register(ModBiomes.SPIDER_CAVE, spiderCave(holdergetter, holdergetter1));
    }

    public static Biome spiderCave(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        MobSpawnSettings.Builder spawnSettings = new MobSpawnSettings.Builder();
        // Add spawns here, for example:
        spawnSettings.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityType.GLOW_SQUID, 10, 2, 4));
        BiomeDefaultFeatures.commonSpawns(spawnSettings);

        BiomeGenerationSettings.Builder generationSettings = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        globalOverworldGeneration(generationSettings);

        // Add vegetation, ores, features, etc.
        BiomeDefaultFeatures.addDefaultOres(generationSettings);
        ModBiomeData.addSpiderCaveVegetation(generationSettings);
        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.5F)
                .downfall(0.5F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(4159204)
                        .waterFogColor(329011)
                        .fogColor(12638463)
                        .skyColor(7907327)
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .ambientAdditionsSound(new AmbientAdditionsSettings(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSoundEvents.SPIDER_CAVE_ADDITIONS.get()), 0.0016D))
                        .build())
                .mobSpawnSettings(spawnSettings.build())
                .generationSettings(generationSettings.build())
                .build();
    }

    private static void globalOverworldGeneration(BiomeGenerationSettings.Builder generationSettings) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generationSettings);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generationSettings);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generationSettings);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generationSettings);
        BiomeDefaultFeatures.addDefaultSprings(generationSettings);
        BiomeDefaultFeatures.addSurfaceFreezing(generationSettings);
    }

    public static void addSpiderCaveVegetation(BiomeGenerationSettings.Builder builder) {
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModCavePlacements.SPIDER_CAVE_CEILING_VEGETATION);
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModCavePlacements.WEB_PATCH);
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModCavePlacements.WEB_VEIN);
        builder.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModCavePlacements.SPIDER_EGG);
    }
}
