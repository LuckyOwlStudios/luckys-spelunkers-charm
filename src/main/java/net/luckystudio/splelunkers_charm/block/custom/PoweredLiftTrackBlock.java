package net.luckystudio.splelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PoweredLiftTrackBlock extends WoodenLiftTrackBlock {
    public static final MapCodec<PoweredLiftTrackBlock> CODEC = simpleCodec(PoweredLiftTrackBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private int distance = 7;

    public PoweredLiftTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);
        boolean isPowered = context.getLevel().hasNeighborSignal(context.getClickedPos())
                || findTrackWithSignal(level, pos, state, true)
                || findTrackWithSignal(level, pos, state, false);
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, isPowered || context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor levelAccessor, BlockPos pos, BlockPos neighborPos) {
        if (!(levelAccessor instanceof Level level)) return state;

        boolean isPowered = level.hasNeighborSignal(pos)
                || findTrackWithSignal(level, pos, state, true)
                || findTrackWithSignal(level, pos, state, false);

        return state.setValue(POWERED, isPowered);
    }

    protected boolean findTrackWithSignal(Level level, BlockPos pos, BlockState state, boolean searchUp) {
        Direction dir = searchUp ? Direction.UP : Direction.DOWN;
        BlockPos.MutableBlockPos currentPos = pos.mutable();

        for (int i = 1; i <= distance; ++i) {
            currentPos.move(dir);
            BlockState currentState = level.getBlockState(currentPos);

            // Stop if block is not the same track
            if (!currentState.is(this)) {
                return false;
            }

            if (!level.getBlockState(currentPos).is(this)) {
                return false;
            }

            // If that track is powered directly or indirectly
            if (level.getBestNeighborSignal(currentPos) > 0) {
                return true;
            }
        }
        return false;
    }
}
