package net.luckystudio.spelunkers_charm.entity.custom.cave_borer;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.datagen.biomes.biomeTags.ModBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cave Borer variants. A borer picks its variant from the biome it spawns in; the first variant whose
 * biome tag matches wins, and STONE is the fallback. To add one: add an entry here with a biome tag,
 * a texture at textures/entity/cave_borer/{name}_cave_borer.png and a loot table at
 * loot_table/entities/{name}_cave_borer.json.
 */
public enum CaveBorerVariant implements StringRepresentable {
    STONE("stone", null),
    SAND("sand", ModBiomeTags.SPAWNS_SAND_CAVE_BORER);

    private static final CaveBorerVariant[] VALUES = values();

    private final String name;
    @Nullable private final TagKey<Biome> biomes;
    private final ResourceLocation texture;
    private final ResourceKey<LootTable> lootTable;

    CaveBorerVariant(String name, @Nullable TagKey<Biome> biomes) {
        this.name = name;
        this.biomes = biomes;
        // The stone variant keeps the original texture and loot table names
        String path = name.equals("stone") ? "cave_borer" : name + "_cave_borer";
        this.texture = SpelunkersCharm.id("textures/entity/cave_borer/" + path + ".png");
        this.lootTable = ResourceKey.create(Registries.LOOT_TABLE, SpelunkersCharm.id("entities/" + path));
    }

    public ResourceLocation getTexture() {
        return this.texture;
    }

    public ResourceKey<LootTable> getLootTable() {
        return this.lootTable;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    public static CaveBorerVariant byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : STONE;
    }

    public static CaveBorerVariant byName(String name) {
        for (CaveBorerVariant variant : VALUES) {
            if (variant.name.equals(name)) return variant;
        }
        return STONE;
    }

    public static CaveBorerVariant forBiome(Holder<Biome> biome) {
        for (CaveBorerVariant variant : VALUES) {
            if (variant.biomes != null && biome.is(variant.biomes)) return variant;
        }
        return STONE;
    }
}
