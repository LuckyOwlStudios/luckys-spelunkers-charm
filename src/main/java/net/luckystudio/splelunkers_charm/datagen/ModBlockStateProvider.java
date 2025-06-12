package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.luckystudio.splelunkers_charm.block.custom.boulder.BoulderBlock;
import net.luckystudio.splelunkers_charm.block.custom.HangingBlock;
import net.luckystudio.splelunkers_charm.block.custom.PoweredLiftTrackBlock;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.block.util.enums.BlockPart;
import net.luckystudio.splelunkers_charm.block.custom.boulder.HangingType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.neoforged.neoforge.client.model.generators.*;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, SpelunkersCharm.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        // Building Blocks
        modSimpleBlock(ModBlocks.DUNESTONE.get());
        modSimpleStairBlock(ModBlocks.DUNESTONE_STAIRS.get(), SpelunkersCharm.id("block/dunestone"));
        modSimpleSlabBlock(ModBlocks.DUNESTONE_SLAB.get(), SpelunkersCharm.id("block/dunestone"), SpelunkersCharm.id("block/dunestone"));
        modWallBlock(ModBlocks.DUNESTONE_WALL.get(), SpelunkersCharm.id("block/dunestone"));
        modSimpleBlock(ModBlocks.DUNESTONE_BRICKS.get());
        modSimpleStairBlock(ModBlocks.DUNESTONE_BRICK_STAIRS.get(), SpelunkersCharm.id("block/dunestone_bricks"));
        modSimpleSlabBlock(ModBlocks.DUNESTONE_BRICK_SLAB.get(), SpelunkersCharm.id("block/dunestone_bricks"), SpelunkersCharm.id("block/dunestone_bricks"));
        modWallBlock(ModBlocks.DUNESTONE_BRICK_WALL.get(), SpelunkersCharm.id("block/dunestone_bricks"));
        modSimpleBlock(ModBlocks.CHISELED_DUNESTONE_BRICKS.get());
        modSimpleBlock(ModBlocks.COBBLED_DUNESTONE.get());
        modSimpleStairBlock(ModBlocks.COBBLED_DUNESTONE_STAIRS.get(), SpelunkersCharm.id("block/cobbled_dunestone"));
        modSimpleSlabBlock(ModBlocks.COBBLED_DUNESTONE_SLAB.get(), SpelunkersCharm.id("block/cobbled_dunestone"), SpelunkersCharm.id("block/cobbled_dunestone"));
        modWallBlock(ModBlocks.COBBLED_DUNESTONE_WALL.get(), SpelunkersCharm.id("block/cobbled_dunestone"));

        modSimpleBlock(ModBlocks.PERMAFROST.get());
        modSimpleStairBlock(ModBlocks.PERMAFROST_STAIRS.get(), SpelunkersCharm.id("block/permafrost"));
        modSimpleSlabBlock(ModBlocks.PERMAFROST_SLAB.get(), SpelunkersCharm.id("block/permafrost"), SpelunkersCharm.id("block/permafrost"));
        modWallBlock(ModBlocks.PERMAFROST_WALL.get(), SpelunkersCharm.id("block/permafrost"));
        modSimpleBlock(ModBlocks.PERMAFROST_BRICKS.get());
        modSimpleStairBlock(ModBlocks.PERMAFROST_BRICK_STAIRS.get(), SpelunkersCharm.id("block/permafrost_bricks"));
        modSimpleSlabBlock(ModBlocks.PERMAFROST_BRICK_SLAB.get(), SpelunkersCharm.id("block/permafrost_bricks"), SpelunkersCharm.id("block/permafrost_bricks"));
        modWallBlock(ModBlocks.PERMAFROST_BRICK_WALL.get(), SpelunkersCharm.id("block/permafrost_bricks"));
        modSimpleBlock(ModBlocks.CHISELED_PERMAFROST_BRICKS.get());
        modSimpleBlock(ModBlocks.COBBLED_PERMAFROST.get());
        modSimpleStairBlock(ModBlocks.COBBLED_PERMAFROST_STAIRS.get(), SpelunkersCharm.id("block/cobbled_permafrost"));
        modSimpleSlabBlock(ModBlocks.COBBLED_PERMAFROST_SLAB.get(), SpelunkersCharm.id("block/cobbled_permafrost"), SpelunkersCharm.id("block/cobbled_permafrost"));
        modWallBlock(ModBlocks.COBBLED_PERMAFROST_WALL.get(), SpelunkersCharm.id("block/cobbled_permafrost"));

        modSimpleBlock(ModBlocks.WILDSTONE.get());
        modSimpleStairBlock(ModBlocks.WILDSTONE_STAIRS.get(), SpelunkersCharm.id("block/wildstone"));
        modSimpleSlabBlock(ModBlocks.WILDSTONE_SLAB.get(), SpelunkersCharm.id("block/wildstone"), SpelunkersCharm.id("block/wildstone"));
        modWallBlock(ModBlocks.WILDSTONE_WALL.get(), SpelunkersCharm.id("block/wildstone"));
        modSimpleBlock(ModBlocks.WILDSTONE_BRICKS.get());
        modSimpleStairBlock(ModBlocks.WILDSTONE_BRICK_STAIRS.get(), SpelunkersCharm.id("block/wildstone_bricks"));
        modSimpleSlabBlock(ModBlocks.WILDSTONE_BRICK_SLAB.get(), SpelunkersCharm.id("block/wildstone_bricks"), SpelunkersCharm.id("block/wildstone_bricks"));
        modWallBlock(ModBlocks.WILDSTONE_BRICK_WALL.get(), SpelunkersCharm.id("block/wildstone_bricks"));
        modSimpleBlock(ModBlocks.CHISELED_WILDSTONE_BRICKS.get());
        modSimpleBlock(ModBlocks.COBBLED_WILDSTONE.get());
        modSimpleStairBlock(ModBlocks.COBBLED_WILDSTONE_STAIRS.get(), SpelunkersCharm.id("block/cobbled_wildstone"));
        modSimpleSlabBlock(ModBlocks.COBBLED_WILDSTONE_SLAB.get(), SpelunkersCharm.id("block/cobbled_wildstone"), SpelunkersCharm.id("block/cobbled_wildstone"));
        modWallBlock(ModBlocks.COBBLED_WILDSTONE_WALL.get(), SpelunkersCharm.id("block/cobbled_wildstone"));

        modSimpleBlock(ModBlocks.AMETHYST_BRICKS.get());
        modSimpleStairBlock(ModBlocks.AMETHYST_BRICK_STAIRS.get(), SpelunkersCharm.id("block/amethyst_bricks"));
        modSimpleSlabBlock(ModBlocks.AMETHYST_BRICK_SLAB.get(), SpelunkersCharm.id("block/amethyst_bricks"), SpelunkersCharm.id("block/amethyst_bricks"));
        modWallBlock(ModBlocks.AMETHYST_BRICK_WALL.get(), SpelunkersCharm.id("block/amethyst_bricks"));
        modSimpleBlock(ModBlocks.CHISELED_AMETHYST_BLOCK.get());

        // Natural Blocks
        modSimpleBlock(ModBlocks.SILT.get());
        icicle(ModBlocks.ICICLE.get(), mcLoc("block/packed_ice"));
        modSimpleBlock(ModBlocks.DUNESTONE_COAL_ORE.get());
        modSimpleBlock(ModBlocks.DUNESTONE_IRON_ORE.get());
        modSimpleBlock(ModBlocks.DUNESTONE_COPPER_ORE.get());
        modSimpleBlock(ModBlocks.DUNESTONE_GOLD_ORE.get());
        modSimpleBlock(ModBlocks.DUNESTONE_REDSTONE_ORE.get());
        modSimpleBlock(ModBlocks.DUNESTONE_EMERALD_ORE.get());
        modSimpleBlock(ModBlocks.DUNESTONE_LAPIS_ORE.get());
        modSimpleBlock(ModBlocks.DUNESTONE_DIAMOND_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_COAL_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_IRON_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_COPPER_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_GOLD_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_REDSTONE_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_EMERALD_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_LAPIS_ORE.get());
        modSimpleBlock(ModBlocks.PERMAFROST_DIAMOND_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_COAL_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_IRON_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_COPPER_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_GOLD_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_REDSTONE_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_EMERALD_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_LAPIS_ORE.get());
        modSimpleBlock(ModBlocks.WILDSTONE_DIAMOND_ORE.get());
        boulderBlock(ModBlocks.BOULDER.get());
        boulderBlock(ModBlocks.IRON_BOULDER.get());
        boulderBlock(ModBlocks.COPPER_BOULDER.get());
        boulderBlock(ModBlocks.GOLD_BOULDER.get());
        boulderBlock(ModBlocks.LUSH_BOULDER.get());
        boulderBlock(ModBlocks.LUSH_IRON_BOULDER.get());
        boulderBlock(ModBlocks.LUSH_COPPER_BOULDER.get());
        boulderBlock(ModBlocks.LUSH_GOLD_BOULDER.get());
        createHugeMushroomBlock(ModBlocks.CAVE_MUSHROOM_BLOCK.get());
        simpleBlockWithGeneratedItem(ModBlocks.CAVE_MUSHROOM.get());
        modCrossBlock(ModBlocks.SHORT_UNDER_BRUSH.get());
        modDoubleTallPlant(ModBlocks.TALL_UNDER_BRUSH.get());
        rockBlock(ModBlocks.CLAY_PILE.get(), ResourceLocation.withDefaultNamespace("block/clay"));
        rockBlock(ModBlocks.ROCK.get(), ResourceLocation.withDefaultNamespace("block/stone"));
        rockBlock(ModBlocks.ICE_BALL.get(), ResourceLocation.withDefaultNamespace("block/packed_ice"));
        rockBlock(ModBlocks.DEEPSLATE_ROCK.get(), ResourceLocation.withDefaultNamespace("block/deepslate"));
        rockBlock(ModBlocks.DRIPSTONE_ROCK.get(), ResourceLocation.withDefaultNamespace("block/dripstone_block"));
        rockBlock(ModBlocks.BASALT_ROCK.get(), ResourceLocation.withDefaultNamespace("block/basalt_side"));
        modSimpleBlock(ModBlocks.PACKED_WEB.get());
        vineLikeBlock(ModBlocks.WEB_VEIN.get());
        hangingBlock(ModBlocks.HANGING_WEB.get());
        spiderEgg(ModBlocks.SPIDER_EGG.get());
        simpleBlockWithItem(ModBlocks.DEEPSLATE_GEYSER.get(), models().cubeColumn(
                "deepslate_geyser",
                ResourceLocation.withDefaultNamespace("block/deepslate"),
                ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MOD_ID, "block/deepslate_geyser_top")
        ));

        simpleBlockWithItem(ModBlocks.BASALT_GEYSER.get(), models().cubeColumn(
                "basalt_geyser",
                ResourceLocation.withDefaultNamespace("block/basalt_side"),
                ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MOD_ID, "block/basalt_geyser_top")
        ));

        // Redstone Blocks
        blaster(ModBlocks.BLASTER.get());
        woodenLiftTrack(ModBlocks.WOODEN_LIFT_TRACK.get());
        poweredLiftTrack(ModBlocks.POWERED_LIFT_TRACK.get());
    }

    private void modSimpleBlock(Block block) {
        simpleBlock(block);
        itemModels().simpleBlockItem(block);
    }

    private void modSimpleStairBlock(Block block, ResourceLocation texture) {
        stairsBlock((StairBlock) block, texture);
        itemModels().simpleBlockItem(block);
    }

    private void modSimpleSlabBlock(Block block, ResourceLocation side, ResourceLocation top) {
        slabBlock((SlabBlock) block, side, top);
        itemModels().simpleBlockItem(block);
    }

    private void bottomTopBlock(Block block, ResourceLocation side, ResourceLocation bottom, ResourceLocation top) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ModelFile model = models().cubeBottomTop(name, side, top, bottom);
        simpleBlock(block, model);
    }

    private void modWallBlock(Block block, ResourceLocation texture) {
        ResourceLocation blockPath = BuiltInRegistries.BLOCK.getKey(block);
        String name = blockPath.getPath();
        wallBlock((WallBlock) block, texture);
        itemModels().wallInventory(name, texture);
    }

    /**
     * Simple block with item that is the same texture as the block
     * @param block
     */
    private void simpleBlockWithGeneratedItem(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        simpleBlock(block, models().cross(name, modLoc("block/" + name)).renderType("cutout"));
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MOD_ID, name);
        itemModels().getBuilder(item.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", SpelunkersCharm.id("block/" + item.getPath()));
    }

    /**
     * Rock block Model with item that uses item generated as its model.
     * @param block
     * @param breakParticle
     */
    private void rockBlock(Block block, ResourceLocation breakParticle) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ModelFile rockOne = models().withExistingParent(name + "_one", modLoc("block/template_rock_one")).texture("all", modLoc("block/" + name)).texture("particle", breakParticle);
        ModelFile rockTwo = models().withExistingParent(name + "_two", modLoc("block/template_rock_two")).texture("all", modLoc("block/" + name)).texture("particle", breakParticle);
        ModelFile rockThree = models().withExistingParent(name + "_three", modLoc("block/template_rock_three")).texture("all", modLoc("block/" + name)).texture("particle", breakParticle);
        ModelFile displayPile = models().withExistingParent(name + "_display", modLoc("block/template_rock_pile_display")).texture("all", modLoc("block/" + name)).texture("particle", breakParticle);

        // Skipping item model of Clay Ball because it is a vanilla item
        if (block != ModBlocks.CLAY_PILE.get()) {
            itemModels().basicItem(block.asItem());
        }

        getVariantBuilder(block).forAllStatesExcept(state -> {
            Direction facing = state.getValue(ButtonBlock.FACING);
            int rocks = state.getValue(ModBlockStateProperties.ROCKS);
            boolean display = state.getValue(ModBlockStateProperties.DISPLAY);

            ModelFile selectedModel = display
                    ? displayPile
                    : switch (rocks) {
                case 2 -> rockTwo;
                case 3 -> rockThree;
                default -> rockOne;
            };

            return ConfiguredModel.builder()
                    .modelFile(selectedModel)
                    .rotationY((int) (facing.getOpposite()).toYRot())
                    .build();
        }, BlockStateProperties.WATERLOGGED);
    }

    private void createHugeMushroomBlock(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);

        itemModels().withExistingParent(
                name,
                mcLoc("block/cube_all")
        ).texture("all", modLoc("block/cave_mushroom_block"));

        // Outer texture
        builder.part()
                .modelFile(models().singleTexture("cave_mushroom_block", mcLoc("block/template_single_face"), modLoc("block/cave_mushroom_block")))
                .addModel()
                .condition(BlockStateProperties.NORTH, true)
                .end();

        builder.part()
                .modelFile(models().singleTexture("cave_mushroom_block", mcLoc("block/template_single_face"), modLoc("block/cave_mushroom_block")))
                .rotationY(90)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.EAST, true)
                .end();

        builder.part()
                .modelFile(models().singleTexture("cave_mushroom_block", mcLoc("block/template_single_face"), modLoc("block/cave_mushroom_block")))
                .rotationY(180)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.SOUTH, true)
                .end();

        builder.part()
                .modelFile(models().singleTexture("cave_mushroom_block", mcLoc("block/template_single_face"), modLoc("block/cave_mushroom_block")))
                .rotationY(270)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.WEST, true)
                .end();

        builder.part()
                .modelFile(models().singleTexture("cave_mushroom_block", mcLoc("block/template_single_face"), modLoc("block/cave_mushroom_block")))
                .rotationX(270)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.UP, true)
                .end();

        builder.part()
                .modelFile(models().singleTexture("cave_mushroom_block", mcLoc("block/template_single_face"), modLoc("block/cave_mushroom_block")))
                .rotationX(90)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.DOWN, true)
                .end();

        // Inner texture (mushroom block inside)
        builder.part()
                .modelFile(models().getExistingFile(mcLoc("block/mushroom_block_inside")))
                .addModel()
                .condition(BlockStateProperties.NORTH, false)
                .end();

        builder.part()
                .modelFile(models().getExistingFile(mcLoc("block/mushroom_block_inside")))
                .rotationY(90)
                .addModel()
                .condition(BlockStateProperties.EAST, false)
                .end();

        builder.part()
                .modelFile(models().getExistingFile(mcLoc("block/mushroom_block_inside")))
                .rotationY(180)
                .addModel()
                .condition(BlockStateProperties.SOUTH, false)
                .end();

        builder.part()
                .modelFile(models().getExistingFile(mcLoc("block/mushroom_block_inside")))
                .rotationY(270)
                .addModel()
                .condition(BlockStateProperties.WEST, false)
                .end();

        builder.part()
                .modelFile(models().getExistingFile(mcLoc("block/mushroom_block_inside")))
                .rotationX(270)
                .addModel()
                .condition(BlockStateProperties.UP, false)
                .end();

        builder.part()
                .modelFile(models().getExistingFile(mcLoc("block/mushroom_block_inside")))
                .rotationX(90)
                .addModel()
                .condition(BlockStateProperties.DOWN, false)
                .end();
    }

    // This is just copied from the vanilla pointed dripstone block state provider, but with different textures.
    private void icicle(Block block, ResourceLocation breakParticle) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();

        ModelFile down_base = models().withExistingParent(name + "_down_base", mcLoc("block/pointed_dripstone_down_base")).texture("cross", modLoc("block/icicle_down_base")).texture("particle", breakParticle).renderType("cutout");
        ModelFile up_base = models().withExistingParent(name + "_up_base", mcLoc("block/pointed_dripstone_up_base")).texture("cross", modLoc("block/icicle_up_base")).texture("particle", breakParticle).renderType("cutout");
        ModelFile down_frustum = models().withExistingParent(name + "_down_frustum", mcLoc("block/pointed_dripstone_down_frustum")).texture("cross", modLoc("block/icicle_down_frustum")).texture("particle", breakParticle).renderType("cutout");
        ModelFile up_frustum = models().withExistingParent(name + "_up_frustum", mcLoc("block/pointed_dripstone_up_frustum")).texture("cross", modLoc("block/icicle_up_frustum")).texture("particle", breakParticle).renderType("cutout");
        ModelFile down_middle = models().withExistingParent(name + "_down_middle", mcLoc("block/pointed_dripstone_down_middle")).texture("cross", modLoc("block/icicle_down_middle")).texture("particle", breakParticle).renderType("cutout");
        ModelFile up_middle = models().withExistingParent(name + "_up_middle", mcLoc("block/pointed_dripstone_up_middle")).texture("cross", modLoc("block/icicle_up_middle")).texture("particle", breakParticle).renderType("cutout");
        ModelFile down_tip = models().withExistingParent(name + "_down_tip", mcLoc("block/pointed_dripstone_down_tip")).texture("cross", modLoc("block/icicle_down_tip")).texture("particle", breakParticle).renderType("cutout");
        ModelFile up_tip = models().withExistingParent(name + "_up_tip", mcLoc("block/pointed_dripstone_up_tip")).texture("cross", modLoc("block/icicle_up_tip")).texture("particle", breakParticle).renderType("cutout");
        ModelFile down_tip_merge = models().withExistingParent(name + "_down_tip_merge", mcLoc("block/pointed_dripstone_down_tip_merge")).texture("cross", modLoc("block/icicle_down_tip_merge")).texture("particle", breakParticle).renderType("cutout");
        ModelFile up_tip_merge = models().withExistingParent(name + "_up_tip_merge", mcLoc("block/pointed_dripstone_up_tip_merge")).texture("cross", modLoc("block/icicle_up_tip_merge")).texture("particle", breakParticle).renderType("cutout");

        itemModels().basicItem(block.asItem());

        getVariantBuilder(block).forAllStatesExcept(state -> {
            DripstoneThickness thickness = state.getValue(BlockStateProperties.DRIPSTONE_THICKNESS);
            Direction vertical_direction = state.getValue(BlockStateProperties.VERTICAL_DIRECTION);

            ModelFile model = switch (thickness) {
                case BASE -> vertical_direction == Direction.DOWN ? down_base : up_base;
                case FRUSTUM -> vertical_direction == Direction.DOWN ? down_frustum : up_frustum;
                case MIDDLE -> vertical_direction == Direction.DOWN ? down_middle : up_middle;
                case TIP -> vertical_direction == Direction.DOWN ? down_tip : up_tip;
                case TIP_MERGE -> vertical_direction == Direction.DOWN ? down_tip_merge : up_tip_merge;
            };

            return ConfiguredModel.builder()
                    .modelFile(model)
                    .build();
        }, BlockStateProperties.WATERLOGGED);
    }

    private void vineLikeBlock(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath(); // e.g., "web_vein"
        String textureName = "block/" + blockName; // e.g., "block/web_vein"

        itemModels().getBuilder(blockName)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", SpelunkersCharm.id(textureName));

        getMultipartBuilder(block)
                .part()
                .modelFile(models().withExistingParent(blockName + "_north", mcLoc("block/sculk_vein")).renderType("cutout")
                        .texture("sculk_vein", modLoc(textureName))
                        .texture("particle", modLoc(textureName)))
                .addModel()
                .condition(BlockStateProperties.NORTH, true)
                .end()
                .part()
                .modelFile(models().withExistingParent(blockName + "_east", mcLoc("block/sculk_vein")).renderType("cutout")
                        .texture("sculk_vein", modLoc(textureName))
                        .texture("particle", modLoc(textureName)))
                .rotationY(90)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.EAST, true)
                .end()
                .part()
                .modelFile(models().withExistingParent(blockName + "_south", mcLoc("block/sculk_vein")).renderType("cutout")
                        .texture("sculk_vein", modLoc(textureName))
                        .texture("particle", modLoc(textureName)))
                .rotationY(180)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.SOUTH, true)
                .end()
                .part()
                .modelFile(models().withExistingParent(blockName + "_west", mcLoc("block/sculk_vein")).renderType("cutout")
                        .texture("sculk_vein", modLoc(textureName))
                        .texture("particle", modLoc(textureName)))
                .rotationY(270)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.WEST, true)
                .end()
                .part()
                .modelFile(models().withExistingParent(blockName + "_up", mcLoc("block/sculk_vein")).renderType("cutout")
                        .texture("sculk_vein", modLoc(textureName))
                        .texture("particle", modLoc(textureName)))
                .rotationX(270)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.UP, true)
                .end()
                .part()
                .modelFile(models().withExistingParent(blockName + "_down", mcLoc("block/sculk_vein")).renderType("cutout")
                        .texture("sculk_vein", modLoc(textureName))
                        .texture("particle", modLoc(textureName)))
                .rotationX(90)
                .uvLock(true)
                .addModel()
                .condition(BlockStateProperties.DOWN, true)
                .end();
    }

    private void spiderEgg(Block block) {
        ModelFile stageOne = models().withExistingParent("spider_egg_stage_0", modLoc("block/spider_egg"))
                .texture("all", modLoc("block/spider_egg_stage_0"))
                .texture("particle", modLoc("block/spider_egg_break"))
                .renderType("cutout");
        ModelFile stageTwo = models().withExistingParent("spider_egg_stage_1", modLoc("block/spider_egg"))
                .texture("all", modLoc("block/spider_egg_stage_1"))
                .texture("particle", modLoc("block/spider_egg_break"))
                .renderType("cutout");
        ModelFile stageThree = models().withExistingParent("spider_egg_stage_2", modLoc("block/spider_egg"))
                .texture("all", modLoc("block/spider_egg_stage_2"))
                .texture("particle", modLoc("block/spider_egg_break"))
                .renderType("cutout");

        itemModels().basicItem(block.asItem());

        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction direction = state.getValue(BlockStateProperties.FACING);
                    int stage = state.getValue(BlockStateProperties.HATCH);

                    ModelFile model = switch (stage) {
                        case 1 -> stageTwo;
                        case 2 -> stageThree;
                        default -> stageOne;
                    };

                    return ConfiguredModel.builder()
                            .modelFile(model)
                            .rotationX(direction == Direction.DOWN ? 180 : direction.getAxis().isHorizontal() ? 90 : 0)
                            .rotationY(direction.getAxis().isVertical() ? 0 : (((int) direction.toYRot()) + 180) % 360)
                            .build();
                });
    }

    private void hangingBlock(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath(); // e.g., "web_vein"
        String textureName = "block/" + blockName; // e.g., "block/web_vein"

        ResourceLocation endTexture = modLoc("block/" + blockName + "_end");
        ResourceLocation mainTexture = modLoc("block/" + blockName);

        // Use vanilla cross model and point it to the appropriate texture
        ModelFile endModel = models().cross(blockName + "_end", endTexture).renderType("cutout");
        ModelFile mainModel = models().cross(blockName, mainTexture).renderType("cutout");

        itemModels().getBuilder(blockName)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", SpelunkersCharm.id(textureName));

        getVariantBuilder(block).forAllStates(state -> {
            boolean end = state.getValue(HangingBlock.END);

            ModelFile selectedModel = end
                    ? endModel
                    : mainModel;

            return ConfiguredModel.builder()
                    .modelFile(selectedModel)
                    .build();
        });
    }

    private void woodenLiftTrack(Block block) {
        ModelFile blockModel = models().withExistingParent("wooden_lift_track", modLoc("block/template_lift_track"))
                .texture("front", modLoc("block/wooden_lift_track_front"))
                .texture("side", modLoc("block/wooden_lift_track_side"))
                .texture("end", modLoc("block/wooden_lift_track_end"))
                .texture("particle", modLoc("block/wooden_lift_track_front"))
                .renderType("cutout");

        horizontalBlock(block, blockModel);
        itemModels().simpleBlockItem(block);
    }

    private void poweredLiftTrack(Block block) {
        ModelFile blockModel = models().withExistingParent("powered_lift_track", modLoc("block/template_lift_track"))
                .texture("front", modLoc("block/powered_lift_track_front"))
                .texture("side", modLoc("block/powered_lift_track_side"))
                .texture("end", modLoc("block/powered_lift_track_end"))
                .texture("particle", modLoc("block/powered_lift_track_front"))
                .renderType("cutout");
        ModelFile poweredBlockModel = models().withExistingParent("powered_lift_track_powered", modLoc("block/template_lift_track"))
                .texture("front", modLoc("block/powered_lift_track_front_powered"))
                .texture("side", modLoc("block/powered_lift_track_side"))
                .texture("end", modLoc("block/powered_lift_track_end_powered"))
                .texture("particle", modLoc("block/powered_lift_track_front_powered"))
                .renderType("cutout");

        itemModels().simpleBlockItem(block);

        getVariantBuilder(block).forAllStates(state -> {
            boolean powered = state.getValue(PoweredLiftTrackBlock.POWERED);

            ModelFile selectedModel = powered
                    ? poweredBlockModel
                    : blockModel;

            return ConfiguredModel.builder()
                    .modelFile(selectedModel)
                    .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360)
                    .build();
        });
    }

    private void boulderBlock(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath(); // e.g., "web_vein"
        String textureName = "block/boulder/" + blockName; // e.g., "block/web_vein"

        // Normal Models
        ModelFile bottomCorner = models().withExistingParent(blockName + "_bottom_corner", modLoc("block/template_boulder_bottom_corner"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile bottomSide = models().withExistingParent(blockName + "_bottom_side", modLoc("block/template_boulder_bottom_side"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile bottomMiddle = models().withExistingParent(blockName + "_bottom_middle", modLoc("block/template_boulder_bottom_middle"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile middleCorner = models().withExistingParent(blockName + "_middle_corner", modLoc("block/template_boulder_middle_corner"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile middleSide = models().withExistingParent(blockName + "_middle_side", modLoc("block/template_boulder_middle_side"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topCorner = models().withExistingParent(blockName + "_top_corner", modLoc("block/template_boulder_top_corner"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topSide = models().withExistingParent(blockName + "_top_side", modLoc("block/template_boulder_top_side"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topMiddle = models().withExistingParent(blockName + "_top_middle", modLoc("block/template_boulder_top_middle"))
                .texture("all", textureName)
                .texture("particle", modLoc("block/boulder_break"));

        // Tied Models
        ModelFile bottomCorner_tied = models().withExistingParent(blockName + "_bottom_corner_tied", modLoc("block/template_boulder_bottom_corner"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile bottomSide_tied = models().withExistingParent(blockName + "_bottom_side_tied", modLoc("block/template_boulder_bottom_side"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile bottomMiddle_tied = models().withExistingParent(blockName + "_bottom_middle_tied", modLoc("block/template_boulder_bottom_middle"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile middleCorner_tied = models().withExistingParent(blockName + "_middle_corner_tied", modLoc("block/template_boulder_middle_corner"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile middleSide_tied = models().withExistingParent(blockName + "_middle_side_tied", modLoc("block/template_boulder_middle_side"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topCorner_tied = models().withExistingParent(blockName + "_top_corner_tied", modLoc("block/template_boulder_top_corner"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topSide_tied = models().withExistingParent(blockName + "_top_side_tied", modLoc("block/template_boulder_top_side"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topMiddle_tied = models().withExistingParent(blockName + "_top_middle_tied", modLoc("block/template_boulder_top_middle"))
                .texture("all", textureName + "_tied")
                .texture("particle", modLoc("block/boulder_break"));

        // Chained Models
        ModelFile bottomCorner_chained = models().withExistingParent(blockName + "_bottom_corner_chained", modLoc("block/template_boulder_bottom_corner"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile bottomSide_chained = models().withExistingParent(blockName + "_bottom_side_chained", modLoc("block/template_boulder_bottom_side"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile bottomMiddle_chained = models().withExistingParent(blockName + "_bottom_middle_chained", modLoc("block/template_boulder_bottom_middle"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile middleCorner_chained = models().withExistingParent(blockName + "_middle_corner_chained", modLoc("block/template_boulder_middle_corner"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile middleSide_chained = models().withExistingParent(blockName + "_middle_side_chained", modLoc("block/template_boulder_middle_side"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topCorner_chained = models().withExistingParent(blockName + "_top_corner_chained", modLoc("block/template_boulder_top_corner"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topSide_chained = models().withExistingParent(blockName + "_top_side_chained", modLoc("block/template_boulder_top_side"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));
        ModelFile topMiddle_chained = models().withExistingParent(blockName + "_top_middle_chained", modLoc("block/template_boulder_top_middle"))
                .texture("all", textureName + "_chained")
                .texture("particle", modLoc("block/boulder_break"));

        itemModels().withExistingParent(blockName, modLoc("block/template_boulder_bottom_corner")).texture("all", textureName);

        getVariantBuilder(block).forAllStatesExcept(state -> {
            BlockPart blockPart = state.getValue(BoulderBlock.BLOCK_PART);
            HangingType hangingType = state.getValue(BoulderBlock.HANGING_TYPE);

            ModelFile modelFile = switch (blockPart) {
                case BOTTOM_CORNER -> switch (hangingType) {
                    case TIED -> bottomCorner_tied;
                    case CHAINED -> bottomCorner_chained;
                    default -> bottomCorner;
                };
                case BOTTOM_SIDE -> switch (hangingType) {
                    case TIED -> bottomSide_tied;
                    case CHAINED -> bottomSide_chained;
                    default -> bottomSide;
                };
                case BOTTOM_MIDDLE -> switch (hangingType) {
                    case TIED -> bottomMiddle_tied;
                    case CHAINED -> bottomMiddle_chained;
                    default -> bottomMiddle;
                };
                case MIDDLE_CORNER -> switch (hangingType) {
                    case TIED -> middleCorner_tied;
                    case CHAINED -> middleCorner_chained;
                    default -> middleCorner;
                };
                case MIDDLE_SIDE -> switch (hangingType) {
                    case TIED -> middleSide_tied;
                    case CHAINED -> middleSide_chained;
                    default -> middleSide;
                };
                case TOP_CORNER -> switch (hangingType) {
                    case TIED -> topCorner_tied;
                    case CHAINED -> topCorner_chained;
                    default -> topCorner;
                };
                case TOP_SIDE -> switch (hangingType) {
                    case TIED -> topSide_tied;
                    case CHAINED -> topSide_chained;
                    default -> topSide;
                };
                case TOP_MIDDLE -> switch (hangingType) {
                    case TIED -> topMiddle_tied;
                    case CHAINED -> topMiddle_chained;
                    default -> topMiddle;
                };
            };

            return ConfiguredModel.builder()
                    .modelFile(modelFile)
                    .rotationY(((int) state.getValue(BlockStateProperties.FACING).toYRot() + 180) % 360)
                    .build();
        }, BlockStateProperties.WATERLOGGED, BoulderBlock.GENERATE);
    }

    private void modCrossBlock(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath(); // e.g., "web_vein"
        String textureName = "block/" + blockName; // e.g., "block/web_vein"

        itemModels().getBuilder(blockName)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", SpelunkersCharm.id(textureName));

        simpleBlock(block, models().withExistingParent(blockName, mcLoc("block/cross")).texture("cross", textureName).renderType("cutout"));
    }

    private void modDoubleTallPlant(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath(); // e.g., "web_vein"
        String textureName = "block/" + blockName; // e.g., "block/web_vein"

        ModelFile bottomPart = models().withExistingParent(blockName + "_bottom", mcLoc("block/cross"))
                .texture("cross", textureName + "_bottom");
        ModelFile topPart = models().withExistingParent(blockName + "_top", modLoc("block/template_boulder_top_middle"))
                .texture("cross", textureName + "_top");

        itemModels().getBuilder(blockName)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", SpelunkersCharm.id(textureName + "_top"));

        getVariantBuilder(block).forAllStates(state -> {
            DoubleBlockHalf blockPart = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);

            ModelFile modelFile = switch (blockPart) {
                case UPPER -> topPart;
                case LOWER -> bottomPart;
            };

            return ConfiguredModel.builder()
                    .modelFile(modelFile)
                    .build();
        });
    }

    private void blaster(Block block) {
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath(); // e.g., "web_vein"

        ModelFile offModel = models().cubeBottomTop(blockName + "_off", SpelunkersCharm.id("block/blaster_side_off"), mcLoc("block/crafter_bottom"), SpelunkersCharm.id("block/blaster_top_off"));
        ModelFile onModel = models().cubeBottomTop(blockName + "_on", SpelunkersCharm.id("block/blaster_side_on"), mcLoc("block/crafter_bottom"), SpelunkersCharm.id("block/blaster_top_on"));

        getVariantBuilder(block).forAllStates(state -> {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            Boolean triggered = state.getValue(BlockStateProperties.TRIGGERED);

            return ConfiguredModel.builder()
                    .modelFile(triggered ? onModel : offModel)
                    .rotationX(dir == Direction.DOWN ? 180 : dir.getAxis().isHorizontal() ? 90 : 0)
                    .rotationY(dir.getAxis().isVertical() ? 0 : (((int) dir.toYRot()) + 180) % 360)
                    .build();
        });

        itemModels().withExistingParent("blaster", modLoc("block/blaster_off"));
    }
}
