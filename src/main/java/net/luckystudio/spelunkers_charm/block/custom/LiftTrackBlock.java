package net.luckystudio.spelunkers_charm.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Shared rack orientation and observer-readable platform presence. */
public abstract class LiftTrackBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty LIFT_PRESENT = BooleanProperty.create("lift_present");

    protected LiftTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(POWERED, false)
                        .setValue(LIFT_PRESENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, LIFT_PRESENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction facing =
                context.getClickedFace().getAxis().isHorizontal()
                        ? context.getClickedFace()
                        : context.getHorizontalDirection().getOpposite();
        for (Direction vertical : new Direction[] {Direction.DOWN, Direction.UP}) {
            BlockState adjacent = context.getLevel().getBlockState(pos.relative(vertical));
            if (adjacent.getBlock() instanceof LiftTrackBlock) {
                facing = adjacent.getValue(FACING);
                break;
            }
        }
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.setValue(POWERED, hasSignal(context.getLevel(), pos, state));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide) {
            updatePower(state, level, pos);
            scheduleSignalUpdates(level, pos);
        }
    }

    @Override
    public void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!level.isClientSide && !next.is(this)) scheduleSignalUpdates(level, pos);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos other,
            boolean moving) {
        if (!level.isClientSide) {
            updatePower(state, level, pos);
            scheduleSignalUpdates(level, pos);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updatePower(state, level, pos);
    }

    protected boolean hasSignal(Level level, BlockPos pos, BlockState state) {
        return false;
    }

    protected void scheduleSignalUpdates(Level level, BlockPos pos) {}

    protected void updatePower(BlockState state, Level level, BlockPos pos) {
        boolean powered = hasSignal(level, pos, state);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }
}
