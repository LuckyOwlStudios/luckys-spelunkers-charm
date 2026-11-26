// Cart loader block.
package net.luckystudio.spelunkers_charm.block.custom.cartloader;

import com.mojang.serialization.MapCodec;

import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

public final class CartLoaderBlock extends BaseEntityBlock {
    public static final MapCodec<CartLoaderBlock> CODEC = simpleCodec(CartLoaderBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public CartLoaderBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(POWERED, false).setValue(TRIGGERED, false));
    }

    @Override
    protected MapCodec<CartLoaderBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, TRIGGERED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public CartLoaderBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CartLoaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !state.getValue(POWERED)
                ? null
                : createTickerHelper(
                        type,
                        ModBlockEntityType.CART_LOADER.get(),
                        CartLoaderBlockEntity::tickPulse);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CartLoaderBlockEntity loader
                ? loader.comparatorSignal()
                : 0;
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return true;
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!oldState.is(this)) updatePower(state, level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean moved) {
        updatePower(state, level, pos);
    }

    private void updatePower(BlockState state, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        boolean signal = server.hasNeighborSignal(pos);
        if (state.getValue(TRIGGERED) == signal) return;
        BlockState next = state.setValue(TRIGGERED, signal);
        if (signal) next = next.setValue(POWERED, true);
        server.setBlock(pos, next, Block.UPDATE_CLIENTS);
        if (signal && server.getBlockEntity(pos) instanceof CartLoaderBlockEntity loader) {
            loader.startPulse(server.getGameTime());
            server.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)
                && level.getBlockEntity(pos) instanceof CartLoaderBlockEntity loader) {
            loader.activate(level);
        }
    }

    static void endPulse(Level level, BlockPos pos, BlockState state, boolean legacyPower) {
        if (legacyPower) state = state.setValue(TRIGGERED, level.hasNeighborSignal(pos));
        level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_CLIENTS);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CartLoaderBlockEntity loader))
            return InteractionResult.PASS;
        if (!level.isClientSide()) player.openMenu(loader);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide()
                && !newState.is(this)
                && level.getBlockEntity(pos) instanceof CartLoaderBlockEntity loader)
            loader.dropContents();
        super.onRemove(state, level, pos, newState, moved);
    }
}
