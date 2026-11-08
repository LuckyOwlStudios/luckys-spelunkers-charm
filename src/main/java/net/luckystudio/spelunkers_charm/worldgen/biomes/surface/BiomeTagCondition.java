package net.luckystudio.spelunkers_charm.worldgen.biomes.surface;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * A surface rule condition that passes when the biome at the current block is in a biome tag.
 * Vanilla's {@link SurfaceRules#isBiome} only takes biome keys, this lets our stone rules
 * follow the same biome tags as the rest of the mod (is_cold_cave, is_hot_cave, ...).
 * Needs the SurfaceRules$Context access transformer in META-INF/accesstransformer.cfg.
 */
public record BiomeTagCondition(TagKey<Biome> tag) implements SurfaceRules.ConditionSource {
    public static final KeyDispatchDataCodec<BiomeTagCondition> CODEC = KeyDispatchDataCodec.of(
            TagKey.codec(Registries.BIOME).fieldOf("biome_tag").xmap(BiomeTagCondition::new, BiomeTagCondition::tag));

    public static BiomeTagCondition of(TagKey<Biome> tag) {
        return new BiomeTagCondition(tag);
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        // context.biome is updated for every block the surface system visits, so this is a true 3D biome check
        return () -> context.biome.get().is(this.tag);
    }
}
