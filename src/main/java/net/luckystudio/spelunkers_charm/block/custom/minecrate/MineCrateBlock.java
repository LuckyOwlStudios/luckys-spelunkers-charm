// Mine crate block.
package net.luckystudio.spelunkers_charm.block.custom.minecrate;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MineCrateBlock extends BaseEntityBlock {
    public static final MapCodec<MineCrateBlock> CODEC = simpleCodec(MineCrateBlock::new);

    public MineCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MineCrateBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public MineCrateBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MineCrateBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND)
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return useCrate(level, pos, player, stack)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        return useCrate(level, pos, player, ItemStack.EMPTY)
                ? InteractionResult.sidedSuccess(level.isClientSide())
                : InteractionResult.PASS;
    }

    private boolean useCrate(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof MineCrateBlockEntity crate)) return false;
        var storage = crate.storage();
        if (level.isClientSide()) {
            return stack.isEmpty()
                    ? !storage.displayItem().isEmpty()
                    : storage.insertItem(0, stack, true).getCount() < stack.getCount();
        }
        return storage.use(player, stack, pos);
    }
}
