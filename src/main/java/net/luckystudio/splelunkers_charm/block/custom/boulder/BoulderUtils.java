package net.luckystudio.splelunkers_charm.block.custom.boulder;

import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BoulderUtils {

    public static BlockState getBoulderFromType(BoulderType type) {
        return switch (type) {
            case STONE -> ModBlocks.BOULDER.get().defaultBlockState();
            case IRON -> ModBlocks.IRON_BOULDER.get().defaultBlockState();
            case COPPER -> ModBlocks.COPPER_BOULDER.get().defaultBlockState();
            case GOLD -> ModBlocks.GOLD_BOULDER.get().defaultBlockState();
            case LUSH -> ModBlocks.LUSH_BOULDER.get().defaultBlockState();
            case LUSH_IRON -> ModBlocks.LUSH_IRON_BOULDER.get().defaultBlockState();
            case LUSH_COPPER -> ModBlocks.LUSH_COPPER_BOULDER.get().defaultBlockState();
            case LUSH_GOLD -> ModBlocks.LUSH_GOLD_BOULDER.get().defaultBlockState();
        };
    }

    public static Block getRawCore(BoulderType type) {
        return switch (type) {
            case STONE, LUSH -> Blocks.COBBLESTONE;
            case IRON, LUSH_IRON -> Blocks.RAW_IRON_BLOCK;
            case COPPER, LUSH_COPPER -> Blocks.RAW_COPPER_BLOCK;
            case GOLD, LUSH_GOLD -> Blocks.RAW_GOLD_BLOCK;
        };
    }
}
