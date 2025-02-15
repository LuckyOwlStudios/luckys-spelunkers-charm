package net.luckystudio.splelunkers_charm.worldgen.feature;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.HugeCaveMushroomFeature;
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

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
