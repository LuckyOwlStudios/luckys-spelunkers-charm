package net.luckystudio.spelunkers_charm.datagen;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.worldgen.ModBiomeModifier;
import net.luckystudio.spelunkers_charm.worldgen.biomes.ModBiomeData;
import net.luckystudio.spelunkers_charm.worldgen.util.ModFeatureUtils;
import net.luckystudio.spelunkers_charm.worldgen.util.ModPlacementUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModDataPackProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ModFeatureUtils::bootstrap)
            .add(Registries.PLACED_FEATURE, ModPlacementUtils::bootstrap)
            .add(Registries.BIOME, ModBiomeData::bootstrap)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifier::bootstrap)
            ;
    public ModDataPackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(SpelunkersCharm.MOD_ID));
    }
}