package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Center block of a Sable lift deck. Right-click to start or stop the lift, sneak-right-click to
 * reverse it. Breaking it gives the lift item back and leaves the rest of the deck loose.
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
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof LiftControllerBlockEntity controller) {
            controller.use(player);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1);
        }
        return InteractionResult.CONSUME;
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
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof LiftControllerBlockEntity controller) {
            AbstractLift.Type type = controller.getLiftType();
            if (type != null)
                return List.of(
                        new ItemStack(
                                switch (type) {
                                    case SMALL -> ModItems.SMALL_LIFT.get();
                                    case MEDIUM -> ModItems.MEDIUM_LIFT.get();
                                    case LARGE -> ModItems.LARGE_LIFT.get();
                                }));
        }
        return List.of();
    }
}
