package net.luckystudio.spelunkers_charm.worldgen.feature.placement;

import net.luckystudio.spelunkers_charm.worldgen.feature.features.ModCaveFeatures;
import net.luckystudio.spelunkers_charm.worldgen.util.ModPlacementUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

// CF -> PF -> BM

// This class is responsible for how the cave features are placed in the world.
public class ModCavePlacements {

    // Overworld Cave Placements
    public static final ResourceKey<PlacedFeature> DEEPSLATE_LAVA_GEYSER = ModPlacementUtils.createKey("deepslate_lava_geyser");
    public static final ResourceKey<PlacedFeature> CLAY_PILE = ModPlacementUtils.createKey("clay_pile");
    public static final ResourceKey<PlacedFeature> ROCK_PILE = ModPlacementUtils.createKey("rock_pile");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_ROCK_PILE = ModPlacementUtils.createKey("deepslate_rock_pile");
    public static final ResourceKey<PlacedFeature> DRIPSTONE_ROCK_PILE = ModPlacementUtils.createKey("dripstone_rock_pile");
    public static final ResourceKey<PlacedFeature> PATCH_CAVE_MUSHROOM = ModPlacementUtils.createKey("patch_cave_mushroom");
    public static final ResourceKey<PlacedFeature> BOULDERS = ModPlacementUtils.createKey("boulders");

    // Ice Cave Placements
    public static final ResourceKey<PlacedFeature> PERMAFROST = ModPlacementUtils.createKey("permafrost");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_COAL_UPPER = ModPlacementUtils.createKey("permafrost_ore_coal_upper");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_COAL_LOWER = ModPlacementUtils.createKey("permafrost_ore_coal_lower");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_IRON_UPPER = ModPlacementUtils.createKey("permafrost_ore_iron_upper");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_IRON_MIDDLE = ModPlacementUtils.createKey("permafrost_ore_iron_middle");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_IRON_SMALL = ModPlacementUtils.createKey("permafrost_ore_iron_small");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_GOLD = ModPlacementUtils.createKey("permafrost_ore_gold");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_GOLD_LOWER = ModPlacementUtils.createKey("permafrost_ore_gold_lower");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_REDSTONE = ModPlacementUtils.createKey("permafrost_ore_redstone");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_REDSTONE_LOWER = ModPlacementUtils.createKey("permafrost_ore_redstone_lower");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_DIAMOND = ModPlacementUtils.createKey("permafrost_ore_diamond");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_DIAMOND_MEDIUM = ModPlacementUtils.createKey("permafrost_ore_diamond_medium");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_DIAMOND_LARGE = ModPlacementUtils.createKey("permafrost_ore_diamond_large");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_DIAMOND_BURIED = ModPlacementUtils.createKey("permafrost_ore_diamond_buried");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_LAPIS = ModPlacementUtils.createKey("permafrost_ore_lapis");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_LAPIS_BURIED = ModPlacementUtils.createKey("permafrost_ore_lapis_buried");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_EMERALD = ModPlacementUtils.createKey("permafrost_ore_emerald");
    public static final ResourceKey<PlacedFeature> PERMAFROST_ORE_COPPER = ModPlacementUtils.createKey("permafrost_ore_copper");
    public static final ResourceKey<PlacedFeature> ORE_SILT = ModPlacementUtils.createKey("permafrost_ore_silt");
    public static final ResourceKey<PlacedFeature> ICE_PILE = ModPlacementUtils.createKey("ice_pile");
    public static final ResourceKey<PlacedFeature> LARGE_ICICLE = ModPlacementUtils.createKey("large_icicle");
    public static final ResourceKey<PlacedFeature> ICICLE_CLUSTER = ModPlacementUtils.createKey("icicle_cluster");
    public static final ResourceKey<PlacedFeature> BOTTOM_ICE_SHEET = ModPlacementUtils.createKey("bottom_ice_sheet");

