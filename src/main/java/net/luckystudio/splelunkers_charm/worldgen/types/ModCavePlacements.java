package net.luckystudio.splelunkers_charm.worldgen.types;

import net.luckystudio.splelunkers_charm.worldgen.util.ModPlacementUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
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
public class ModCavePlacements {
    public static final ResourceKey<PlacedFeature> CLAY_PILE = ModPlacementUtils.createKey("clay_pile");
    public static final ResourceKey<PlacedFeature> ROCK_PILE = ModPlacementUtils.createKey("rock_pile");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_ROCK_PILE = ModPlacementUtils.createKey("deepslate_rock_pile");
    public static final ResourceKey<PlacedFeature> DRIPSTONE_ROCK_PILE = ModPlacementUtils.createKey("dripstone_rock_pile");
    public static final ResourceKey<PlacedFeature> BASALT_ROCK_PILE = ModPlacementUtils.createKey("basalt_rock_pile");
    public static final ResourceKey<PlacedFeature> PATCH_CAVE_MUSHROOM = ModPlacementUtils.createKey("patch_cave_mushroom");
    public static final ResourceKey<PlacedFeature> PATCH_SHORT_UNDERGROWTH = ModPlacementUtils.createKey("patch_short_undergrowth");
    public static final ResourceKey<PlacedFeature> PATCH_TALL_UNDERGROWTH = ModPlacementUtils.createKey("patch_tall_undergrowth");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> holdergetter = context.lookup(Registries.CONFIGURED_FEATURE);

        PlacementUtils.register(context, CLAY_PILE, holdergetter.getOrThrow(ModCaveFeatures.CLAY_PILE), List.of(
                CountPlacement.of(UniformInt.of(8, 16)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
                BiomeFilter.biome()
        ));

        // Based on Fire Feature in the Nether
        PlacementUtils.register(context, ROCK_PILE, holdergetter.getOrThrow(ModCaveFeatures.ROCK_PILE), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
                BiomeFilter.biome()
        ));

        PlacementUtils.register(context, DEEPSLATE_ROCK_PILE, holdergetter.getOrThrow(ModCaveFeatures.DEEPSLATE_ROCK_PILE), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(0)),
                BiomeFilter.biome()
        ));

        PlacementUtils.register(context, DRIPSTONE_ROCK_PILE, holdergetter.getOrThrow(ModCaveFeatures.DRIPSTONE_ROCK_PILE), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()),
                BiomeFilter.biome()
        ));

        PlacementUtils.register(context, BASALT_ROCK_PILE, holdergetter.getOrThrow(ModCaveFeatures.BASALT_ROCK_PILE), List.of(
                CountPlacement.of(UniformInt.of(8, 24)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(8), VerticalAnchor.belowTop(8)),
                BiomeFilter.biome()
        ));

        PlacementUtils.register(context, PATCH_CAVE_MUSHROOM, holdergetter.getOrThrow(ModCaveFeatures.PATCH_CAVE_MUSHROOM), List.of(
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(64)),
                EnvironmentScanPlacement.scanningFor(
                        Direction.DOWN,
                        BlockPredicate.solid(),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        12),
                RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                BiomeFilter.biome()
        ));

        PlacementUtils.register(context, PATCH_SHORT_UNDERGROWTH, holdergetter.getOrThrow(ModCaveFeatures.PATCH_SHORT_UNDERGROWTH), List.of(
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(64)),
                EnvironmentScanPlacement.scanningFor(
                        Direction.DOWN,
                        BlockPredicate.solid(),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        12),
                RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                BiomeFilter.biome()
        ));

        PlacementUtils.register(context, PATCH_TALL_UNDERGROWTH, holdergetter.getOrThrow(ModCaveFeatures.PATCH_TALL_UNDERGROWTH), List.of(
                CountPlacement.of(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(64)),
                EnvironmentScanPlacement.scanningFor(
                        Direction.DOWN,
                        BlockPredicate.solid(),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        12),
                RandomOffsetPlacement.vertical(ConstantInt.of(1)),
                BiomeFilter.biome()
        ));
    }
}
