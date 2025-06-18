package net.luckystudio.spelunkers_charm.datagen.blockTags;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
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

        tag(BlockTags.REPLACEABLE)
                .add(ModBlocks.HANGING_WEB.get())
                .add(ModBlocks.WEB_VEIN.get())
        ;

        // Mod Tags
        // Used for the worldgen stone overrides
        tag(ModBlockTags.OVERWORLD_REPLACEABLE)
                .add(Blocks.STONE)
//                .add(Blocks.COAL_ORE)
//                .add(Blocks.IRON_ORE)
//                .add(Blocks.COPPER_ORE)
//                .add(Blocks.GOLD_ORE)
//                .add(Blocks.REDSTONE_ORE)
//                .add(Blocks.EMERALD_ORE)
//                .add(Blocks.LAPIS_ORE)
//                .add(Blocks.DIAMOND_ORE)
        ;

        tag(ModBlockTags.DUNESTONE_REPLACEABLE)
                .addTag(ModBlockTags.OVERWORLD_REPLACEABLE)
                .add(Blocks.DIORITE)
                .add(Blocks.ANDESITE)
        ;

        tag(ModBlockTags.WILDSTONE_REPLACEABLE)
                .addTag(ModBlockTags.OVERWORLD_REPLACEABLE)
                .add(Blocks.DIORITE)
                .add(Blocks.ANDESITE)
        ;

        tag(ModBlockTags.PERMAFROST_REPLACEABLE)
                .addTag(ModBlockTags.OVERWORLD_REPLACEABLE)
                .add(Blocks.GRANITE)
        ;

        // TOOLS
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.DUNESTONE.get())
                .add(ModBlocks.DUNESTONE_STAIRS.get())
                .add(ModBlocks.DUNESTONE_SLAB.get())
                .add(ModBlocks.DUNESTONE_WALL.get())
                .add(ModBlocks.DUNESTONE_BRICKS.get())
                .add(ModBlocks.DUNESTONE_BRICK_STAIRS.get())
                .add(ModBlocks.DUNESTONE_BRICK_SLAB.get())
                .add(ModBlocks.DUNESTONE_BRICK_WALL.get())
                .add(ModBlocks.CHISELED_DUNESTONE_BRICKS.get())
                .add(ModBlocks.COBBLED_DUNESTONE.get())
                .add(ModBlocks.COBBLED_DUNESTONE_STAIRS.get())
                .add(ModBlocks.COBBLED_DUNESTONE_SLAB.get())
                .add(ModBlocks.COBBLED_DUNESTONE_WALL.get())

                .add(ModBlocks.PERMAFROST.get())
                .add(ModBlocks.PERMAFROST_STAIRS.get())
                .add(ModBlocks.PERMAFROST_SLAB.get())
                .add(ModBlocks.PERMAFROST_WALL.get())
                .add(ModBlocks.PERMAFROST_BRICKS.get())
                .add(ModBlocks.PERMAFROST_BRICK_STAIRS.get())
                .add(ModBlocks.PERMAFROST_BRICK_SLAB.get())
                .add(ModBlocks.PERMAFROST_BRICK_WALL.get())
                .add(ModBlocks.CHISELED_PERMAFROST_BRICKS.get())
                .add(ModBlocks.COBBLED_PERMAFROST.get())
                .add(ModBlocks.COBBLED_PERMAFROST_STAIRS.get())
                .add(ModBlocks.COBBLED_PERMAFROST_SLAB.get())
                .add(ModBlocks.COBBLED_PERMAFROST_WALL.get())

                .add(ModBlocks.WILDSTONE.get())
                .add(ModBlocks.WILDSTONE_STAIRS.get())
                .add(ModBlocks.WILDSTONE_SLAB.get())
                .add(ModBlocks.WILDSTONE_WALL.get())
                .add(ModBlocks.WILDSTONE_BRICKS.get())
                .add(ModBlocks.WILDSTONE_BRICK_STAIRS.get())
                .add(ModBlocks.WILDSTONE_BRICK_SLAB.get())
                .add(ModBlocks.WILDSTONE_BRICK_WALL.get())
                .add(ModBlocks.CHISELED_WILDSTONE_BRICKS.get())
                .add(ModBlocks.COBBLED_WILDSTONE.get())
                .add(ModBlocks.COBBLED_WILDSTONE_STAIRS.get())
                .add(ModBlocks.COBBLED_WILDSTONE_SLAB.get())
                .add(ModBlocks.COBBLED_WILDSTONE_WALL.get())

                // Amethyst
                .add(ModBlocks.AMETHYST_BRICKS.get())
                .add(ModBlocks.AMETHYST_BRICK_STAIRS.get())
                .add(ModBlocks.AMETHYST_BRICK_SLAB.get())
                .add(ModBlocks.AMETHYST_BRICK_WALL.get())
                .add(ModBlocks.CHISELED_AMETHYST_BLOCK.get())

                .add(ModBlocks.ICICLE.get())
                .add(ModBlocks.DUNESTONE_COAL_ORE.get())
                .add(ModBlocks.DUNESTONE_IRON_ORE.get())
                .add(ModBlocks.DUNESTONE_COPPER_ORE.get())
                .add(ModBlocks.DUNESTONE_GOLD_ORE.get())
                .add(ModBlocks.DUNESTONE_REDSTONE_ORE.get())
                .add(ModBlocks.DUNESTONE_EMERALD_ORE.get())
                .add(ModBlocks.DUNESTONE_LAPIS_ORE.get())
                .add(ModBlocks.DUNESTONE_DIAMOND_ORE.get())
                .add(ModBlocks.PERMAFROST_COAL_ORE.get())
                .add(ModBlocks.PERMAFROST_IRON_ORE.get())
                .add(ModBlocks.PERMAFROST_COPPER_ORE.get())
                .add(ModBlocks.PERMAFROST_GOLD_ORE.get())
                .add(ModBlocks.PERMAFROST_REDSTONE_ORE.get())
                .add(ModBlocks.PERMAFROST_EMERALD_ORE.get())
                .add(ModBlocks.PERMAFROST_LAPIS_ORE.get())
                .add(ModBlocks.PERMAFROST_DIAMOND_ORE.get())
                .add(ModBlocks.WILDSTONE_COAL_ORE.get())
                .add(ModBlocks.WILDSTONE_IRON_ORE.get())
                .add(ModBlocks.WILDSTONE_COPPER_ORE.get())
                .add(ModBlocks.WILDSTONE_GOLD_ORE.get())
                .add(ModBlocks.WILDSTONE_REDSTONE_ORE.get())
                .add(ModBlocks.WILDSTONE_EMERALD_ORE.get())
                .add(ModBlocks.WILDSTONE_LAPIS_ORE.get())
                .add(ModBlocks.WILDSTONE_DIAMOND_ORE.get())

                // Boulders
                .add(ModBlocks.BOULDER.get())
                .add(ModBlocks.IRON_BOULDER.get())
                .add(ModBlocks.COPPER_BOULDER.get())
                .add(ModBlocks.GOLD_BOULDER.get())
                .add(ModBlocks.LUSH_BOULDER.get())
                .add(ModBlocks.LUSH_IRON_BOULDER.get())
                .add(ModBlocks.LUSH_COPPER_BOULDER.get())
                .add(ModBlocks.LUSH_GOLD_BOULDER.get())

                // Geysers
                .add(ModBlocks.DEEPSLATE_GEYSER.get())
                .add(ModBlocks.BASALT_GEYSER.get())

                // Redstone
                .add(ModBlocks.BLASTER.get())
                .add(ModBlocks.WOODEN_LIFT_TRACK.get())
                .add(ModBlocks.POWERED_LIFT_TRACK.get())
        ;
        tag(BlockTags.MINEABLE_WITH_AXE)
                // Redstone
                .add(ModBlocks.WOODEN_LIFT_TRACK.get())
                .add(ModBlocks.POWERED_LIFT_TRACK.get())
        ;
        tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(ModBlocks.SILT.get())
        ;

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.DUNESTONE_IRON_ORE.get())
                .add(ModBlocks.DUNESTONE_COPPER_ORE.get())
                .add(ModBlocks.WILDSTONE_IRON_ORE.get())
                .add(ModBlocks.WILDSTONE_COPPER_ORE.get())
                .add(ModBlocks.PERMAFROST_IRON_ORE.get())
                .add(ModBlocks.PERMAFROST_COPPER_ORE.get())
                .add(ModBlocks.BLASTER.get())
        ;

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.DUNESTONE_GOLD_ORE.get())
                .add(ModBlocks.WILDSTONE_GOLD_ORE.get())
                .add(ModBlocks.PERMAFROST_GOLD_ORE.get())
                .add(ModBlocks.DUNESTONE_REDSTONE_ORE.get())
                .add(ModBlocks.WILDSTONE_REDSTONE_ORE.get())
                .add(ModBlocks.PERMAFROST_REDSTONE_ORE.get())
                .add(ModBlocks.DUNESTONE_EMERALD_ORE.get())
                .add(ModBlocks.WILDSTONE_EMERALD_ORE.get())
                .add(ModBlocks.PERMAFROST_EMERALD_ORE.get())
                .add(ModBlocks.DUNESTONE_LAPIS_ORE.get())
                .add(ModBlocks.WILDSTONE_LAPIS_ORE.get())
                .add(ModBlocks.PERMAFROST_LAPIS_ORE.get())
                .add(ModBlocks.DUNESTONE_DIAMOND_ORE.get())
                .add(ModBlocks.WILDSTONE_DIAMOND_ORE.get())
                .add(ModBlocks.PERMAFROST_DIAMOND_ORE.get())
                .add(ModBlocks.BOULDER.get())
                .add(ModBlocks.IRON_BOULDER.get())
                .add(ModBlocks.COPPER_BOULDER.get())
                .add(ModBlocks.GOLD_BOULDER.get())
                .add(ModBlocks.LUSH_BOULDER.get())
                .add(ModBlocks.LUSH_IRON_BOULDER.get())
                .add(ModBlocks.LUSH_COPPER_BOULDER.get())
                .add(ModBlocks.LUSH_GOLD_BOULDER.get())
        ;

        tag(BlockTags.SWORD_EFFICIENT)
                .add(ModBlocks.PACKED_WEB.get())
                .add(ModBlocks.WEB_VEIN.get())
                .add(ModBlocks.HANGING_WEB.get())
                .add(ModBlocks.SPIDER_EGG.get())
        ;

        // WORLDGEN
        tag(BlockTags.BASE_STONE_OVERWORLD)
                .add(ModBlocks.PERMAFROST.get())
                .add(ModBlocks.DUNESTONE.get())
                .add(ModBlocks.WILDSTONE.get())
        ;

        // This is so all ores still generate over the new stones.
        tag(BlockTags.STONE_ORE_REPLACEABLES)
                .add(ModBlocks.PERMAFROST.get())
                .add(ModBlocks.DUNESTONE.get())
                .add(ModBlocks.WILDSTONE.get())
        ;

        tag(BlockTags.DRIPSTONE_REPLACEABLE)
                .add(ModBlocks.DUNESTONE.get())
        ;

        tag(BlockTags.FEATURES_CANNOT_REPLACE)
        ;

        tag(BlockTags.COAL_ORES)
                .add(ModBlocks.DUNESTONE_COAL_ORE.get())
                .add(ModBlocks.PERMAFROST_COAL_ORE.get())
                .add(ModBlocks.WILDSTONE_COAL_ORE.get())
        ;

        tag(BlockTags.IRON_ORES)
                .add(ModBlocks.DUNESTONE_IRON_ORE.get())
                .add(ModBlocks.PERMAFROST_IRON_ORE.get())
                .add(ModBlocks.WILDSTONE_IRON_ORE.get())
        ;

        tag(BlockTags.COPPER_ORES)
                .add(ModBlocks.DUNESTONE_COPPER_ORE.get())
                .add(ModBlocks.PERMAFROST_COPPER_ORE.get())
                .add(ModBlocks.WILDSTONE_COPPER_ORE.get())
        ;

        tag(BlockTags.GOLD_ORES)
                .add(ModBlocks.DUNESTONE_GOLD_ORE.get())
                .add(ModBlocks.PERMAFROST_GOLD_ORE.get())
                .add(ModBlocks.WILDSTONE_GOLD_ORE.get())
        ;

        tag(BlockTags.REDSTONE_ORES)
                .add(ModBlocks.DUNESTONE_REDSTONE_ORE.get())
                .add(ModBlocks.PERMAFROST_REDSTONE_ORE.get())
                .add(ModBlocks.WILDSTONE_REDSTONE_ORE.get())
        ;

        tag(BlockTags.EMERALD_ORES)
                .add(ModBlocks.DUNESTONE_EMERALD_ORE.get())
                .add(ModBlocks.PERMAFROST_EMERALD_ORE.get())
                .add(ModBlocks.WILDSTONE_EMERALD_ORE.get())
        ;

        tag(BlockTags.LAPIS_ORES)
                .add(ModBlocks.DUNESTONE_LAPIS_ORE.get())
                .add(ModBlocks.PERMAFROST_LAPIS_ORE.get())
                .add(ModBlocks.WILDSTONE_LAPIS_ORE.get())
        ;

        tag(BlockTags.DIAMOND_ORES)
                .add(ModBlocks.DUNESTONE_DIAMOND_ORE.get())
                .add(ModBlocks.PERMAFROST_DIAMOND_ORE.get())
                .add(ModBlocks.WILDSTONE_DIAMOND_ORE.get())
        ;

// ENTITIES

        tag(BlockTags.GOATS_SPAWNABLE_ON)
                .add(ModBlocks.PERMAFROST.get())
        ;

// BLOCKS
        tag(BlockTags.WALLS)
                .add(ModBlocks.DUNESTONE_WALL.get())
                .add(ModBlocks.PERMAFROST_WALL.get())
                .add(ModBlocks.WILDSTONE_WALL.get())
                .add(ModBlocks.DUNESTONE_BRICK_WALL.get())
                .add(ModBlocks.PERMAFROST_BRICK_WALL.get())
                .add(ModBlocks.WILDSTONE_BRICK_WALL.get())
                .add(ModBlocks.COBBLED_DUNESTONE_WALL.get())
                .add(ModBlocks.COBBLED_PERMAFROST_WALL.get())
                .add(ModBlocks.COBBLED_WILDSTONE_WALL.get())
        ;
    }
}