    // Desert Cave Placements
    public static final ResourceKey<PlacedFeature> DUNESTONE = ModPlacementUtils.createKey("dunestone");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_COAL_UPPER = ModPlacementUtils.createKey("dunestone_ore_coal_upper");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_COAL_LOWER = ModPlacementUtils.createKey("dunestone_ore_coal_lower");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_IRON_UPPER = ModPlacementUtils.createKey("dunestone_ore_iron_upper");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_IRON_MIDDLE = ModPlacementUtils.createKey("dunestone_ore_iron_middle");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_IRON_SMALL = ModPlacementUtils.createKey("dunestone_ore_iron_small");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_GOLD_EXTRA = ModPlacementUtils.createKey("dunestone_ore_gold_extra");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_GOLD = ModPlacementUtils.createKey("dunestone_ore_gold");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_GOLD_LOWER = ModPlacementUtils.createKey("dunestone_ore_gold_lower");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_REDSTONE = ModPlacementUtils.createKey("dunestone_ore_redstone");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_REDSTONE_LOWER = ModPlacementUtils.createKey("dunestone_ore_redstone_lower");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_DIAMOND = ModPlacementUtils.createKey("dunestone_ore_diamond");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_DIAMOND_MEDIUM = ModPlacementUtils.createKey("dunestone_ore_diamond_medium");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_DIAMOND_LARGE = ModPlacementUtils.createKey("dunestone_ore_diamond_large");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_DIAMOND_BURIED = ModPlacementUtils.createKey("dunestone_ore_diamond_buried");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_LAPIS = ModPlacementUtils.createKey("dunestone_ore_lapis");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_LAPIS_BURIED = ModPlacementUtils.createKey("dunestone_ore_lapis_buried");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_EMERALD = ModPlacementUtils.createKey("dunestone_ore_emerald");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_COPPER = ModPlacementUtils.createKey("dunestone_ore_copper");
    public static final ResourceKey<PlacedFeature> DUNESTONE_ORE_COPPER_LARGE = ModPlacementUtils.createKey("dunestone_ore_copper_large");

    // Jungle Cave Placements
    public static final ResourceKey<PlacedFeature> WILDSTONE = ModPlacementUtils.createKey("wildstone");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_COAL_UPPER = ModPlacementUtils.createKey("wildstone_ore_coal_upper");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_COAL_LOWER = ModPlacementUtils.createKey("wildstone_ore_coal_lower");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_IRON_UPPER = ModPlacementUtils.createKey("wildstone_ore_iron_upper");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_IRON_MIDDLE = ModPlacementUtils.createKey("wildstone_ore_iron_middle");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_IRON_SMALL = ModPlacementUtils.createKey("wildstone_ore_iron_small");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_GOLD = ModPlacementUtils.createKey("wildstone_ore_gold");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_GOLD_LOWER = ModPlacementUtils.createKey("wildstone_ore_gold_lower");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_REDSTONE = ModPlacementUtils.createKey("wildstone_ore_redstone");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_REDSTONE_LOWER = ModPlacementUtils.createKey("wildstone_ore_redstone_lower");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_DIAMOND = ModPlacementUtils.createKey("wildstone_ore_diamond");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_DIAMOND_MEDIUM = ModPlacementUtils.createKey("wildstone_ore_diamond_medium");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_DIAMOND_LARGE = ModPlacementUtils.createKey("wildstone_ore_diamond_large");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_DIAMOND_BURIED = ModPlacementUtils.createKey("wildstone_ore_diamond_buried");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_LAPIS = ModPlacementUtils.createKey("wildstone_ore_lapis");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_LAPIS_BURIED = ModPlacementUtils.createKey("wildstone_ore_lapis_buried");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_EMERALD = ModPlacementUtils.createKey("wildstone_ore_emerald");
    public static final ResourceKey<PlacedFeature> WILDSTONE_ORE_COPPER = ModPlacementUtils.createKey("wildstone_ore_copper");
    public static final ResourceKey<PlacedFeature> NEW_LUSH_VEGETATION = ModPlacementUtils.createKey("new_lush_vegetation");

