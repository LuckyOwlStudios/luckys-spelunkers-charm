package net.luckystudio.spelunkers_charm.worldgen.feature.features;

import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.block.custom.HangingBlock;
import net.luckystudio.spelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.spelunkers_charm.block.util.enums.GeyserType;
import net.luckystudio.spelunkers_charm.datagen.blockTags.ModBlockTags;
import net.luckystudio.spelunkers_charm.init.ModFeature;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.directional_block.DirectionalBlockFeatureConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.geyser.GeyserConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.large_icicle.LargeIcicleFeatureConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.packed_ice_cluster.IcicleClusterConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.feature.custom.replace.ReplaceMultiBlockSphereConfiguration;
import net.luckystudio.spelunkers_charm.worldgen.util.ModFeatureUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
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
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEPSLATE_LAVA_GEYSER = ModFeatureUtils.createKey("deepslate_lava_geyser");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOULDERS = ModFeatureUtils.createKey("boulders");

    // Ice Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST = ModFeatureUtils.createKey("permafrost");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_COAL = ModFeatureUtils.createKey("permafrost_ore_coal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_COAL_BURIED = ModFeatureUtils.createKey("permafrost_ore_coal_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_IRON = ModFeatureUtils.createKey("permafrost_ore_iron");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_IRON_SMALL = ModFeatureUtils.createKey("permafrost_ore_iron_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_GOLD = ModFeatureUtils.createKey("permafrost_ore_gold");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_GOLD_BURIED = ModFeatureUtils.createKey("permafrost_ore_gold_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_REDSTONE = ModFeatureUtils.createKey("permafrost_ore_redstone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_DIAMOND_SMALL = ModFeatureUtils.createKey("permafrost_ore_diamond_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_DIAMOND_MEDIUM = ModFeatureUtils.createKey("permafrost_ore_diamond_medium");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_DIAMOND_LARGE = ModFeatureUtils.createKey("permafrost_ore_diamond_large");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_DIAMOND_BURIED = ModFeatureUtils.createKey("permafrost_ore_diamond_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_LAPIS = ModFeatureUtils.createKey("permafrost_ore_lapis");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_LAPIS_BURIED = ModFeatureUtils.createKey("permafrost_ore_lapis_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_EMERALD = ModFeatureUtils.createKey("permafrost_ore_emerald");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PERMAFROST_ORE_COPPER_SMALL = ModFeatureUtils.createKey("permafrost_ore_copper_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SILT = ModFeatureUtils.createKey("ore_silt");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICE_PILE = ModFeatureUtils.createKey("ice_pile");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_ICICLE = ModFeatureUtils.createKey("large_icicle");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ICICLE_CLUSTER = ModFeatureUtils.createKey("icicle_cluster");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BOTTOM_ICE_SHEET = ModFeatureUtils.createKey("bottom_ice_sheet");

    // Desert Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE = ModFeatureUtils.createKey("dunestone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_COAL = ModFeatureUtils.createKey("dunestone_ore_coal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_COAL_BURIED = ModFeatureUtils.createKey("dunestone_ore_coal_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_IRON = ModFeatureUtils.createKey("dunestone_ore_iron");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_IRON_SMALL = ModFeatureUtils.createKey("dunestone_ore_iron_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_GOLD = ModFeatureUtils.createKey("dunestone_ore_gold");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_GOLD_BURIED = ModFeatureUtils.createKey("dunestone_ore_gold_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_REDSTONE = ModFeatureUtils.createKey("dunestone_ore_redstone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_DIAMOND_SMALL = ModFeatureUtils.createKey("dunestone_ore_diamond_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_DIAMOND_MEDIUM = ModFeatureUtils.createKey("dunestone_ore_diamond_medium");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_DIAMOND_LARGE = ModFeatureUtils.createKey("dunestone_ore_diamond_large");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_DIAMOND_BURIED = ModFeatureUtils.createKey("dunestone_ore_diamond_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_LAPIS = ModFeatureUtils.createKey("dunestone_ore_lapis");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_LAPIS_BURIED = ModFeatureUtils.createKey("dunestone_ore_lapis_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_EMERALD = ModFeatureUtils.createKey("dunestone_ore_emerald");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_COPPER_SMALL = ModFeatureUtils.createKey("dunestone_ore_copper_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DUNESTONE_ORE_COPPER_LARGE = ModFeatureUtils.createKey("dunestone_ore_copper_large");

    // Jungle Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE = ModFeatureUtils.createKey("wildstone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_COAL = ModFeatureUtils.createKey("wildstone_ore_coal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_COAL_BURIED = ModFeatureUtils.createKey("wildstone_ore_coal_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_IRON = ModFeatureUtils.createKey("wildstone_ore_iron");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_IRON_SMALL = ModFeatureUtils.createKey("wildstone_ore_iron_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_GOLD = ModFeatureUtils.createKey("wildstone_ore_gold");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_GOLD_BURIED = ModFeatureUtils.createKey("wildstone_ore_gold_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_REDSTONE = ModFeatureUtils.createKey("wildstone_ore_redstone");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_DIAMOND_SMALL = ModFeatureUtils.createKey("wildstone_ore_diamond_small");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_DIAMOND_MEDIUM = ModFeatureUtils.createKey("wildstone_ore_diamond_medium");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_DIAMOND_LARGE = ModFeatureUtils.createKey("wildstone_ore_diamond_large");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_DIAMOND_BURIED = ModFeatureUtils.createKey("wildstone_ore_diamond_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_LAPIS = ModFeatureUtils.createKey("wildstone_ore_lapis");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_LAPIS_BURIED = ModFeatureUtils.createKey("wildstone_ore_lapis_buried");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_EMERALD = ModFeatureUtils.createKey("wildstone_ore_emerald");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDSTONE_ORE_COPPER_SMALL = ModFeatureUtils.createKey("wildstone_ore_copper_small");

    // Spider Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> HANGING_WEB = ModFeatureUtils.createKey("hanging_web");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIDER_CAVE_PATCH_CEILING = ModFeatureUtils.createKey("spider_cave_patch_ceiling");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WEB_PATCH = ModFeatureUtils.createKey("web_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WEB_VEIN = ModFeatureUtils.createKey("web_vein");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SPIDER_EGG = ModFeatureUtils.createKey("spider_egg");

    // Nether Cave Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_ROCK_PILE = ModFeatureUtils.createKey("basalt_rock_pile");

    // Just some list of blocks for features to use.
    static List<Block> generalOverworldBlocks = List.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DRIPSTONE_BLOCK);
    static List<Block> generalNetherBlocks = List.of(Blocks.NETHERRACK, Blocks.BASALT, Blocks.BLACKSTONE);

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest ruleOverworld = new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD);
        RuleTest ruletest = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        HolderGetter<ConfiguredFeature<?, ?>> holderGetter = context.lookup(Registries.CONFIGURED_FEATURE);
        // Overworld Cave Features
        rockFeature(ModBlocks.CLAY_PILE.get(), CLAY_PILE, context, generalOverworldBlocks);
        rockFeature(ModBlocks.ROCK.get(), ROCK_PILE, context, generalOverworldBlocks);
        rockFeature(ModBlocks.DEEPSLATE_ROCK.get(), DEEPSLATE_ROCK_PILE, context, List.of(Blocks.DEEPSLATE));
        rockFeature(ModBlocks.DRIPSTONE_ROCK.get(), DRIPSTONE_ROCK_PILE, context, List.of(Blocks.DRIPSTONE_BLOCK, ModBlocks.DUNESTONE.get()));
        FeatureUtils.register(context, PATCH_CAVE_MUSHROOM, Feature.RANDOM_PATCH, FeatureUtils.simplePatchConfiguration(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.CAVE_MUSHROOM.get())),
                generalOverworldBlocks));
        FeatureUtils.register(context, DEEPSLATE_LAVA_GEYSER, ModFeature.GEYSER.get(),
                new GeyserConfiguration(
                        BlockStateProvider.simple(ModBlocks.DEEPSLATE_GEYSER.get().defaultBlockState().setValue(ModBlockStateProperties.GEYSER_TYPE, GeyserType.LAVA)), // or whatever your geyser block is
                        BlockStateProvider.simple(Blocks.DEEPSLATE),
                        BlockStateProvider.simple(Blocks.LAVA)
                ));
        FeatureUtils.register(context, BOULDERS,
                ModFeature.BOULDER.get(), NoneFeatureConfiguration.INSTANCE
        );

        // ICE CAVES
        /**
         * Ice caves:
         * - Permafrost: a new stone block that replaces stone in cold biomes
         * - Coal Ore generates less frequently in permafrost
         * - Infested Stone doesn't generate in permafrost
         */
        stoneSpecificBiomeFeature(context, PERMAFROST, ModBlocks.PERMAFROST.get(), ModBlockTags.PERMAFROST_REPLACEABLE);
        FeatureUtils.register(context, PERMAFROST_ORE_COAL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_COAL_ORE.get().defaultBlockState(), 17));
        FeatureUtils.register(context, PERMAFROST_ORE_COAL_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_COAL_ORE.get().defaultBlockState(), 17, 0.5F));
        FeatureUtils.register(context, PERMAFROST_ORE_IRON, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_IRON_ORE.get().defaultBlockState(), 9));
        FeatureUtils.register(context, PERMAFROST_ORE_IRON_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_IRON_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, PERMAFROST_ORE_GOLD, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_GOLD_ORE.get().defaultBlockState(), 9));
        FeatureUtils.register(context, PERMAFROST_ORE_GOLD_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_GOLD_ORE.get().defaultBlockState(), 9, 0.5F));
        FeatureUtils.register(
                context,
                PERMAFROST_ORE_REDSTONE,
                ModFeature.MOD_ORE_FEATURE.get(),
                new OreConfiguration(
                        ruletest,
                        ModBlocks.PERMAFROST_REDSTONE_ORE.get().defaultBlockState(),
                        8
                )
        );
        FeatureUtils.register(context, PERMAFROST_ORE_DIAMOND_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_DIAMOND_ORE.get().defaultBlockState(), 4, 0.5F));
        FeatureUtils.register(context, PERMAFROST_ORE_DIAMOND_LARGE, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_DIAMOND_ORE.get().defaultBlockState(), 12, 0.7F));
        FeatureUtils.register(context, PERMAFROST_ORE_DIAMOND_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_DIAMOND_ORE.get().defaultBlockState(), 8, 1.0F));
        FeatureUtils.register(context, PERMAFROST_ORE_DIAMOND_MEDIUM, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_DIAMOND_ORE.get().defaultBlockState(), 8, 0.5F));
        FeatureUtils.register(context, PERMAFROST_ORE_LAPIS, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_LAPIS_ORE.get().defaultBlockState(), 7));
        FeatureUtils.register(context, PERMAFROST_ORE_LAPIS_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_LAPIS_ORE.get().defaultBlockState(), 7, 1.0F));
        FeatureUtils.register(
                context,
                PERMAFROST_ORE_EMERALD,
                ModFeature.MOD_ORE_FEATURE.get(),
                new OreConfiguration(
                        ruletest,
                        ModBlocks.PERMAFROST_EMERALD_ORE.get().defaultBlockState(),
                        3
                )
        );
        FeatureUtils.register(context, PERMAFROST_ORE_COPPER_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.PERMAFROST_COPPER_ORE.get().defaultBlockState(), 10));
        FeatureUtils.register(context, ORE_SILT, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruleOverworld, ModBlocks.SILT.get().defaultBlockState(), 32));
        rockFeature(ModBlocks.ICE_BALL.get(), ICE_PILE, context, List.of(ModBlocks.PERMAFROST.get(), Blocks.PACKED_ICE, Blocks.ICE));
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
                ModFeature.ICE_CLUSTER.get(),
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
        FeatureUtils.register(context, BOTTOM_ICE_SHEET, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.ICE)));

        // Desert Cave Features
        stoneSpecificBiomeFeature(context, DUNESTONE, ModBlocks.DUNESTONE.get(), ModBlockTags.DUNESTONE_REPLACEABLE);
        FeatureUtils.register(context, DUNESTONE_ORE_COAL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_COAL_ORE.get().defaultBlockState(), 17));
        FeatureUtils.register(context, DUNESTONE_ORE_COAL_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_COAL_ORE.get().defaultBlockState(), 17, 0.5F));
        FeatureUtils.register(context, DUNESTONE_ORE_IRON, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_IRON_ORE.get().defaultBlockState(), 9));
        FeatureUtils.register(context, DUNESTONE_ORE_IRON_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_IRON_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, DUNESTONE_ORE_GOLD, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_GOLD_ORE.get().defaultBlockState(), 9));
        FeatureUtils.register(context, DUNESTONE_ORE_GOLD_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_GOLD_ORE.get().defaultBlockState(), 9, 0.5F));
        FeatureUtils.register(
                context,
                DUNESTONE_ORE_REDSTONE,
                ModFeature.MOD_ORE_FEATURE.get(),
                new OreConfiguration(
                        ruletest,
                        ModBlocks.DUNESTONE_REDSTONE_ORE.get().defaultBlockState(),
                        8
                )
        );
        FeatureUtils.register(context, DUNESTONE_ORE_DIAMOND_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_DIAMOND_ORE.get().defaultBlockState(), 4, 0.5F));
        FeatureUtils.register(context, DUNESTONE_ORE_DIAMOND_LARGE, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_DIAMOND_ORE.get().defaultBlockState(), 12, 0.7F));
        FeatureUtils.register(context, DUNESTONE_ORE_DIAMOND_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_DIAMOND_ORE.get().defaultBlockState(), 8, 1.0F));
        FeatureUtils.register(context, DUNESTONE_ORE_DIAMOND_MEDIUM, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_DIAMOND_ORE.get().defaultBlockState(), 8, 0.5F));
        FeatureUtils.register(context, DUNESTONE_ORE_LAPIS, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_LAPIS_ORE.get().defaultBlockState(), 7));
        FeatureUtils.register(context, DUNESTONE_ORE_LAPIS_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_LAPIS_ORE.get().defaultBlockState(), 7, 1.0F));
        FeatureUtils.register(
                context,
                DUNESTONE_ORE_EMERALD,
                ModFeature.MOD_ORE_FEATURE.get(),
                new OreConfiguration(
                        ruletest,
                        ModBlocks.DUNESTONE_EMERALD_ORE.get().defaultBlockState(),
                        3
                )
        );
        FeatureUtils.register(context, DUNESTONE_ORE_COPPER_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_COPPER_ORE.get().defaultBlockState(), 10));
        FeatureUtils.register(context, DUNESTONE_ORE_COPPER_LARGE, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.DUNESTONE_COPPER_ORE.get().defaultBlockState(), 20));

        // Jungle Cave Features
        stoneSpecificBiomeFeature(context, WILDSTONE, ModBlocks.WILDSTONE.get(), ModBlockTags.WILDSTONE_REPLACEABLE);
        FeatureUtils.register(context, WILDSTONE_ORE_COAL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_COAL_ORE.get().defaultBlockState(), 17));
        FeatureUtils.register(context, WILDSTONE_ORE_COAL_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_COAL_ORE.get().defaultBlockState(), 17, 0.5F));
        FeatureUtils.register(context, WILDSTONE_ORE_IRON, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_IRON_ORE.get().defaultBlockState(), 9));
        FeatureUtils.register(context, WILDSTONE_ORE_IRON_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_IRON_ORE.get().defaultBlockState(), 4));
        FeatureUtils.register(context, WILDSTONE_ORE_GOLD, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_GOLD_ORE.get().defaultBlockState(), 9));
        FeatureUtils.register(context, WILDSTONE_ORE_GOLD_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_GOLD_ORE.get().defaultBlockState(), 9, 0.5F));
        FeatureUtils.register(
                context,
                WILDSTONE_ORE_REDSTONE,
                ModFeature.MOD_ORE_FEATURE.get(),
                new OreConfiguration(
                        ruletest,
                        ModBlocks.WILDSTONE_REDSTONE_ORE.get().defaultBlockState(),
                        8
                )
        );
        FeatureUtils.register(context, WILDSTONE_ORE_DIAMOND_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_DIAMOND_ORE.get().defaultBlockState(), 4, 0.5F));
        FeatureUtils.register(context, WILDSTONE_ORE_DIAMOND_LARGE, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_DIAMOND_ORE.get().defaultBlockState(), 12, 0.7F));
        FeatureUtils.register(context, WILDSTONE_ORE_DIAMOND_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_DIAMOND_ORE.get().defaultBlockState(), 8, 1.0F));
        FeatureUtils.register(context, WILDSTONE_ORE_DIAMOND_MEDIUM, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_DIAMOND_ORE.get().defaultBlockState(), 8, 0.5F));
        FeatureUtils.register(context, WILDSTONE_ORE_LAPIS, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_LAPIS_ORE.get().defaultBlockState(), 7));
        FeatureUtils.register(context, WILDSTONE_ORE_LAPIS_BURIED, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_LAPIS_ORE.get().defaultBlockState(), 7, 1.0F));
        FeatureUtils.register(
                context,
                WILDSTONE_ORE_EMERALD,
                ModFeature.MOD_ORE_FEATURE.get(),
                new OreConfiguration(
                        ruletest,
                        ModBlocks.WILDSTONE_EMERALD_ORE.get().defaultBlockState(),
                        3
                )
        );
        FeatureUtils.register(context, WILDSTONE_ORE_COPPER_SMALL, ModFeature.MOD_ORE_FEATURE.get(), new OreConfiguration(ruletest, ModBlocks.WILDSTONE_COPPER_ORE.get().defaultBlockState(), 10));

        // Spider Cave Features
        FeatureUtils.register(
                context,
                HANGING_WEB,
                Feature.BLOCK_COLUMN,
                new BlockColumnConfiguration(
                        List.of(
                                BlockColumnConfiguration.layer(
                                        new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder().add(UniformInt.of(0, 3), 5).add(UniformInt.of(1, 7), 1).build()),
                                        BlockStateProvider.simple(ModBlocks.HANGING_WEB.get().defaultBlockState().setValue(HangingBlock.END, false))
                                ),
                                BlockColumnConfiguration.layer(ConstantInt.of(1), BlockStateProvider.simple(ModBlocks.HANGING_WEB.get().defaultBlockState().setValue(HangingBlock.END, true)))
                        ),
                        Direction.DOWN,
                        BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        true
                )
        );
        FeatureUtils.register(
                context,
                SPIDER_CAVE_PATCH_CEILING,
                Feature.VEGETATION_PATCH,
                new VegetationPatchConfiguration(
                        BlockTags.BASE_STONE_OVERWORLD,
                        BlockStateProvider.simple(ModBlocks.PACKED_WEB.get()),
                        PlacementUtils.inlinePlaced(holderGetter.getOrThrow(HANGING_WEB)),
                        CaveSurface.CEILING,
                        UniformInt.of(1, 2),
                        0.0F,
                        5,
                        0.08F,
                        UniformInt.of(4, 7),
                        0.3F
                )
        );
        FeatureUtils.register(context, WEB_PATCH, ModFeature.WEB_PATCH.get(), new SculkPatchConfiguration(10, 32, 64, 0, 1, ConstantInt.of(0), 0.5F));
        MultifaceBlock webVein = (MultifaceBlock)ModBlocks.WEB_VEIN.get();
        FeatureUtils.register(
                context,
                WEB_VEIN,
                Feature.MULTIFACE_GROWTH,
                new MultifaceGrowthConfiguration(
                        webVein,           // the multiface block to place
                        20,                // search range (how far it tries to spread)
                        true,              // can place on floor
                        true,              // can place on ceiling
                        true,              // can place on walls
                        1.0F,              // chance of spreading (100%)
                        HolderSet.direct(  // blocks it can be placed on
                                Block::builtInRegistryHolder,
                                Blocks.STONE,
                                Blocks.ANDESITE,
                                Blocks.DIORITE,
                                Blocks.GRANITE,
                                Blocks.DRIPSTONE_BLOCK,
                                Blocks.CALCITE,
                                Blocks.TUFF,
                                Blocks.DEEPSLATE
                        )
                )
        );
        FeatureUtils.register(
                context,
                SPIDER_EGG,
                ModFeature.DIRECTIONAL_BLOCK.get(),
                new DirectionalBlockFeatureConfiguration(BlockStateProvider.simple(ModBlocks.SPIDER_EGG.get()), BlockStateProvider.simple(ModBlocks.PACKED_WEB.get())));

        // Nether Cave Features
        rockFeature(ModBlocks.BASALT_ROCK.get(), BASALT_ROCK_PILE, context, generalNetherBlocks);
    }

    private static void stoneSpecificBiomeFeature(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> stoneFeatureKey, Block newStone, TagKey<Block> blocksToReplace) {
        FeatureUtils.register(
                context,
                stoneFeatureKey,
                Feature.RANDOM_PATCH,
                FeatureUtils.simplePatchConfiguration(
                        ModFeature.REPLACE_MULTI_BLOCK_BLOB_WITH_CONFIG.get(),
                        new ReplaceMultiBlockSphereConfiguration(newStone.defaultBlockState(), blocksToReplace, UniformInt.of(4, 8))));
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
