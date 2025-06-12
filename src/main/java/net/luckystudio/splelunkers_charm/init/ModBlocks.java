package net.luckystudio.splelunkers_charm.init;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.custom.*;
import net.luckystudio.splelunkers_charm.block.custom.blaster.BlasterBlock;
import net.luckystudio.splelunkers_charm.block.custom.boulder.BoulderBlock;
import net.luckystudio.splelunkers_charm.block.custom.web_vein.WebVeinBlock;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.block.util.enums.GeyserState;
import net.luckystudio.splelunkers_charm.block.custom.boulder.BoulderType;
import net.luckystudio.splelunkers_charm.item.custom.*;
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
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SpelunkersCharm.MOD_ID);

    // ============================
    // 🔧 Utility / Helpers
    // ============================

    public static Block geyserBlock(Block copyFrom) {
        return new GeyserBlock(BlockBehaviour.Properties.ofFullCopy(copyFrom)
                .emissiveRendering((state, getter, pos) -> state.getValue(ModBlockStateProperties.GEYSER_STATE) == GeyserState.ERUPTING)
                .lightLevel(litBlockEmissionFromGeyserState(15)));
    }

    // ============================
    // 🧱 Building Blocks
    // ============================

    // --- Amethyst Variants ---
    public static final DeferredBlock<Block> AMETHYST_BRICKS = registerBlock("amethyst_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));
    public static final DeferredBlock<StairBlock> AMETHYST_BRICK_STAIRS = registerBlock("amethyst_brick_stairs",
            () -> new StairBlock(AMETHYST_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));
    public static final DeferredBlock<SlabBlock> AMETHYST_BRICK_SLAB = registerBlock("amethyst_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));
    public static final DeferredBlock<WallBlock> AMETHYST_BRICK_WALL = registerBlock("amethyst_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));
    public static final DeferredBlock<Block> CHISELED_AMETHYST_BLOCK = registerBlock("chiseled_amethyst_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)));

    // --- Cold Stone Variants ---
    public static final DeferredBlock<Block> PERMAFROST = registerBlock("permafrost", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> COBBLED_PERMAFROST = registerBlock("cobbled_permafrost", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));
    public static final DeferredBlock<StairBlock> COBBLED_PERMAFROST_STAIRS = registerBlock("cobbled_permafrost_stairs",
            () -> new StairBlock(PERMAFROST.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ModBlocks.COBBLED_PERMAFROST.get())));
    public static final DeferredBlock<SlabBlock> COBBLED_PERMAFROST_SLAB = registerBlock("cobbled_permafrost_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.COBBLED_PERMAFROST.get())));
    public static final DeferredBlock<WallBlock> COBBLED_PERMAFROST_WALL = registerBlock("cobbled_permafrost_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.COBBLED_PERMAFROST.get())));
    public static final DeferredBlock<Block> PERMAFROST_STAIRS = registerBlock("permafrost_stairs",
            () -> new StairBlock(PERMAFROST.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ModBlocks.PERMAFROST.get())));
    public static final DeferredBlock<Block> PERMAFROST_SLAB = registerBlock("permafrost_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.PERMAFROST.get())));
    public static final DeferredBlock<Block> PERMAFROST_WALL = registerBlock("permafrost_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.PERMAFROST.get())));
    public static final DeferredBlock<Block> PERMAFROST_BRICKS = registerBlock("permafrost_bricks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(ModBlocks.PERMAFROST.get())));
    public static final DeferredBlock<StairBlock> PERMAFROST_BRICK_STAIRS = registerBlock("permafrost_brick_stairs",
            () -> new StairBlock(PERMAFROST_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ModBlocks.PERMAFROST.get())));
    public static final DeferredBlock<SlabBlock> PERMAFROST_BRICK_SLAB = registerBlock("permafrost_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.PERMAFROST.get())));
    public static final DeferredBlock<WallBlock> PERMAFROST_BRICK_WALL = registerBlock("permafrost_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.PERMAFROST.get())));
    public static final DeferredBlock<Block> CHISELED_PERMAFROST_BRICKS = registerBlock("chiseled_permafrost_bricks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> PERMAFROST_COAL_ORE = registerBlock("permafrost_coal_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)));
    public static final DeferredBlock<Block> PERMAFROST_IRON_ORE = registerBlock("permafrost_iron_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<Block> PERMAFROST_COPPER_ORE = registerBlock("permafrost_copper_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)));
    public static final DeferredBlock<Block> PERMAFROST_GOLD_ORE = registerBlock("permafrost_gold_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)));
    public static final DeferredBlock<Block> PERMAFROST_REDSTONE_ORE = registerBlock("permafrost_redstone_ore", () -> new RedStoneOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)));
    public static final DeferredBlock<Block> PERMAFROST_EMERALD_ORE = registerBlock("permafrost_emerald_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)));
    public static final DeferredBlock<Block> PERMAFROST_LAPIS_ORE = registerBlock("permafrost_lapis_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)));
    public static final DeferredBlock<Block> PERMAFROST_DIAMOND_ORE = registerBlock("permafrost_diamond_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)));

    // ============================
    // 🌋 Cave Blocks
    // ============================

    public static final DeferredBlock<Block> BASALT_GEYSER = registerBlock("basalt_geyser", () -> geyserBlock(Blocks.BASALT));
    public static final DeferredBlock<Block> DEEPSLATE_GEYSER = registerBlock("deepslate_geyser", () -> geyserBlock(Blocks.DEEPSLATE));
    public static final DeferredBlock<Block> BOULDER = registerBlock("boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE), BoulderType.STONE));
    public static final DeferredBlock<Block> IRON_BOULDER = registerBlock("iron_boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK), BoulderType.IRON));
    public static final DeferredBlock<Block> COPPER_BOULDER = registerBlock("copper_boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_COPPER_BLOCK), BoulderType.COPPER));
    public static final DeferredBlock<Block> GOLD_BOULDER = registerBlock("gold_boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_GOLD_BLOCK), BoulderType.GOLD));
    public static final DeferredBlock<Block> LUSH_BOULDER = registerBlock("lush_boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE), BoulderType.LUSH));
    public static final DeferredBlock<Block> LUSH_IRON_BOULDER = registerBlock("lush_iron_boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK), BoulderType.LUSH_IRON));
    public static final DeferredBlock<Block> LUSH_COPPER_BOULDER = registerBlock("lush_copper_boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_COPPER_BLOCK), BoulderType.LUSH_COPPER));
    public static final DeferredBlock<Block> LUSH_GOLD_BOULDER = registerBlock("lush_gold_boulder",
            () -> new BoulderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_GOLD_BLOCK), BoulderType.LUSH_GOLD));

    // ============================
    // 🪨 Rocks
    // ============================

    public static final DeferredBlock<Block> ROCK = registerBlockAndBlockItem("rock",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(1.5F, 6.0F)
                    .sound(SoundType.STONE).instabreak().noCollission().replaceable()),
            block -> new RockItem(block, new Item.Properties().stacksTo(16)));

    public static final DeferredBlock<Block> ICE_BALL = registerBlockAndBlockItem("ice_ball",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(1.5F, 6.0F)
                    .sound(SoundType.GLASS).instabreak().noCollission().replaceable()),
            block -> new IceBallItem(block, new Item.Properties().stacksTo(16)));

    public static final DeferredBlock<Block> DEEPSLATE_ROCK = registerBlockAndBlockItem("deepslate_rock",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE).strength(3.0F, 6.0F)
                    .sound(SoundType.DEEPSLATE).instabreak().noCollission().replaceable()),
            block -> new DeepslateRockItem(block, new Item.Properties().stacksTo(16)));

    public static final DeferredBlock<Block> DRIPSTONE_ROCK = registerBlockAndBlockItem("dripstone_rock",
            () -> new RockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5F, 1.0F)
                    .sound(SoundType.DRIPSTONE_BLOCK).instabreak().noCollission().replaceable()),
            block -> new DripstoneRockItem(block, new Item.Properties().stacksTo(16)));

    public static final DeferredBlock<Block> BASALT_ROCK = registerBlockAndBlockItem("basalt_rock",
            () -> new RockBlock(BlockBehaviour.Properties.of().instabreak().noCollission().replaceable()),
            block -> new BasaltRockItem(block, new Item.Properties().stacksTo(16)));

    public static final DeferredBlock<Block> CLAY_PILE = BLOCKS.register("clay_ball",
            () -> new RockBlock(BlockBehaviour.Properties.of().instabreak().noCollission().replaceable()));

    // ============================
    // 🍄 Cave Flora
    // ============================

    public static final DeferredBlock<Block> CAVE_MUSHROOM = registerBlockAndBlockItem("cave_mushroom",
            () -> new CaveMushroomBlock(ModTreeFeatures.HUGE_CAVE_MUSHROOM,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN)
                            .noCollission()
                            .randomTicks()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .lightLevel(p -> 1)
                            .hasPostProcess(ModBlocks::always)
                            .pushReaction(PushReaction.DESTROY)),
            block -> new BlockItem(block, new Item.Properties().food(ModFoods.CAVE_MUSHROOM)));

    public static final DeferredBlock<Block> CAVE_MUSHROOM_BLOCK = registerBlock("cave_mushroom_block",
            () -> new HugeMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN).instrument(NoteBlockInstrument.BASS)
                    .strength(0.2F).sound(SoundType.WOOD).ignitedByLava()));

    // ============================
    // 🌿 Underbrush
    // ============================

    public static final DeferredBlock<Block> SHORT_UNDER_BRUSH = BLOCKS.register("short_under_brush",
            () -> new UnderBrushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).replaceable().noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> TALL_UNDER_BRUSH = BLOCKS.register("tall_under_brush",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT).replaceable().noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY)));

    // ============================
    // ❄️ Ice Cave Features
    // ============================

    public static final DeferredBlock<Block> SILT = registerBlock("silt",
            () -> new ColoredFallingBlock(new ColorRGBA(Color.BLACK.brighter().getAlpha()), BlockBehaviour.Properties.ofFullCopy(Blocks.MUD)));

    public static final DeferredBlock<Block> ICICLE = registerBlock("icicle",
            () -> new ModPointedBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_ICE)));

    // ============================
    // 🏜️ Desert Cave Blocks
    // ============================

    public static final DeferredBlock<Block> DUNESTONE = registerBlock("dunestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> COBBLED_DUNESTONE = registerBlock("cobbled_dunestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));
    public static final DeferredBlock<StairBlock> COBBLED_DUNESTONE_STAIRS = registerBlock("cobbled_dunestone_stairs",
            () -> new StairBlock(ModBlocks.DUNESTONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<SlabBlock> COBBLED_DUNESTONE_SLAB = registerBlock("cobbled_dunestone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<WallBlock> COBBLED_DUNESTONE_WALL = registerBlock("cobbled_dunestone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<StairBlock> DUNESTONE_STAIRS = registerBlock("dunestone_stairs",
            () -> new StairBlock(ModBlocks.DUNESTONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<SlabBlock> DUNESTONE_SLAB = registerBlock("dunestone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<WallBlock> DUNESTONE_WALL = registerBlock("dunestone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> DUNESTONE_BRICKS = registerBlock("dunestone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<StairBlock> DUNESTONE_BRICK_STAIRS = registerBlock("dunestone_brick_stairs",
            () -> new StairBlock(ModBlocks.DUNESTONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<SlabBlock> DUNESTONE_BRICK_SLAB = registerBlock("dunestone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<WallBlock> DUNESTONE_BRICK_WALL = registerBlock("dunestone_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> CHISELED_DUNESTONE_BRICKS = registerBlock("chiseled_dunestone_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> DUNESTONE_COAL_ORE = registerBlock("dunestone_coal_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)));
    public static final DeferredBlock<Block> DUNESTONE_IRON_ORE = registerBlock("dunestone_iron_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<Block> DUNESTONE_COPPER_ORE = registerBlock("dunestone_copper_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)));
    public static final DeferredBlock<Block> DUNESTONE_GOLD_ORE = registerBlock("dunestone_gold_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)));
    public static final DeferredBlock<Block> DUNESTONE_REDSTONE_ORE = registerBlock("dunestone_redstone_ore", () -> new RedStoneOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)));
    public static final DeferredBlock<Block> DUNESTONE_EMERALD_ORE = registerBlock("dunestone_emerald_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)));
    public static final DeferredBlock<Block> DUNESTONE_LAPIS_ORE = registerBlock("dunestone_lapis_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)));
    public static final DeferredBlock<Block> DUNESTONE_DIAMOND_ORE = registerBlock("dunestone_diamond_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)));

    // ============================
    // 🌴 Jungle Cave Blocks
    // ============================

    public static final DeferredBlock<Block> WILDSTONE = registerBlock("wildstone", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> COBBLED_WILDSTONE = registerBlock("cobbled_wildstone", () -> new Block(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<StairBlock> COBBLED_WILDSTONE_STAIRS = registerBlock("cobbled_wildstone_stairs", () -> new StairBlock(ModBlocks.WILDSTONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<SlabBlock> COBBLED_WILDSTONE_SLAB = registerBlock("cobbled_wildstone_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<Block> COBBLED_WILDSTONE_WALL = registerBlock("cobbled_wildstone_wall", () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<StairBlock> WILDSTONE_STAIRS = registerBlock("wildstone_stairs",
            () -> new StairBlock(ModBlocks.WILDSTONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<SlabBlock> WILDSTONE_SLAB = registerBlock("wildstone_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<WallBlock> WILDSTONE_WALL = registerBlock("wildstone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<Block> WILDSTONE_BRICKS = registerBlock("wildstone_bricks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<StairBlock> WILDSTONE_BRICK_STAIRS = registerBlock("wildstone_brick_stairs",
            () -> new StairBlock(ModBlocks.WILDSTONE_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<SlabBlock> WILDSTONE_BRICK_SLAB = registerBlock("wildstone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<WallBlock> WILDSTONE_BRICK_WALL = registerBlock("wildstone_brick_wall",
            () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<Block> CHISELED_WILDSTONE_BRICKS = registerBlock("chiseled_wildstone_bricks", () -> new Block(BlockBehaviour.Properties.ofFullCopy(ModBlocks.WILDSTONE.get())));
    public static final DeferredBlock<Block> WILDSTONE_COAL_ORE = registerBlock("wildstone_coal_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE)));
    public static final DeferredBlock<Block> WILDSTONE_IRON_ORE = registerBlock("wildstone_iron_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<Block> WILDSTONE_COPPER_ORE = registerBlock("wildstone_copper_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE)));
    public static final DeferredBlock<Block> WILDSTONE_GOLD_ORE = registerBlock("wildstone_gold_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)));
    public static final DeferredBlock<Block> WILDSTONE_REDSTONE_ORE = registerBlock("wildstone_redstone_ore", () -> new RedStoneOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE)));
    public static final DeferredBlock<Block> WILDSTONE_EMERALD_ORE = registerBlock("wildstone_emerald_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE)));
    public static final DeferredBlock<Block> WILDSTONE_LAPIS_ORE = registerBlock("wildstone_lapis_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE)));
    public static final DeferredBlock<Block> WILDSTONE_DIAMOND_ORE = registerBlock("wildstone_diamond_ore", () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE)));

    // ============================
    // 🕸️ Spider Cave Blocks
    // ============================

    public static final DeferredBlock<Block> PACKED_WEB = registerBlock("packed_web",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL).sound(SoundType.COBWEB).forceSolidOn()
                    .requiresCorrectToolForDrops().strength(4.0F).pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> WEB_VEIN = registerBlock("web_vein",
            () -> new WebVeinBlock(BlockBehaviour.Properties.of()
                    .replaceable()
                    .dynamicShape()
                    .mapColor(MapColor.WOOL)
                    .sound(SoundType.COBWEB)
                    .noCollission()
                    .requiresCorrectToolForDrops()
                    .strength(4.0F)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredBlock<Block> HANGING_WEB = registerBlock("hanging_web",
            () -> new HangingWebBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB)));

    public static final DeferredBlock<Block> SPIDER_EGG = registerBlock("spider_egg",
            () -> new SpiderEgg(BlockBehaviour.Properties.of()
                    .randomTicks().mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.2F).sound(SoundType.AZALEA)
                    .lightLevel(SpiderEgg.LIGHT_EMISSION)
                    .noOcclusion().isRedstoneConductor((state, world, pos) -> false)));

    // ============================
    // 🔴 Redstone Blocks
    // ============================
    public static final DeferredBlock<Block> BLASTER = registerBlock("blaster",
            () -> new BlasterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> WOODEN_LIFT_TRACK = registerBlock("wooden_lift_track",
            () -> new WoodenLiftTrackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
    public static final DeferredBlock<Block> POWERED_LIFT_TRACK = registerBlock("powered_lift_track",
            () -> new PoweredLiftTrackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

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
