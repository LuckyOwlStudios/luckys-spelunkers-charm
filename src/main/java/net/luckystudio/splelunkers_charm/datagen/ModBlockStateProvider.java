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
import net.neoforged.neoforge.client.model.generators.*;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public static final String BLOCK_FOLDER = "block";

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, SpelunkersCharm.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        rockBlock(ModBlocks.CLAY_BALL.get(), ResourceLocation.withDefaultNamespace("block/clay"));
        rockBlock(ModBlocks.ROCK.get(), ResourceLocation.withDefaultNamespace("block/stone"));
        rockBlock(ModBlocks.DEEPSLATE_ROCK.get(), ResourceLocation.withDefaultNamespace("block/deepslate"));
        rockBlock(ModBlocks.DRIPSTONE_ROCK.get(), ResourceLocation.withDefaultNamespace("block/dripstone_block"));
        rockBlock(ModBlocks.BASALT_ROCK.get(), ResourceLocation.withDefaultNamespace("block/basalt_side"));
        simpleBlockWithItem(ModBlocks.DEEPSLATE_GEYSER.get(), models().cubeColumn("deepslate_geyser", ResourceLocation.withDefaultNamespace("block/deepslate"), ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MODID,"block/deepslate_geyser_top")));
        simpleBlockWithItem(ModBlocks.BASALT_GEYSER.get(), models().cubeColumn("basalt_geyser", ResourceLocation.withDefaultNamespace("block/basalt_side"), ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MODID,"block/basalt_geyser_top")));
        simpleBlockWithGeneratedItem(ModBlocks.CAVE_MUSHROOM.get());
        createHugeMushroomBlock(ModBlocks.CAVE_MUSHROOM_BLOCK.get());
    }

    /**
     * Simple block with item that is the same texture as the block
     * @param block
     */
    private void simpleBlockWithGeneratedItem(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        simpleBlock(block, models().cross(name, modLoc("block/" + name)).renderType("cutout"));
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MODID, name);
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
}
