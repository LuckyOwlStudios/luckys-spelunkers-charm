// Cart loader block entity.
package net.luckystudio.spelunkers_charm.block.custom.cartloader;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartBlockLoading;
import net.luckystudio.spelunkers_charm.init.ModBlockEntityType;
import net.luckystudio.spelunkers_charm.init.ModParticleTypes;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Comparator;

public final class CartLoaderBlockEntity extends BlockEntity implements MenuProvider {
    private static final int PULSE_TICKS = 10;
    private long pulseEndsAt;
    private final ItemStackHandler inventory =
            new ItemStackHandler(1) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }
            };

    public CartLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityType.CART_LOADER.get(), pos, state);
    }

    public IItemHandler itemHandler() {
        return inventory;
    }

    int comparatorSignal() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) return 0;
        int capacity = Math.min(inventory.getSlotLimit(0), stack.getMaxStackSize());
        return 1 + 14 * Math.min(stack.getCount(), capacity) / capacity;
    }

    void startPulse(long gameTime) {
        pulseEndsAt = gameTime + PULSE_TICKS;
        setChanged();
    }

    static void tickPulse(
            Level level, BlockPos pos, BlockState state, CartLoaderBlockEntity loader) {
        if (level.getGameTime() >= loader.pulseEndsAt) {
            CartLoaderBlock.endPulse(level, pos, state, loader.pulseEndsAt == 0);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public CartLoaderMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CartLoaderMenu(
                id,
                playerInventory,
                inventory,
                ContainerLevelAccess.create(player.level(), worldPosition),
                this::setChanged);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pulseEndsAt = tag.getLong("PulseEndsAt");
        inventory.setStackInSlot(0, ItemStack.parseOptional(registries, tag.getCompound("Item")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (getBlockState().getValue(CartLoaderBlock.POWERED) && pulseEndsAt != 0) {
            tag.putLong("PulseEndsAt", pulseEndsAt);
        }
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) tag.put("Item", stack.save(registries));
    }

    void activate(ServerLevel server) {
        spawnTrail(server);
        ItemStack remaining = inventory.getStackInSlot(0).copy();
        if (remaining.isEmpty()) return;
        BlockPos chute = worldPosition.below();
        boolean loaded = loadAt(server, chute, remaining);
        if (!loaded && server.getBlockState(chute).getCollisionShape(server, chute).isEmpty()) {
            loaded = loadAt(server, chute.below(), remaining);
        }
        if (loaded) {
            inventory.setStackInSlot(0, remaining);
            server.playSound(
                    null,
                    worldPosition,
                    ModSoundEvents.LOADED_CART.get(),
                    SoundSource.BLOCKS,
                    1,
                    1);
        }
    }

    private void spawnTrail(ServerLevel server) {
        Vec3 center = Vec3.atBottomCenterOf(worldPosition);
        for (int i = 0; i < 5; i++) {
            double x = center.x + (server.random.nextDouble() - 0.5) * 0.45;
            double z = center.z + (server.random.nextDouble() - 0.5) * 0.45;
            double y = center.y - 0.12 - server.random.nextDouble() * 0.25;
            server.sendParticles(
                    ModParticleTypes.CART_LOAD_TRAIL.get(),
                    x,
                    y,
                    z,
                    0,
                    0,
                    -0.28 - server.random.nextDouble() * 0.14,
                    0,
                    1);
        }
    }

    private boolean loadAt(ServerLevel server, BlockPos chute, ItemStack remaining) {
        Vec3 center = Vec3.atCenterOf(chute);
        // Match a detector rail's footprint so the front of a moving cart can trigger loading.
        AABB opening = new AABB(chute).deflate(0.2, 0, 0.2);
        var carts = server.getEntitiesOfClass(AbstractMinecart.class, opening);
        carts.sort(
                Comparator.comparingDouble((AbstractMinecart cart) -> cart.distanceToSqr(center))
                        .thenComparingInt(AbstractMinecart::getId));
        for (AbstractMinecart cart : carts) {
            if (MinecartBlockLoading.load(cart, remaining, null)) return true;
        }
        return false;
    }

    void dropContents() {
        ItemStack stack = inventory.extractItem(0, 64, false);
        if (!stack.isEmpty() && level != null) {
            Containers.dropItemStack(
                    level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
    }
}
