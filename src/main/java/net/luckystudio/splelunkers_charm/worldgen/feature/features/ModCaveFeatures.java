package net.luckystudio.splelunkers_charm.worldgen.feature.features;

import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.block.util.enums.GeyserType;
import net.luckystudio.splelunkers_charm.worldgen.feature.ModFeature;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.geyser.GeyserConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.icicle.IcicleConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.large_icicle.LargeIcicleFeatureConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.feature.custom.packed_ice_cluster.IcicleClusterConfiguration;
import net.luckystudio.splelunkers_charm.worldgen.util.ModFeatureUtils;
import net.luckystudio.splelunkers_charm.worldgen.util.ModPlacementUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// Good example of how to use
// 1. VegetationFeatures
// 2. VegetationPlacements

// CF -> PF -> BM

// This class is responsible for registering all the cave features
public class ModCaveFeatures {

    // Overworld Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> CLAY_PILE = ModFeatureUtils.createKey("clay_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_PILE = ModFeatureUtils.createKey("rock_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEPSLATE_ROCK_PILE = ModFeatureUtils.createKey("deepslate_rock_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DRIPSTONE_ROCK_PILE = ModFeatureUtils.createKey("dripstone_rock_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_CAVE_MUSHROOM = ModFeatureUtils.createKey("patch_cave_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_SHORT_UNDERGROWTH = ModFeatureUtils.createKey("patch_short_undergrowth");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_TALL_UNDERGROWTH = ModFeatureUtils.createKey("patch_tall_undergrowth");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEPSLATE_LAVA_GEYSER = ModFeatureUtils.createKey("deepslate_lava_geyser");

    // Ice Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> COLD_STONE = ModFeatureUtils.createKey("cold_stone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SILT = ModFeatureUtils.createKey("ore_silt");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_ICICLE = ModFeatureUtils.createKey("large_icicle");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICICLE_CLUSTER = ModFeatureUtils.createKey("icicle_cluster");

    // Nether Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_ROCK_PILE = ModFeatureUtils.createKey("basalt_rock_pile");

    // Just some list of blocks for features to use.
    static List<Block> generalOverworldBlocks = List.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DRIPSTONE_BLOCK);
    static List<Block> generalNetherBlocks = List.of(Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE);

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest ruleOverworld = new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD);
        // Overworld Cave Features
        rockFeature(ModBlocks.CLAY_BALL.get(), CLAY_PILE, context, generalOverworldBlocks);
        rockFeature(ModBlocks.ROCK.get(), ROCK_PILE, context, generalOverworldBlocks);
        rockFeature(ModBlocks.DEEPSLATE_ROCK.get(), DEEPSLATE_ROCK_PILE, context, List.of(Blocks.DEEPSLATE));
        rockFeature(ModBlocks.DRIPSTONE_ROCK.get(), DRIPSTONE_ROCK_PILE, context, List.of(Blocks.DRIPSTONE_BLOCK));
        FeatureUtils.register(context, PATCH_CAVE_MUSHROOM, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.CAVE_MUSHROOM.get())),
                generalOverworldBlocks));
        FeatureUtils.register(context, PATCH_SHORT_UNDERGROWTH, Feature.RANDOM_PATCH, grassPatch(BlockStateProvider.simple(ModBlocks.SHORT_UNDER_BRUSH.get()), 32));
        FeatureUtils.register(
                context,
                PATCH_TALL_UNDERGROWTH,
                Feature.RANDOM_PATCH,
                FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.TALL_UNDER_BRUSH.get())))
        );
        FeatureUtils.register(context, DEEPSLATE_LAVA_GEYSER, ModFeature.GEYSER.get(),
                new GeyserConfiguration(
                        BlockStateProvider.simple(ModBlocks.DEEPSLATE_GEYSER.get().defaultBlockState().setValue(ModBlockStateProperties.GEYSER_TYPE, GeyserType.LAVA)), // or whatever your geyser block is
                        BlockStateProvider.simple(Blocks.DEEPSLATE),
                        BlockStateProvider.simple(Blocks.LAVA)
                ));

        // Ice Cave Features
        FeatureUtils.register(
                context,
                COLD_STONE,
                Feature.RANDOM_PATCH,
                FeatureUtils.simplePatchConfiguration(
                        Feature.REPLACE_BLOBS,
                        new ReplaceSphereConfiguration(Blocks.STONE.defaultBlockState(), ModBlocks.COLD_STONE.get().defaultBlockState(), UniformInt.of(3, 7))
                )
        );
        FeatureUtils.register(context, ORE_SILT, Feature.ORE, new OreConfiguration(ruleOverworld, ModBlocks.SILT.get().defaultBlockState(), 33));
        // Copied from LARGE_DRIPSTONE in CaveFeature class
        FeatureUtils.register(
                context,
                LARGE_ICICLE,
                ModFeature.LARGE_ICICLE.get(),
                new LargeIcicleFeatureConfiguration(
                        30,
                        UniformInt.of(3, 19),
                        UniformFloat.of(0.4F, 2.0F),
                        0.33F,
                        UniformFloat.of(0.3F, 0.9F),
                        UniformFloat.of(0.4F, 1.0F),
                        UniformFloat.of(0.0F, 0.3F),
                        4,
                        0.6F
                )
        );
        // Copied fromCaveFeature class, the DRIPSTONE_CLUSTER
        FeatureUtils.register(
                context,
                ICICLE_CLUSTER,
                ModFeature.PACKED_ICE_CLUSTER.get(),
                new IcicleClusterConfiguration(
                        12,
                        UniformInt.of(3, 6),
                        UniformInt.of(2, 8),
                        1,
                        3,
                        UniformInt.of(2, 4),
                        UniformFloat.of(0.3F, 0.7F),
                        ClampedNormalFloat.of(0.1F, 0.3F, 0.1F, 0.9F),
                        0.1F,
                        3,
                        8
                )
        );

        // Nether Cave Features
        rockFeature(ModBlocks.BASALT_ROCK.get(), BASALT_ROCK_PILE, context, generalNetherBlocks);
    }

    // The exact same class copied from the one in the VegetationFeatures class, it's a helper method for grass like generation
    private static RandomPatchConfiguration grassPatch(BlockStateProvider stateProvider, int tries) {
        return FeatureUtils.simpleRandomPatchConfiguration(
                tries, PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(stateProvider))
        );
    }

    // This is copied from the PINK_PETALS feature inside the VegetationFeatures class
    private static void rockFeature(Block block, ResourceKey<ConfiguredFeature<?, ?>> rockKey, BootstrapContext<ConfiguredFeature<?, ?>> context, List<@NotNull Block> blocks) {
        SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
        for (int i = 1; i <= 3; i++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                builder.add(block.defaultBlockState().setValue(ModBlockStateProperties.ROCKS, i).setValue(PinkPetalsBlock.FACING, direction), 1);
            }
        }
        FeatureUtils.register(context, rockKey, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(builder)), blocks));
    }
}
