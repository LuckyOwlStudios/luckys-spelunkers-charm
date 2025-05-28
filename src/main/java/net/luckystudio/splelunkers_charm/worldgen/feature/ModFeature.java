package net.luckystudio.splelunkers_charm.worldgen.feature;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.HugeCaveMushroomFeature;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.geyser.GeyserConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.geyser.GeyserFeature;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.icicle.IcicleConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.icicle.PointedBlockFeature;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.large_icicle.LargeIcicleFeature;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.large_icicle.LargeIcicleFeatureConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.packed_ice_cluster.IcicleClusterConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.packed_ice_cluster.IcicleClusterFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFeature {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, SpelunkersCharm.MODID);

    public static final Supplier<Feature<HugeMushroomFeatureConfiguration>> HUGE_CAVE_MUSHROOM =
            FEATURES.register("huge_cave_mushroom", () -> new HugeCaveMushroomFeature(HugeMushroomFeatureConfiguration.CODEC));

    public static final Supplier<Feature<GeyserConfiguration>> GEYSER =
            FEATURES.register("geyser", () -> new GeyserFeature(GeyserConfiguration.CODEC));

    // Ice Cave Features
    public static final Supplier<Feature<IcicleClusterConfiguration>> PACKED_ICE_CLUSTER =
            FEATURES.register("icicle_cluster", () -> new IcicleClusterFeature(IcicleClusterConfiguration.CODEC));
    public static final Supplier<Feature<LargeIcicleFeatureConfiguration>> LARGE_ICICLE =
            FEATURES.register("large_icicle", () -> new LargeIcicleFeature(LargeIcicleFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
