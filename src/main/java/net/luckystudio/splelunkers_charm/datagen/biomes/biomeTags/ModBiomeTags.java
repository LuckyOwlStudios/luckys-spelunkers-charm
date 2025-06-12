package net.luckystudio.splelunkers_charm.datagen.biomes.biomeTags;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class ModBiomeTags {

    public static final TagKey<Biome> NO_TREMORS = create("no_tremors");
    public static final TagKey<Biome> IS_COLD_CAVE = create("is_cold_cave");
    public static final TagKey<Biome> IS_HOT_CAVE = create("is_hot_cave");
    public static final TagKey<Biome> IS_JUNGLE_CAVE = create("is_beach");

    private ModBiomeTags() {
    }

    private static TagKey<Biome> create(String name) {
        return TagKey.create(Registries.BIOME, SpelunkersCharm.id(name));
    }
}
