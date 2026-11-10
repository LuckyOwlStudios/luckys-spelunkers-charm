package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.List;

/** Ordinary motor rack: redstone reaches seven connected motor segments vertically. */
public class PoweredLiftTrackBlock extends LiftTrackBlock {
    public static final MapCodec<PoweredLiftTrackBlock> CODEC =
            simpleCodec(PoweredLiftTrackBlock::new);
    public static final BooleanProperty POWERED = LiftTrackBlock.POWERED;
    private static final int SIGNAL_RANGE = 7;

    public PoweredLiftTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_motor"));
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_motor_power"));
    }

    @Override
    protected MapCodec<PoweredLiftTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasSignal(Level level, BlockPos pos, BlockState state) {
        return level.hasNeighborSignal(pos)
                || findTrackWithSignal(level, pos, state, true)
                || findTrackWithSignal(level, pos, state, false);
    }

    protected boolean findTrackWithSignal(Level level, BlockPos pos, BlockState state, boolean up) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        Direction direction = up ? Direction.UP : Direction.DOWN;
        Direction facing = state.getValue(FACING);
        for (int distance = 1; distance <= SIGNAL_RANGE; distance++) {
            cursor.move(direction);
            if (!level.hasChunkAt(cursor)) return false;
            BlockState next = level.getBlockState(cursor);
            if (!next.is(this) || next.getValue(FACING) != facing) return false;
            // Only a real signal can extend power, never another cached POWERED state.
            if (level.hasNeighborSignal(cursor)) return true;
        }
        return false;
    }

    @Override
    protected void scheduleSignalUpdates(Level level, BlockPos pos) {
        for (int offset = -SIGNAL_RANGE; offset <= SIGNAL_RANGE; offset++) {
            BlockPos target = pos.offset(0, offset, 0);
            if (level.hasChunkAt(target) && level.getBlockState(target).is(this)) {
                level.scheduleTick(target, this, 1);
            }
        }
    }
}
