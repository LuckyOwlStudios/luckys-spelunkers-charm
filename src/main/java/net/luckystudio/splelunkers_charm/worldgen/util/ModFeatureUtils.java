package net.luckystudio.splelunkers_charm.worldgen.util;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ModFeatureUtils {
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        AquaticFeatures.bootstrap(context);
        CaveFeatures.bootstrap(context);
        EndFeatures.bootstrap(context);
        MiscOverworldFeatures.bootstrap(context);
        NetherFeatures.bootstrap(context);
        OreFeatures.bootstrap(context);
        PileFeatures.bootstrap(context);
        TreeFeatures.bootstrap(context);
        VegetationFeatures.bootstrap(context);
    }

    // This is a copy of the createKey method from the FeatureUtils class
    // All we did was change the Resource location to SpelunkersCharm.id(key)
    public static ResourceKey<ConfiguredFeature<?, ?>> createKey(String key) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, SpelunkersCharm.id(key));
    }
}
