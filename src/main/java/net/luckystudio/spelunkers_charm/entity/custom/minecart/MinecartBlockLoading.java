// Minecart block loading.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public final class MinecartBlockLoading {
    private MinecartBlockLoading() {}

    public static boolean load(AbstractMinecart cart, ItemStack stack, @Nullable Player player) {
        if (cart.level().isClientSide() || cart.isRemoved() || stack.isEmpty()) return false;
        if (cart instanceof MineCrateMinecart crate) {
            ItemStack remainder = crate.storage().insertItem(0, stack, false);
            int inserted = stack.getCount() - remainder.getCount();
            stack.shrink(inserted);
            return inserted > 0;
        }
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        if (cart instanceof BlockMinecart cargo) {
            int loaded = cargo.addBlocks(state, stack.getCount());
            if (loaded == 0) return false;
            stack.shrink(loaded);
            if (player != null) playLoadSound(cargo, state);
            return true;
        }
        if (!(cart instanceof Minecart empty)) return false;

        AbstractMinecart replacement = createMinecart(empty, block, stack);
        if (replacement instanceof BlockMinecart cargo) cargo.addBlocks(state, stack.getCount());
        if (!MinecartCouplings.replaceEntity(empty, replacement)) return false;
        if (replacement instanceof BlockMinecart) stack.shrink(stack.getCount());
        else if (player != null) stack.consume(1, player);
        else stack.shrink(1);
        if (player != null) playLoadSound(replacement, state);
        return true;
    }

    private static void playLoadSound(AbstractMinecart cart, BlockState state) {
        cart.playSound(
                state.getSoundType(cart.level(), cart.blockPosition(), cart).getPlaceSound(),
                1.0F,
                1.0F);
    }

    private static AbstractMinecart createMinecart(Minecart cart, Block block, ItemStack stack) {
        if (block == ModBlocks.MINE_CRATE.get()) return new MineCrateMinecart(cart, stack);
        if (block == ModBlocks.TANK.get()) return new TankMinecart(cart, stack);
        Level level = cart.level();
        double x = cart.getX(), y = cart.getY(), z = cart.getZ();
        if (block == Blocks.HOPPER) return new MinecartHopper(level, x, y, z);
        if (block == Blocks.CHEST) return new MinecartChest(level, x, y, z);
        if (block == Blocks.FURNACE) return new MinecartFurnace(level, x, y, z);
        if (block == Blocks.TNT) return new MinecartTNT(level, x, y, z);
        if (block == Blocks.BLAST_FURNACE) return new LocomotiveMinecart(level, x, y, z);
        if (block == Blocks.JUKEBOX) return new JukeboxMinecart(level, x, y, z);
        return new BlockMinecart(level, x, y, z);
    }
}
