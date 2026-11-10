package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** A passive rack with a detector-rail output, not another motor or station. */
public class DetectorLiftTrackBlock extends BasicLiftTrackBlock {
    public static final MapCodec<DetectorLiftTrackBlock> CODEC =
            simpleCodec(DetectorLiftTrackBlock::new);

    public DetectorLiftTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DetectorLiftTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasSignal(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return state.getValue(POWERED);
        for (AbstractLift lift :
                level.getEntitiesOfClass(
                        AbstractLift.class, new AABB(pos).inflate(8.0, 1.0, 8.0))) {
            if (lift.isRemoved()
                    || lift.getY() >= pos.getY() + 1.0 - 1.0E-6
                    || lift.getBoundingBox().maxY <= pos.getY() + 1.0E-6) continue;
            for (BlockPos guide : lift.getGuidePositions()) {
                if (guide.getX() != pos.getX() || guide.getZ() != pos.getZ()) continue;
                double dx = lift.getX() - pos.getX() - 0.5, dz = lift.getZ() - pos.getZ() - 0.5;
                Direction inward =
                        Math.abs(dx) > Math.abs(dz)
                                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
                if (state.getValue(FACING) == inward) return true;
            }
        }
        return false;
    }

    public void refresh(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        boolean occupied = hasSignal(level, pos, state);
        if (occupied != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, occupied), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, this);
            level.updateNeighborsAt(pos.below(), this);
        }
        if (occupied) level.scheduleTick(pos, this, 2);
    }

    @Override
    public void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        refresh(level, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        refresh(level, pos);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos other,
            boolean moving) {
        refresh(level, pos);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(
            BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(POWERED) && side == Direction.UP ? 15 : 0;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_detector"));
    }
}
