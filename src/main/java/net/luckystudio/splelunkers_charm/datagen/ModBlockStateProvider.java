package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, SpelunkersCharm.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.DEEPSLATE_GEYSER.get(), models().cubeColumn("deepslate_geyser", ResourceLocation.withDefaultNamespace("block/deepslate"), ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MODID,"block/deepslate_geyser_top")));
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
