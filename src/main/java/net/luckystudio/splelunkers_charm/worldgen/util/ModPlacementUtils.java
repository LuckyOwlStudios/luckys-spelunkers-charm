package net.luckystudio.splelunkers_charm.worldgen.util;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.worldgen.types.ModCavePlacements;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class ModPlacementUtils {
    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        ModCavePlacements.bootstrap(context);
    }

    // A simple copy of the createKey method from the PlacementUtils class, with the namespace changed to SpelunkersCharm.id(key)
    public static ResourceKey<PlacedFeature> createKey(String key) {
        return ResourceKey.create(Registries.PLACED_FEATURE, SpelunkersCharm.id(key));
    }
}
