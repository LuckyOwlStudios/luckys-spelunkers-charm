package net.luckystudio.splelunkers_charm.worldgen.types;

import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.worldgen.util.ModFeatureUtils;
import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// Good example of how to use
// 1. VegetationFeatures
// 2. VegetationPlacements

// CF -> PF -> BM
public class ModCaveFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> CLAY_PILE = ModFeatureUtils.createKey("clay_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_PILE = ModFeatureUtils.createKey("rock_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEPSLATE_ROCK_PILE = ModFeatureUtils.createKey("deepslate_rock_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DRIPSTONE_ROCK_PILE = ModFeatureUtils.createKey("dripstone_rock_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_ROCK_PILE = ModFeatureUtils.createKey("basalt_rock_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_CAVE_MUSHROOM = ModFeatureUtils.createKey("patch_cave_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_SHORT_UNDERGROWTH = ModFeatureUtils.createKey("patch_short_undergrowth");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PATCH_TALL_UNDERGROWTH = ModFeatureUtils.createKey("patch_tall_undergrowth");

    static List<Block> generalOverworldBlocks = List.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DRIPSTONE_BLOCK);
    static List<Block> generalNetherBlocks = List.of(Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE);

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        rockFeature(ModBlocks.CLAY_BALL.get(), CLAY_PILE, context, generalOverworldBlocks);
        rockFeature(ModBlocks.ROCK.get(), ROCK_PILE, context, generalOverworldBlocks);
        rockFeature(ModBlocks.DEEPSLATE_ROCK.get(), DEEPSLATE_ROCK_PILE, context, List.of(Blocks.DEEPSLATE));
        rockFeature(ModBlocks.DRIPSTONE_ROCK.get(), DRIPSTONE_ROCK_PILE, context, List.of(Blocks.DRIPSTONE_BLOCK));
        rockFeature(ModBlocks.BASALT_ROCK.get(), BASALT_ROCK_PILE, context, generalNetherBlocks);
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
    }

    // The exact same class copied from the one in the VegetationFeatures class
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
