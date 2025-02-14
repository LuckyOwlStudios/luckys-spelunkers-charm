package net.luckystudio.splelunkers_charm.worldgen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.worldgen.util.ModFeatureUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// Good example of how to use
// 1. VegetationFeatures
// 2. VegetationPlacements

// CF -> PF -> BM
public class ModConfiguredFeature {

    public static final ResourceKey<ConfiguredFeature<?, ?>> CLAY_KEY = ModFeatureUtils.createKey("clay_feature");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_KEY = ModFeatureUtils.createKey("rock_feature");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEPSLATE_ROCK_KEY = ModFeatureUtils.createKey("deepslate_rock_feature");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DRIPSTONE_ROCK_KEY = ModFeatureUtils.createKey("dripstone_rock_feature");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_ROCK_KEY = ModFeatureUtils.createKey("basalt_rock_feature");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CAVE_MUSHROOM_KEY = ModFeatureUtils.createKey("cave_mushroom_feature");

    static List<Block> generalOverworldBlocks = List.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DRIPSTONE_BLOCK);
    static List<Block> generalNetherBlocks = List.of(Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE);

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        register(context, CLAY_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.CLAY_BALL.get())), List.of(Blocks.CLAY)));
        rockFeature(ModBlocks.ROCK.get(), ROCK_KEY, context, generalOverworldBlocks);
        rockFeature(ModBlocks.DEEPSLATE_ROCK.get(), DEEPSLATE_ROCK_KEY, context, List.of(Blocks.DEEPSLATE));
        rockFeature(ModBlocks.DRIPSTONE_ROCK.get(), DRIPSTONE_ROCK_KEY, context, generalOverworldBlocks);
        rockFeature(ModBlocks.BASALT_ROCK.get(), BASALT_ROCK_KEY, context, generalNetherBlocks);
        register(context, CAVE_MUSHROOM_KEY, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.CAVE_MUSHROOM.get())),
                generalOverworldBlocks));
    }

    // This is copied from the PINK_PETALS feature inside the VegetationFeatures class
    private static void rockFeature(Block block, ResourceKey<ConfiguredFeature<?, ?>> rockKey, BootstrapContext<ConfiguredFeature<?, ?>> context, List<@NotNull Block> blocks) {
        SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
        for (int i = 1; i <= 3; i++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                builder.add(block.defaultBlockState().setValue(ModBlockStateProperties.ROCKS, i).setValue(PinkPetalsBlock.FACING, direction), 1);
            }
        }
        register(context, rockKey, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(builder)), blocks));
    }


    // This is similar to how an armor trim is made, i don't know what use is this...
    private static <FC extends FeatureConfiguration,
            F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key,
                                                                                          F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
