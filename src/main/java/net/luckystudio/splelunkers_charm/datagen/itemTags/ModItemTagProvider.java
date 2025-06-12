package net.luckystudio.splelunkers_charm.datagen.itemTags;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.datagen.blockTags.ModBlockTags;
import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
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

        // Mod Item Tags
        // This is used on the lift to determine what items can be used on the lift to change its appearance
        this.tag(ModItemTags.MINERALS)
                .add(Items.IRON_INGOT)
                .add(Items.COPPER_INGOT)
                .add(Items.GOLD_INGOT)
                .add(Items.DIAMOND)
                .add(Items.NETHERITE_INGOT)
                .addOptional(ResourceLocation.fromNamespaceAndPath("create", "brass_ingot"))
        ;

        tag(ModItemTags.GEYSERS)
                .add(ModBlocks.DEEPSLATE_GEYSER.get().asItem())
                .add(ModBlocks.BASALT_GEYSER.get().asItem())
        ;

        this.tag(ItemTags.COAL_ORES)
                .add(ModBlocks.DUNESTONE_COAL_ORE.asItem())
                .add(ModBlocks.PERMAFROST_COAL_ORE.asItem())
                .add(ModBlocks.WILDSTONE_COAL_ORE.asItem())
        ;

        this.tag(ItemTags.IRON_ORES)
                .add(ModBlocks.DUNESTONE_IRON_ORE.asItem())
                .add(ModBlocks.PERMAFROST_IRON_ORE.asItem())
                .add(ModBlocks.WILDSTONE_IRON_ORE.asItem())
        ;

        this.tag(ItemTags.COPPER_ORES)
                .add(ModBlocks.DUNESTONE_COPPER_ORE.asItem())
                .add(ModBlocks.PERMAFROST_COPPER_ORE.asItem())
                .add(ModBlocks.WILDSTONE_COPPER_ORE.asItem())
        ;

        this.tag(ItemTags.GOLD_ORES)
                .add(ModBlocks.DUNESTONE_GOLD_ORE.asItem())
                .add(ModBlocks.PERMAFROST_GOLD_ORE.asItem())
                .add(ModBlocks.WILDSTONE_GOLD_ORE.asItem())
        ;

        this.tag(ItemTags.REDSTONE_ORES)
                .add(ModBlocks.DUNESTONE_REDSTONE_ORE.asItem())
                .add(ModBlocks.PERMAFROST_REDSTONE_ORE.asItem())
                .add(ModBlocks.WILDSTONE_REDSTONE_ORE.asItem())
        ;

        this.tag(ItemTags.LAPIS_ORES)
                .add(ModBlocks.DUNESTONE_LAPIS_ORE.asItem())
                .add(ModBlocks.PERMAFROST_LAPIS_ORE.asItem())
                .add(ModBlocks.WILDSTONE_LAPIS_ORE.asItem())
        ;

        this.tag(ItemTags.EMERALD_ORES)
                .add(ModBlocks.DUNESTONE_EMERALD_ORE.asItem())
                .add(ModBlocks.PERMAFROST_EMERALD_ORE.asItem())
                .add(ModBlocks.WILDSTONE_EMERALD_ORE.asItem())
        ;

        this.tag(ItemTags.DIAMOND_ORES)
                .add(ModBlocks.DUNESTONE_DIAMOND_ORE.asItem())
                .add(ModBlocks.PERMAFROST_DIAMOND_ORE.asItem())
                .add(ModBlocks.WILDSTONE_DIAMOND_ORE.asItem())
        ;

        this.tag(ItemTags.STONE_BRICKS)
                .add(ModBlocks.AMETHYST_BRICKS.asItem())
                .add(ModBlocks.DUNESTONE_BRICKS.asItem())
                .add(ModBlocks.PERMAFROST_BRICKS.asItem())
                .add(ModBlocks.WILDSTONE_BRICKS.asItem())
        ;

        // Place modded cobblestones here
        this.tag(ItemTags.STONE_TOOL_MATERIALS)
                .add(ModBlocks.COBBLED_DUNESTONE.asItem())
                .add(ModBlocks.COBBLED_PERMAFROST.asItem())
                .add(ModBlocks.COBBLED_WILDSTONE.asItem())
        ;

        // Place modded cobblestones here
        this.tag(ItemTags.STONE_CRAFTING_MATERIALS)
                .add(ModBlocks.COBBLED_DUNESTONE.asItem())
                .add(ModBlocks.COBBLED_PERMAFROST.asItem())
                .add(ModBlocks.COBBLED_WILDSTONE.asItem())
        ;

        this.tag(ItemTags.WALLS)
                .add(ModBlocks.DUNESTONE_WALL.asItem())
                .add(ModBlocks.PERMAFROST_WALL.asItem())
                .add(ModBlocks.WILDSTONE_WALL.asItem())
                .add(ModBlocks.DUNESTONE_BRICK_WALL.asItem())
                .add(ModBlocks.PERMAFROST_BRICK_WALL.asItem())
                .add(ModBlocks.WILDSTONE_BRICK_WALL.asItem())
                .add(ModBlocks.COBBLED_DUNESTONE_WALL.asItem())
                .add(ModBlocks.COBBLED_PERMAFROST_WALL.asItem())
                .add(ModBlocks.COBBLED_WILDSTONE_WALL.asItem())
        ;

        this.tag(ItemTags.STAIRS)
                .add(ModBlocks.DUNESTONE_STAIRS.asItem())
                .add(ModBlocks.PERMAFROST_STAIRS.asItem())
                .add(ModBlocks.WILDSTONE_STAIRS.asItem())
                .add(ModBlocks.DUNESTONE_BRICK_STAIRS.asItem())
                .add(ModBlocks.PERMAFROST_BRICK_STAIRS.asItem())
                .add(ModBlocks.WILDSTONE_BRICK_STAIRS.asItem())
                .add(ModBlocks.COBBLED_DUNESTONE_STAIRS.asItem())
                .add(ModBlocks.COBBLED_PERMAFROST_STAIRS.asItem())
                .add(ModBlocks.COBBLED_WILDSTONE_STAIRS.asItem())
        ;

        this.tag(ItemTags.SLABS)
                .add(ModBlocks.DUNESTONE_SLAB.asItem())
                .add(ModBlocks.PERMAFROST_SLAB.asItem())
                .add(ModBlocks.WILDSTONE_SLAB.asItem())
                .add(ModBlocks.DUNESTONE_BRICK_SLAB.asItem())
                .add(ModBlocks.PERMAFROST_BRICK_SLAB.asItem())
                .add(ModBlocks.WILDSTONE_BRICK_SLAB.asItem())
                .add(ModBlocks.COBBLED_DUNESTONE_SLAB.asItem())
                .add(ModBlocks.COBBLED_PERMAFROST_SLAB.asItem())
                .add(ModBlocks.COBBLED_WILDSTONE_SLAB.asItem())
        ;
    }
}
