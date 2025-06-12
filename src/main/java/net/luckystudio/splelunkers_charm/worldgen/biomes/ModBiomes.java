package net.luckystudio.splelunkers_charm.worldgen.biomes;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public class ModBiomes {
    public static final ResourceKey<Biome> SPIDER_CAVE = register("spider_cave");

    private static ResourceKey<Biome> register(String key) {
        return ResourceKey.create(Registries.BIOME, SpelunkersCharm.id(key));
    }
}
