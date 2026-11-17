package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Center block of a Sable lift deck, and the one that draws the lift model. Right-click any part of
 * the deck to start or stop the lift, sneak-right-click to reverse it, and use planks or a mineral on
 * it to restyle it. Breaking any part breaks the whole lift and gives the lift item back.
 */
public class LiftControllerBlock extends BaseEntityBlock {
    public static final MapCodec<LiftControllerBlock> CODEC = simpleCodec(LiftControllerBlock::new);

    public LiftControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LiftControllerBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // Drawn by LiftControllerRenderer with the lift's own model.
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(
                        type, ModBlockEntityType.LIFT_CONTROLLER.get(), LiftControllerClient::tick)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return LiftDeckInteractions.useItem(stack, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return LiftDeckInteractions.use(level, pos, player);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        LiftDeckInteractions.broken(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LiftControllerBlockEntity controller)
            controller.dismantle(null);
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof LiftControllerBlockEntity controller)
            controller.onBroken();
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(); // dismantle() hands back the lift item.
    }
}
