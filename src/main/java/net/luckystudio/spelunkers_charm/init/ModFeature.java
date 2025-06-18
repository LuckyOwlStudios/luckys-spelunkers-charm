package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.HugeCaveMushroomFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.boulder.BoulderFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.directional_block.DirectionalBlockFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.directional_block.DirectionalBlockFeatureConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.geyser.GeyserConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.geyser.GeyserFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.large_icicle.LargeIcicleFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.large_icicle.LargeIcicleFeatureConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.ore.ModOreFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.packed_ice_cluster.IcicleClusterConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.packed_ice_cluster.IcicleClusterFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.replace.ReplaceMultiBlockBlobFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.replace.ReplaceMultiBlockSphereConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.spread_blob.ReplaceSpreadingBlobsFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.spread_blob.ReplaceSpreadingSphereConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.web_patch.WebPatchFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SculkPatchConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFeature {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, SpelunkersCharm.MOD_ID);

    public static final Supplier<Feature<ReplaceMultiBlockSphereConfiguration>> REPLACE_MULTI_BLOCK_BLOB_WITH_CONFIG =
            FEATURES.register("replace_multi_block_blob", () -> new ReplaceMultiBlockBlobFeature(ReplaceMultiBlockSphereConfiguration.CODEC));

    public static final Supplier<Feature<OreConfiguration>> MOD_ORE_FEATURE =
            FEATURES.register("mod_ore_feature", () -> new ModOreFeature(OreConfiguration.CODEC));

    public static final Supplier<Feature<ReplaceSpreadingSphereConfiguration>> REPLACE_SPREADING_BLOCK_BLOB =
            FEATURES.register("replace_spreading_block_blob", () -> new ReplaceSpreadingBlobsFeature(ReplaceSpreadingSphereConfiguration.CODEC));

    public static final Supplier<Feature<SculkPatchConfiguration>> WEB_PATCH =
            FEATURES.register("web_patch", () -> new WebPatchFeature(SculkPatchConfiguration.CODEC));

    public static final Supplier<Feature<DirectionalBlockFeatureConfiguration>> DIRECTIONAL_BLOCK =
            FEATURES.register("directional_block", () -> new DirectionalBlockFeature(DirectionalBlockFeatureConfiguration.CODEC));

    public static final Supplier<Feature<GeyserConfiguration>> GEYSER =
            FEATURES.register("geyser", () -> new GeyserFeature(GeyserConfiguration.CODEC));

    public static final Supplier<Feature<HugeMushroomFeatureConfiguration>> HUGE_CAVE_MUSHROOM =
            FEATURES.register("huge_cave_mushroom", () -> new HugeCaveMushroomFeature(HugeMushroomFeatureConfiguration.CODEC));

    public static final Supplier<Feature<NoneFeatureConfiguration>> BOULDER =
            FEATURES.register("boulder", () -> new BoulderFeature(NoneFeatureConfiguration.CODEC));

    // Ice Cave Features
    public static final Supplier<Feature<IcicleClusterConfiguration>> ICE_CLUSTER =
            FEATURES.register("icicle_cluster", () -> new IcicleClusterFeature(IcicleClusterConfiguration.CODEC));
    public static final Supplier<Feature<LargeIcicleFeatureConfiguration>> LARGE_ICICLE =
            FEATURES.register("large_icicle", () -> new LargeIcicleFeature(LargeIcicleFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
