package net.luckystudio.spelunkers_charm.worldgen.feature.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.init.ModPlacementModifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Lets a placed feature be turned on or off by one of our common config options.
 * Used for features built from vanilla feature types (like rock piles, which use RANDOM_PATCH),
 * where we can't check the config inside the feature itself.
 */
public class ConfigFilter extends PlacementFilter {
    // Suppliers, because the config values are created after this class is first loaded.
    private static final Map<String, Supplier<ModConfigSpec.BooleanValue>> OPTIONS = Map.of(
            "rocks", () -> SpelunkersCharmConfig.ROCKS
    );

    public static final MapCodec<ConfigFilter> CODEC = Codec.STRING
            .validate(option -> OPTIONS.containsKey(option)
                    ? DataResult.success(option)
                    : DataResult.error(() -> "Unknown Spelunker's Charm config option: " + option))
            .fieldOf("option")
            .xmap(ConfigFilter::new, filter -> filter.option);

    private final String option;

    private ConfigFilter(String option) {
        this.option = option;
    }

    public static ConfigFilter of(String option) {
        return new ConfigFilter(option);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return OPTIONS.get(this.option).get().get();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.CONFIG_FILTER.get();
    }
}
