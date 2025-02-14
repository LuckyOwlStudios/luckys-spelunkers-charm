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
    }

    /**
     * Simple block with item that is the same texture as the block
     * @param block
     */
    private void simpleBlockWithGeneratedItem(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        simpleBlock(block, models().cross(name, SpelunkersCharm.id("block/" + name)).renderType("cutout"));
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
    public void rockBlock(Block block, ResourceLocation breakParticle) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        ModelFile rockOne = models().withExistingParent(name + "_one", SpelunkersCharm.id("block/template_rock_one")).texture("all", SpelunkersCharm.id("block/" + name)).texture("particle", breakParticle);
        ModelFile rockTwo = models().withExistingParent(name + "_two", SpelunkersCharm.id("block/template_rock_two")).texture("all", SpelunkersCharm.id("block/" + name)).texture("particle", breakParticle);
        ModelFile rockThree = models().withExistingParent(name + "_three", SpelunkersCharm.id("block/template_rock_three")).texture("all", SpelunkersCharm.id("block/" + name)).texture("particle", breakParticle);

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
}
