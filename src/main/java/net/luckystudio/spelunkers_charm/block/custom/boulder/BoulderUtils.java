package net.luckystudio.spelunkers_charm.block.custom.boulder;

import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.block.util.enums.BlockPart;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class BoulderUtils {

    public static BlockState getBoulderFromType(BoulderType type) {
        return ModBlocks.BOULDERS.get(type).get().defaultBlockState();
    }

    /** The core block of the boulder. Falls back to cobblestone if the ore's mod is missing. */
    public static Block getRawCore(BoulderType type) {
        Block core = type.getOre().getCore();
        return core != null ? core : Blocks.COBBLESTONE;
    }

    /** Ores whose boulders can generate right now: the ore exists and its config toggle is on. */
    public static List<BoulderOre> getGeneratableOres() {
        List<BoulderOre> ores = new ArrayList<>();
        for (BoulderOre ore : BoulderOre.values()) {
            if (ore.isModded()) {
                ModConfigSpec.BooleanValue enabled = SpelunkersCharmConfig.MODDED_BOULDERS.get(ore);
                if (enabled != null && !enabled.get()) continue;
            }
            if (ore.isAvailable()) ores.add(ore);
        }
        return ores;
    }

    /** Picks a random ore for a naturally generated boulder, weighted by {@link BoulderOre#getWeight}. */
    public static BoulderOre pickOre(RandomSource random, boolean belowZero) {
        List<BoulderOre> ores = getGeneratableOres();
        float total = 0;
        for (BoulderOre ore : ores) total += ore.getWeight(belowZero);
        float roll = random.nextFloat() * total;
        for (BoulderOre ore : ores) {
            roll -= ore.getWeight(belowZero);
            if (roll < 0) return ore;
        }
        return BoulderOre.NONE;
    }

    /**
     * Places a full 3x3x3 boulder with the given bottom-middle position.
     * The middle of the boulder is the ore's raw block.
     */
    public static void placeBoulder(LevelAccessor level, BlockPos origin, BoulderType type, HangingType hangingType, int flags) {
        BlockState boulderState = getBoulderFromType(type).setValue(BoulderBlock.HANGING_TYPE, hangingType);
        Direction direction = Direction.NORTH;
        BlockPart[][] layers = {
                {BlockPart.BOTTOM_MIDDLE, BlockPart.BOTTOM_SIDE, BlockPart.BOTTOM_CORNER},
                {null, BlockPart.MIDDLE_SIDE, BlockPart.MIDDLE_CORNER},
                {BlockPart.TOP_MIDDLE, BlockPart.TOP_SIDE, BlockPart.TOP_CORNER}
        };
        for (int y = 0; y < 3; y++) {
            BlockPos center = origin.above(y);
            BlockPart middle = layers[y][0];
            if (middle == null) {
                level.setBlock(center, getRawCore(type).defaultBlockState(), flags);
            } else {
                level.setBlock(center, boulderState
                        .setValue(BoulderBlock.FACING, direction)
                        .setValue(BoulderBlock.BLOCK_PART, middle), flags);
            }
            // Walk around the center: each side faces outward, and each corner shares the facing of the side before it.
            Direction facing = direction;
            for (int i = 0; i < 4; i++) {
                level.setBlock(center.relative(facing), boulderState
                        .setValue(BoulderBlock.FACING, facing)
                        .setValue(BoulderBlock.BLOCK_PART, layers[y][1]), flags);
                level.setBlock(center.relative(facing).relative(facing.getClockWise()), boulderState
                        .setValue(BoulderBlock.FACING, facing)
                        .setValue(BoulderBlock.BLOCK_PART, layers[y][2]), flags);
                facing = facing.getClockWise();
            }
        }
    }
}
