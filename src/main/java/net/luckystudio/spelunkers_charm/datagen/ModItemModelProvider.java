package net.luckystudio.spelunkers_charm.datagen;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModItems;
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
        basicItem(ModItems.SMALL_LIFT.get());
        basicItem(ModItems.MEDIUM_LIFT.get());
        basicItem(ModItems.LARGE_LIFT.get());
    }
}
