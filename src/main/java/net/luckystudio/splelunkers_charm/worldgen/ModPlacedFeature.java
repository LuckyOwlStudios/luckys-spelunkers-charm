package net.luckystudio.splelunkers_charm.worldgen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.commands.arguments.HeightmapTypeArgument;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

// CF -> PF -> BM
public class ModPlacedFeature {

    public static final ResourceKey<PlacedFeature> CLAY_PLACED = registerKey("clay_placed");
    public static final ResourceKey<PlacedFeature> ROCK_PLACED = registerKey("rock_placed");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_ROCK_PLACED = registerKey("deepslate_rock_placed");
    public static final ResourceKey<PlacedFeature> DRIPSTONE_ROCK_PLACED = registerKey("dripstone_rock_placed");
    public static final ResourceKey<PlacedFeature> BASALT_ROCK_PLACED = registerKey("basalt_rock_placed");
    public static final ResourceKey<PlacedFeature> CAVE_MUSHROOM_PLACED = registerKey("cave_mushroom_placed");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, CLAY_PLACED, configuredFeatures.getOrThrow(ModConfiguredFeature.CLAY_KEY), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
                BiomeFilter.biome()
        ));

        // Based on Fire Feature in the Nether
        register(context, ROCK_PLACED, configuredFeatures.getOrThrow(ModConfiguredFeature.ROCK_KEY), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
                BiomeFilter.biome()
        ));

        register(context, DEEPSLATE_ROCK_PLACED, configuredFeatures.getOrThrow(ModConfiguredFeature.DEEPSLATE_ROCK_KEY), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(0)),
                BiomeFilter.biome()
        ));

        register(context, DRIPSTONE_ROCK_PLACED, configuredFeatures.getOrThrow(ModConfiguredFeature.DRIPSTONE_ROCK_KEY), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()),
                BiomeFilter.biome()
        ));

        register(context, BASALT_ROCK_PLACED, configuredFeatures.getOrThrow(ModConfiguredFeature.BASALT_ROCK_KEY), List.of(
                CountPlacement.of(UniformInt.of(8, 24)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(8), VerticalAnchor.belowTop(8)),
                BiomeFilter.biome()
        ));

        register(context, CAVE_MUSHROOM_PLACED, configuredFeatures.getOrThrow(ModConfiguredFeature.CAVE_MUSHROOM_KEY), List.of(
                CountPlacement.of(6),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.top()),
                EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                BiomeFilter.biome()
        ));
    }

    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, SpelunkersCharm.id(name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }
}
