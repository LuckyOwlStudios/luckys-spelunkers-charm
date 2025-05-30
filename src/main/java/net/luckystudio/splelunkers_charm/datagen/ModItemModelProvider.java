package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, SpelunkersCharm.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.MINING_HELMET.get());
        simpleBlockItem(ModBlocks.COLD_STONE.get());
        simpleBlockItem(ModBlocks.SILT.get());
    }
}
