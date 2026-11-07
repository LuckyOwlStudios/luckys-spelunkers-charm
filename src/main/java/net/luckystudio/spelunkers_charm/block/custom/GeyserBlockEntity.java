package net.luckystudio.spelunkers_charm.block.custom;

import net.luckystudio.spelunkers_charm.block.custom.blaster.BlasterUtil;
import net.luckystudio.spelunkers_charm.block.util.enums.GeyserState;
import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Times a geyser's charge and eruption. It only ticks while the geyser is charging or erupting
 * (see {@link GeyserBlock#getTicker}) and stores nothing, so dormant geysers cost nothing.
 * If a chunk unloads mid-cycle, the current phase simply restarts when it loads again.
 */
public class GeyserBlockEntity extends BlockEntity {
    @Nullable
    private GeyserState trackedState;
    private int phaseTicks;

    public GeyserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityType.GEYSER_BLOCK_ENTITY.get(), pos, state);
    }

    /** Counts ticks spent in the current phase, starting again from 1 whenever the phase changes. */
    private int advance(GeyserState state) {
        if (state != trackedState) {
            trackedState = state;
            phaseTicks = 0;
        }
        return ++phaseTicks;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeyserBlockEntity geyser) {
        if (!(state.getBlock() instanceof GeyserBlock block)) return;
        GeyserState geyserState = state.getValue(GeyserBlock.GEYSER_STATE);
        int ticks = geyser.advance(geyserState);
        switch (geyserState) {
            case CHARGING -> {
                if (ticks >= GeyserBlock.CHARGE_TICKS) {
                    block.erupt(level, pos, state);
                }
            }
            case ERUPTING -> {
                if (ticks > GeyserBlock.ERUPTION_TICKS) {
                    block.stopErupting(level, pos, state);
                } else {
                    BlasterUtil.shoot(level, pos, state.getValue(GeyserBlock.GEYSER_TYPE), Direction.UP);
                }
            }
            default -> {}
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GeyserBlockEntity geyser) {
        // Particles, plus pushing the local player so the jet feels responsive
        BlasterUtil.shoot(level, pos, state.getValue(GeyserBlock.GEYSER_TYPE), Direction.UP);
    }
}
