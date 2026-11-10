package net.luckystudio.spelunkers_charm.events.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.block.custom.rail.CrossingRailBlock;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class RailEvents {

    /**
     * Right-clicking a flat, straight rail with another rail while facing across it
     * turns it into a rail crossing.
     */
    @SubscribeEvent
    public static void onRailUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.RAIL)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.RAIL)) return;

        RailShape shape = state.getValue(RailBlock.SHAPE);
        Direction.Axis railAxis;
        if (shape == RailShape.NORTH_SOUTH) {
            railAxis = Direction.Axis.Z;
        } else if (shape == RailShape.EAST_WEST) {
            railAxis = Direction.Axis.X;
        } else {
            return; // Slopes and curves can't be crossed.
        }

        Player player = event.getEntity();
        if (player.getDirection().getAxis() == railAxis) return; // The new rail would face the same way.

        if (!level.isClientSide()) {
            BlockState crossing = ModBlocks.CROSSING_RAIL.get().defaultBlockState()
                    .setValue(CrossingRailBlock.SHAPE, shape)
                    .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
            level.setBlock(pos, crossing, Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            stack.consume(1, player);
        }
        player.swing(event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
