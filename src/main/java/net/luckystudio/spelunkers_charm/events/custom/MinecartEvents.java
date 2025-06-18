package net.luckystudio.spelunkers_charm.events.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.BlockMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MinecartEvents {

    @SubscribeEvent
    public static InteractionResult onMinecartInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide()) return InteractionResult.PASS;  // Early return for client side

        Entity target = event.getTarget();
        InteractionHand hand = event.getHand();
        if (!hand.equals(InteractionHand.MAIN_HAND)) return InteractionResult.PASS;
        ItemStack heldItem = player.getItemInHand(hand);
        if (!(target instanceof AbstractMinecart abstractMinecart)) return InteractionResult.PASS;
        if (abstractMinecart instanceof MinecartTNT minecartTNT) {
            minecartTNT.primeFuse();
            player.swing(hand, true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!player.isCrouching()) return InteractionResult.PASS;

        // Handle Minecart Interaction
        if (abstractMinecart instanceof Minecart minecart && heldItem.getItem() instanceof BlockItem blockItem) {
            if (minecartBlocks(blockItem.getBlock())) {
                Entity newMinecart = getAbstractMinecart(minecart, blockItem.getBlock(), level);
                // Copy motion and rotation from the original minecart
                newMinecart.setPosRaw(minecart.getX(), minecart.getY(), minecart.getZ());
                newMinecart.setDeltaMovement(minecart.getDeltaMovement());
                newMinecart.setYRot(minecart.getYRot());
                newMinecart.setXRot(minecart.getXRot());

                SoundEvent placeBlockSound = blockItem.getBlock().defaultBlockState().getSoundType(level, BlockPos.containing(minecart.getX(), minecart.getY(), minecart.getZ()), minecart).getPlaceSound();
                minecart.playSound(placeBlockSound, 1.0F, 1.0F);

                // Remove the old minecart and add the new one
                minecart.remove(Entity.RemovalReason.DISCARDED);
                level.addFreshEntity(newMinecart);
                player.swing(hand, true);
                return InteractionResult.SUCCESS;
            } else {
                BlockMinecart newMinecart = new BlockMinecart(level, minecart.getX(), minecart.getY(), minecart.getZ());
                // Copy motion and rotation from the original minecart
                newMinecart.setPosRaw(minecart.getX(), minecart.getY(), minecart.getZ());
                newMinecart.setDeltaMovement(minecart.getDeltaMovement());
                newMinecart.setYRot(minecart.getYRot());
                newMinecart.setXRot(minecart.getXRot());

                newMinecart.setDisplayBlockState(blockItem.getBlock().defaultBlockState());
                newMinecart.setBlockAmount(heldItem.getCount());
                newMinecart.updateName();
                heldItem.shrink(heldItem.getCount());

                SoundEvent placeBlockSound = blockItem.getBlock().defaultBlockState().getSoundType(level, BlockPos.containing(minecart.getX(), minecart.getY(), minecart.getZ()), minecart).getPlaceSound();
                minecart.playSound(placeBlockSound, 1.0F, 1.0F);

                // Remove the old minecart and add the new one
                minecart.remove(Entity.RemovalReason.DISCARDED);
                level.addFreshEntity(newMinecart);
                player.swing(hand, true);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean minecartBlocks(Block block) {
        return block == Blocks.HOPPER || block == Blocks.CHEST || block == Blocks.FURNACE || block == Blocks.TNT;
    }

    private static Entity getAbstractMinecart(Minecart oldMinecart, Block block, Level level) {
        if (block == Blocks.HOPPER) {
            return new MinecartHopper(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.CHEST) {
            return new MinecartChest(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.FURNACE) {
            return new MinecartFurnace(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        } else if (block == Blocks.TNT) {
            return new MinecartTNT(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
        }
        return new Minecart(level, oldMinecart.getX(), oldMinecart.getY(), oldMinecart.getZ());
    }
}
