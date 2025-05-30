package net.luckystudio.splelunkers_charm.datagen;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.datagen.itemTags.ModItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, SpelunkersCharm.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(ModItemTags.MINERALS)
                .add(Items.IRON_INGOT)
                .add(Items.COPPER_INGOT)
                .add(Items.GOLD_INGOT)
                .add(Items.DIAMOND)
                .add(Items.NETHERITE_INGOT)
                .addOptional(ResourceLocation.fromNamespaceAndPath("create", "brass_ingot"));
        ;
    }
}
