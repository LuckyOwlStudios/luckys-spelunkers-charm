package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.neoforged.neoforge.client.model.generators.*;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public static final String BLOCK_FOLDER = "block";

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, SpelunkersCharm.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Cave Blocks
        rockBlock(ModBlocks.CLAY_BALL.get(), ResourceLocation.withDefaultNamespace("block/clay"));
        rockBlock(ModBlocks.ROCK.get(), ResourceLocation.withDefaultNamespace("block/stone"));
        rockBlock(ModBlocks.DEEPSLATE_ROCK.get(), ResourceLocation.withDefaultNamespace("block/deepslate"));
        rockBlock(ModBlocks.DRIPSTONE_ROCK.get(), ResourceLocation.withDefaultNamespace("block/dripstone_block"));
        rockBlock(ModBlocks.BASALT_ROCK.get(), ResourceLocation.withDefaultNamespace("block/basalt_side"));
        simpleBlockWithItem(ModBlocks.DEEPSLATE_GEYSER.get(), models().cubeColumn("deepslate_geyser", ResourceLocation.withDefaultNamespace("block/deepslate"), ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MOD_ID,"block/deepslate_geyser_top")));
        simpleBlockWithItem(ModBlocks.BASALT_GEYSER.get(), models().cubeColumn("basalt_geyser", ResourceLocation.withDefaultNamespace("block/basalt_side"), ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MOD_ID,"block/basalt_geyser_top")));
        simpleBlockWithGeneratedItem(ModBlocks.CAVE_MUSHROOM.get());
        createHugeMushroomBlock(ModBlocks.CAVE_MUSHROOM_BLOCK.get());

        // Ice Cave Blocks
        simpleBlock(ModBlocks.COLD_STONE.get());
        simpleBlock(ModBlocks.SILT.get());
        icicle(ModBlocks.ICICLE.get(), mcLoc("block/packed_ice"));
        directionalBlock(ModBlocks.AQUAMARINE_CLUSTER.get(), models().withExistingParent("aquamarine_cluster", mcLoc("block/amethyst_cluster")).texture("cross", modLoc("block/aquamarine_cluster")).renderType("cutout"));

        // Desert Cave Blocks
        simpleBlock(ModBlocks.HOT_STONE.get());
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
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "block/" + item.getPath()));
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

        // Skipping item model of Clay Ball because it is a vanilla item
        if (block != ModBlocks.CLAY_BALL.get()) {
            itemModels().basicItem(block.asItem());
        }

        getVariantBuilder(block).forAllStatesExcept(state -> {
            Direction facing = state.getValue(ButtonBlock.FACING);
            int rocks = state.getValue(ModBlockStateProperties.ROCKS);

            return ConfiguredModel.builder()
                    .modelFile(switch (rocks) {
                        case 2 -> rockTwo;
                        case 3 -> rockThree;
                        default -> rockOne;
                    })
                    .rotationY((int) (facing.getOpposite()).toYRot())
                    .build();
        }, BlockStateProperties.WATERLOGGED);
    }

    private void createHugeMushroomBlock(Block block) {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(block);

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
}