    // Spider Cave Placements
    public static final ResourceKey<PlacedFeature> SPIDER_CAVE_CEILING_VEGETATION = ModPlacementUtils.createKey("spider_cave_ceiling_vegetation");
    public static final ResourceKey<PlacedFeature> WEB_PATCH = ModPlacementUtils.createKey("web_patch");
    public static final ResourceKey<PlacedFeature> WEB_VEIN = ModPlacementUtils.createKey("web_vein");
    public static final ResourceKey<PlacedFeature> SPIDER_EGG = ModPlacementUtils.createKey("spider_egg");

    // Nether Cave Placements
    public static final ResourceKey<PlacedFeature> BASALT_ROCK_PILE = ModPlacementUtils.createKey("basalt_rock_pile");


    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> holdergetter = context.lookup(Registries.CONFIGURED_FEATURE);

        // Overworld Cave Placements
        // Copied from CaveFeature class, the SPORE_BLOSSOM. We just inverted the direction to down make it work for our geyser.
        PlacementUtils.register(context, DEEPSLATE_LAVA_GEYSER, holdergetter.getOrThrow(ModCaveFeatures.DEEPSLATE_LAVA_GEYSER), List.of(
                CountPlacement.of(16),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(4), VerticalAnchor.aboveBottom(24)),
                EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                BiomeFilter.biome()
        ));

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

        PlacementUtils.register(context, BOULDERS, holdergetter.getOrThrow(ModCaveFeatures.BOULDERS), List.of(
                CountPlacement.of(1),
                RarityFilter.onAverageOnceEvery(2), // new line to make it less common
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(32)),
                EnvironmentScanPlacement.scanningFor(
                        Direction.DOWN,
                        BlockPredicate.solid(),
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        12),
                BiomeFilter.biome()
        ));

        // Ice Cave Placements
        PlacementUtils.register(
                context, PERMAFROST, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST), commonOrePlacement(8,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.belowTop(16)))
        );
        PlacementUtils.register(
                context, PERMAFROST_ORE_COAL_UPPER, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_COAL), commonOrePlacement(30, HeightRangePlacement.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top()))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_COAL_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_COAL_BURIED),
                commonOrePlacement(20, HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_IRON_UPPER,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_IRON),
                commonOrePlacement(90, HeightRangePlacement.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_IRON_MIDDLE,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_IRON),
                commonOrePlacement(10, HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)))
        );
        PlacementUtils.register(
                context, PERMAFROST_ORE_IRON_SMALL, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_IRON_SMALL), commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))
        );
        PlacementUtils.register(
                context, PERMAFROST_ORE_GOLD, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_GOLD_BURIED), commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_GOLD_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_GOLD_BURIED),
                orePlacement(CountPlacement.of(UniformInt.of(0, 1)), HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48)))
        );
        PlacementUtils.register(
                context, PERMAFROST_ORE_REDSTONE, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_REDSTONE), commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(15)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_REDSTONE_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_REDSTONE),
                commonOrePlacement(8, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.aboveBottom(32)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_DIAMOND,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_DIAMOND_SMALL),
                commonOrePlacement(7, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_DIAMOND_MEDIUM,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_DIAMOND_MEDIUM),
                commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-4)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_DIAMOND_LARGE,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_DIAMOND_LARGE),
                rareOrePlacement(9, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_DIAMOND_BURIED,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_DIAMOND_BURIED),
                commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context, PERMAFROST_ORE_LAPIS, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_LAPIS), commonOrePlacement(2, HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32)))
        );
        PlacementUtils.register(
                context, PERMAFROST_ORE_LAPIS_BURIED, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_LAPIS_BURIED), commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))
        );
        PlacementUtils.register(
                context,
                PERMAFROST_ORE_EMERALD,
                holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_EMERALD),
                commonOrePlacement(100, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(480)))
        );
        PlacementUtils.register(
                context, PERMAFROST_ORE_COPPER, holdergetter.getOrThrow(ModCaveFeatures.PERMAFROST_ORE_COPPER_SMALL), commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
        );
        PlacementUtils.register(
                context, ORE_SILT, holdergetter.getOrThrow(ModCaveFeatures.ORE_SILT), commonOrePlacement(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top()))
        );
        PlacementUtils.register(context, ICE_PILE, holdergetter.getOrThrow(ModCaveFeatures.ICE_PILE), List.of(
                CountPlacement.of(UniformInt.of(6, 12)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
                BiomeFilter.biome()
        ));
        PlacementUtils.register(
                context,
                LARGE_ICICLE,
                holdergetter.getOrThrow(ModCaveFeatures.LARGE_ICICLE),
                CountPlacement.of(UniformInt.of(10, 48)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
                BiomeFilter.biome()
        );
        PlacementUtils.register(
                context,
                ICICLE_CLUSTER,
                holdergetter.getOrThrow(ModCaveFeatures.ICICLE_CLUSTER),
                CountPlacement.of(UniformInt.of(48, 96)),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.top()),
                BiomeFilter.biome()
        );
        PlacementUtils.register(
                context,
                BOTTOM_ICE_SHEET,
                holdergetter.getOrThrow(ModCaveFeatures.BOTTOM_ICE_SHEET),
                List.of(
                        CarvingMaskPlacement.forStep(GenerationStep.Carving.AIR),
                        HeightRangePlacement.of(ConstantHeight.of(VerticalAnchor.absolute(8))),
                        BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(List.of(Blocks.AIR))),
                        BiomeFilter.biome()
        ));

        // Desert Cave Placements
        PlacementUtils.register(
                context, DUNESTONE, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE), commonOrePlacement(16,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.belowTop(16)))
        );
        PlacementUtils.register(
                context, DUNESTONE_ORE_COAL_UPPER, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_COAL), commonOrePlacement(30, HeightRangePlacement.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top()))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_COAL_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_COAL_BURIED),
                commonOrePlacement(20, HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_IRON_UPPER,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_IRON),
                commonOrePlacement(90, HeightRangePlacement.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_IRON_MIDDLE,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_IRON),
                commonOrePlacement(10, HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)))
        );
        PlacementUtils.register(
                context, DUNESTONE_ORE_IRON_SMALL, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_IRON_SMALL), commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_GOLD_EXTRA,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_GOLD),
                commonOrePlacement(50, HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(256)))
        );
        PlacementUtils.register(
                context, DUNESTONE_ORE_GOLD, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_GOLD_BURIED), commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_GOLD_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_GOLD_BURIED),
                orePlacement(CountPlacement.of(UniformInt.of(0, 1)), HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48)))
        );
        PlacementUtils.register(
                context, DUNESTONE_ORE_REDSTONE, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_REDSTONE), commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(15)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_REDSTONE_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_REDSTONE),
                commonOrePlacement(8, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.aboveBottom(32)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_DIAMOND,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_DIAMOND_SMALL),
                commonOrePlacement(7, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_DIAMOND_MEDIUM,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_DIAMOND_MEDIUM),
                commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-4)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_DIAMOND_LARGE,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_DIAMOND_LARGE),
                rareOrePlacement(9, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_DIAMOND_BURIED,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_DIAMOND_BURIED),
                commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context, DUNESTONE_ORE_LAPIS, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_LAPIS), commonOrePlacement(2, HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32)))
        );
        PlacementUtils.register(
                context, DUNESTONE_ORE_LAPIS_BURIED, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_LAPIS_BURIED), commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_EMERALD,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_EMERALD),
                commonOrePlacement(100, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(480)))
        );
        PlacementUtils.register(
                context, DUNESTONE_ORE_COPPER, holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_COPPER_SMALL), commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
        );
        PlacementUtils.register(
                context,
                DUNESTONE_ORE_COPPER_LARGE,
                holdergetter.getOrThrow(ModCaveFeatures.DUNESTONE_ORE_COPPER_LARGE),
                commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
        );

        // Jungle Cave Placements
        PlacementUtils.register(
                context, WILDSTONE, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE), commonOrePlacement(16,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.belowTop(16)))
        );
        PlacementUtils.register(
                context, WILDSTONE_ORE_COAL_UPPER, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_COAL), commonOrePlacement(30, HeightRangePlacement.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top()))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_COAL_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_COAL_BURIED),
                commonOrePlacement(20, HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_IRON_UPPER,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_IRON),
                commonOrePlacement(90, HeightRangePlacement.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_IRON_MIDDLE,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_IRON),
                commonOrePlacement(10, HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56)))
        );
        PlacementUtils.register(
                context, WILDSTONE_ORE_IRON_SMALL, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_IRON_SMALL), commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72)))
        );
        PlacementUtils.register(
                context, WILDSTONE_ORE_GOLD, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_GOLD_BURIED), commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_GOLD_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_GOLD_BURIED),
                orePlacement(CountPlacement.of(UniformInt.of(0, 1)), HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48)))
        );
        PlacementUtils.register(
                context, WILDSTONE_ORE_REDSTONE, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_REDSTONE), commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(15)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_REDSTONE_LOWER,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_REDSTONE),
                commonOrePlacement(8, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.aboveBottom(32)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_DIAMOND,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_DIAMOND_SMALL),
                commonOrePlacement(7, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_DIAMOND_MEDIUM,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_DIAMOND_MEDIUM),
                commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-4)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_DIAMOND_LARGE,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_DIAMOND_LARGE),
                rareOrePlacement(9, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_DIAMOND_BURIED,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_DIAMOND_BURIED),
                commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)))
        );
        PlacementUtils.register(
                context, WILDSTONE_ORE_LAPIS, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_LAPIS), commonOrePlacement(2, HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32)))
        );
        PlacementUtils.register(
                context, WILDSTONE_ORE_LAPIS_BURIED, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_LAPIS_BURIED), commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)))
        );
        PlacementUtils.register(
                context,
                WILDSTONE_ORE_EMERALD,
                holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_EMERALD),
                commonOrePlacement(100, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(480)))
        );
        PlacementUtils.register(
                context, WILDSTONE_ORE_COPPER, holdergetter.getOrThrow(ModCaveFeatures.WILDSTONE_ORE_COPPER_SMALL), commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)))
        );
        PlacementUtils.register(
                context,
                NEW_LUSH_VEGETATION,
                holdergetter.getOrThrow(CaveFeatures.MOSS_PATCH),
                CountPlacement.of(125),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(20), VerticalAnchor.absolute(50)),
                EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                BiomeFilter.biome()
        );

        // Spider Cave Placements
        PlacementUtils.register(
                context,
                SPIDER_CAVE_CEILING_VEGETATION,
                holdergetter.getOrThrow(ModCaveFeatures.SPIDER_CAVE_PATCH_CEILING),
                CountPlacement.of(125),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
                RandomOffsetPlacement.vertical(ConstantInt.of(-1)),
                BiomeFilter.biome()
        );
        PlacementUtils.register(
                context,
                WEB_PATCH,
                holdergetter.getOrThrow(ModCaveFeatures.WEB_PATCH),
                CountPlacement.of(ConstantInt.of(256)),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                BiomeFilter.biome()
        );
        PlacementUtils.register(
                context,
                WEB_VEIN,
                holdergetter.getOrThrow(ModCaveFeatures.WEB_VEIN),
                CountPlacement.of(UniformInt.of(204, 250)),
                InSquarePlacement.spread(),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                BiomeFilter.biome()
        );
        PlacementUtils.register(
                context,
                SPIDER_EGG,
                holdergetter.getOrThrow(ModCaveFeatures.SPIDER_EGG),
                CountPlacement.of(UniformInt.of(52, 78)),
                PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT,
                InSquarePlacement.spread(),
                SurfaceRelativeThresholdFilter.of(Heightmap.Types.OCEAN_FLOOR_WG, Integer.MIN_VALUE, -13),
                BiomeFilter.biome()
        );
    }

    private static List<PlacementModifier> commonOrePlacement(int count, PlacementModifier heightRange) {
        return orePlacement(CountPlacement.of(count), heightRange);
    }

    private static List<PlacementModifier> orePlacement(PlacementModifier countPlacement, PlacementModifier heightRange) {
        return List.of(countPlacement, InSquarePlacement.spread(), heightRange, BiomeFilter.biome());
    }

    private static List<PlacementModifier> rareOrePlacement(int chance, PlacementModifier heightRange) {
        return orePlacement(RarityFilter.onAverageOnceEvery(chance), heightRange);
    }
}
