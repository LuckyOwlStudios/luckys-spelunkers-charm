package net.luckystudio.splelunkers_charm.datagen.blockTags;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, SpelunkersCharm.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.COLD_STONE.get())
                .add(ModBlocks.AQUAMARINE_CLUSTER.get())
                .add(ModBlocks.HOT_STONE.get())
                .add(ModBlocks.DEEPSLATE_GEYSER.get())
                .add(ModBlocks.BASALT_GEYSER.get())
        ;

        tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(ModBlocks.SILT.get());

        tag(BlockTags.DRIPSTONE_REPLACEABLE)
                .add(ModBlocks.COLD_STONE.get())
                .add(ModBlocks.HOT_STONE.get())
        ;

        tag(BlockTags.BASE_STONE_OVERWORLD)
                .add(ModBlocks.COLD_STONE.get())
                .add(ModBlocks.HOT_STONE.get())
        ;

        // This is so all ores still generate over the new stones.
        tag(BlockTags.STONE_ORE_REPLACEABLES)
                .add(ModBlocks.COLD_STONE.get())
                .add(ModBlocks.HOT_STONE.get())
        ;
    }
}
