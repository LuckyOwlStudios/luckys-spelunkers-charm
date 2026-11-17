package net.luckystudio.spelunkers_charm.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** What clicking and breaking any block of a Sable lift deck does. Shared by the controller and parts. */
final class LiftDeckInteractions {
    private LiftDeckInteractions() {}

    /** Planks and minerals restyle the lift. Anything else falls through (so blocks can be placed). */
    static ItemInteractionResult useItem(ItemStack stack, Level level, BlockPos pos) {
        LiftControllerBlockEntity controller = LiftControllerBlockEntity.find(level, pos);
        if (controller == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        return controller.restyle(stack)
                ? ItemInteractionResult.CONSUME
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Empty hand: start or stop. Sneaking: reverse. */
    static InteractionResult use(Level level, BlockPos pos, Player player) {
        // Holding something (a block to build with, say)? Let it be used normally.
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        LiftControllerBlockEntity controller = LiftControllerBlockEntity.find(level, pos);
        if (controller == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        controller.use(player);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1);
        return InteractionResult.CONSUME;
    }

    /** Breaking any one block breaks the whole lift. */
    static void broken(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        LiftControllerBlockEntity controller = LiftControllerBlockEntity.find(level, pos);
        if (controller != null) controller.dismantle(player);
    }
}
