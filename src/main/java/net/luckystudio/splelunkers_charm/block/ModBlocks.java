package net.luckystudio.splelunkers_charm.block;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.custom.*;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.block.util.enums.GeyserState;
import net.luckystudio.splelunkers_charm.item.ModItems;
import net.luckystudio.splelunkers_charm.item.custom.BasaltRockItem;
import net.luckystudio.splelunkers_charm.item.custom.DeepslateRockItem;
import net.luckystudio.splelunkers_charm.item.custom.DripstoneRockItem;
import net.luckystudio.splelunkers_charm.item.custom.RockItem;
import net.luckystudio.splelunkers_charm.item.util.ModFoods;
import net.luckystudio.splelunkers_charm.worldgen.feature.features.ModTreeFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.awt.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SpelunkersCharm.MODID);

    public static Block geyserBlock(Block copyFrom) {
        return new GeyserBlock(BlockBehaviour.Properties.ofFullCopy(copyFrom)
                .emissiveRendering((blockState, blockGetter, blockPos) -> blockState.getValue(ModBlockStateProperties.GEYSER_STATE) == GeyserState.ERUPTING)
                .lightLevel(litBlockEmissionFromGeyserState(15)));
    }

    // Ice Cave Blocks
    public static final DeferredBlock<Block> COLD_STONE = registerBlock("cold_stone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> SILT = registerBlock("silt",
            () -> new ColoredFallingBlock(new ColorRGBA(Color.BLACK.brighter().getAlpha()), BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL)));
    public static final DeferredBlock<Block> ICICLE = registerBlock("icicle", () -> new ModPointedBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_ICE)));
    public static final DeferredBlock<Block> AQUAMARINE_CLUSTER = registerBlock("aquamarine_cluster",
            () -> new AmethystClusterBlock(
                    7.0F,
                    3.0F,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .forceSolidOn()
                            .noOcclusion()
                            .sound(SoundType.AMETHYST_CLUSTER)
                            .strength(1.5F)
                            .lightLevel(blockState -> 8)
                            .emissiveRendering(ModBlocks::always)
                            .pushReaction(PushReaction.DESTROY)
            ));

    public static final DeferredBlock<Block> DEEPSLATE_GEYSER = registerBlock("deepslate_geyser", () -> geyserBlock(Blocks.DEEPSLATE));
    public static final DeferredBlock<Block> BASALT_GEYSER = registerBlock("basalt_geyser", () -> geyserBlock(Blocks.BASALT));

    public static final DeferredBlock<Block> CLAY_BALL = BLOCKS.register("clay_ball",
            () -> new RockBlock(BlockBehaviour.Properties.of().instabreak().noCollission().replaceable()));

    public static final DeferredBlock<Block> ROCK = registerBlockAndBlockItem("rock",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .instabreak()
                    .noCollission()
                    .replaceable()),
            block -> new RockItem(block, new Item.Properties().stacksTo(16))
    );

    public static final DeferredBlock<Block> DEEPSLATE_ROCK = registerBlockAndBlockItem("deepslate_rock",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.DEEPSLATE)
                    .instabreak()
                    .noCollission()
                    .replaceable()),
            block -> new DeepslateRockItem(block, new Item.Properties().stacksTo(16))
    );

    public static final DeferredBlock<Block> DRIPSTONE_ROCK = registerBlockAndBlockItem("dripstone_rock",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .sound(SoundType.DRIPSTONE_BLOCK)
                    .strength(1.5F, 1.0F)
                    .instabreak()
                    .noCollission()
                    .replaceable()),
            block -> new DripstoneRockItem(block, new Item.Properties().stacksTo(16))
    );

    public static final DeferredBlock<Block> BASALT_ROCK = registerBlockAndBlockItem("basalt_rock",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .noCollission()
                    .replaceable()),
            block -> new BasaltRockItem(block, new Item.Properties().stacksTo(16))
    );

    // Copied from the Brown Mushroom Block
    public static final DeferredBlock<Block> CAVE_MUSHROOM = registerBlockAndBlockItem("cave_mushroom",
            () -> new CaveMushroomBlock(ModTreeFeatures.HUGE_CAVE_MUSHROOM, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .lightLevel(p_50892_ -> 1)
                    .hasPostProcess(ModBlocks::always)
                    .pushReaction(PushReaction.DESTROY)),
            block -> new BlockItem(block, new Item.Properties().food(ModFoods.CAVE_MUSHROOM))
    );

    public static final DeferredBlock<Block> CAVE_MUSHROOM_BLOCK = registerBlock("cave_mushroom_block",
            () -> new HugeMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(0.2F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
    ));

    public static final DeferredBlock<Block> SHORT_UNDER_BRUSH = BLOCKS.register("short_under_brush",
            () -> new UnderBrushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XYZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
    ));

    public static final DeferredBlock<Block> TALL_UNDER_BRUSH = BLOCKS.register("tall_under_brush",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
    ));

    private static boolean always(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return true;
    }

    private static ToIntFunction<BlockState> litBlockEmissionFromGeyserState(int lightValue) {
        return blockState -> blockState.getValue(ModBlockStateProperties.GEYSER_STATE) == GeyserState.ERUPTING ? lightValue : 0;
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    public static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> DeferredBlock<T> registerBlockAndBlockItem(
            String name, Supplier<T> block, Function<T, ? extends BlockItem> itemFactory) {

        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(toReturn.get()));
        return toReturn;
    }


    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
