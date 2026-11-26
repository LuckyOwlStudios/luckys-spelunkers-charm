// Tank tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.tank.TankBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.tank.TankStorage;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.TankMinecart;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModDataComponents;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TankTests {
    private static final BlockPos TANK = new BlockPos(8, 2, 8);

    @GameTest(template = "empty")
    public static void tankTakesEightBucketsAndDropsEmpty(GameTestHelper h) {
        h.setBlock(TANK.below(), Blocks.STONE);
        h.setBlock(TANK, ModBlocks.TANK.get());
        var tank = (TankBlockEntity) h.getBlockEntity(TANK);
        var pos = h.absolutePos(TANK);
        var handler =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
        h.assertTrue(
                handler != null && handler.getTankCapacity(0) == 8000,
                "Tank should expose a capacity of eight buckets");
        var player = playerNear(h, pos);
        for (int i = 0; i < 8; i++) {
            h.assertTrue(
                    useBucket(player, pos, Items.WATER_BUCKET).consumesAction()
                            && player.getMainHandItem().is(Items.BUCKET),
                    "Tank should accept a water bucket and return the empty bucket");
        }
        h.assertTrue(tank.storage().getFluidAmount() == 8000, "Tank did not reach eight buckets");
        h.assertFalse(
                useBucket(player, pos, Items.WATER_BUCKET).consumesAction(),
                "Tank accepted a ninth bucket");
        h.assertTrue(
                useBucket(player, pos, Items.BUCKET).consumesAction()
                        && player.getMainHandItem().is(Items.WATER_BUCKET),
                "Empty bucket did not draw water from the tank");
        h.assertFalse(
                useBucket(player, pos, Items.LAVA_BUCKET).consumesAction(),
                "Tank accepted a different fluid");
        h.assertTrue(
                tank.storage().getFluidAmount() == 7000
                        && player.getMainHandItem().is(Items.LAVA_BUCKET),
                "Rejected lava changed the tank or bucket");
        h.getLevel().destroyBlock(pos, true);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        ItemStack packed =
                drops.stream()
                        .map(ItemEntity::getItem)
                        .filter(item -> item.is(ModBlocks.TANK.asItem()))
                        .findFirst()
                        .orElse(ItemStack.EMPTY)
                        .copy();
        h.assertTrue(
                packed.is(ModBlocks.TANK.asItem())
                        && !packed.has(ModDataComponents.TANK_CONTENTS.get()),
                "Breaking a filled tank should drop a plain tank item");
        player.setItemInHand(InteractionHand.MAIN_HAND, packed);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        h.assertTrue(
                player.gameMode
                        .useItemOn(player, h.getLevel(), packed, InteractionHand.MAIN_HAND, hit)
                        .consumesAction(),
                "Could not place the dropped tank");
        var restored = (TankBlockEntity) h.getBlockEntity(TANK);
        h.assertTrue(
                restored.storage().getFluid().isEmpty(),
                "The dropped tank should be empty after placement");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void destroyedTankMinecartDropsPlainTankAndCart(GameTestHelper h) {
        var position = h.absolutePos(TANK);
        var cart = ModEntityType.TANK_MINECART.get().create(h.getLevel());
        cart.setPos(Vec3.atBottomCenterOf(position));
        cart.storage().fill(new FluidStack(Fluids.LAVA, 8000), FluidAction.EXECUTE);
        h.getLevel().addFreshEntity(cart);
        var player = playerNear(h, position);
        h.assertTrue(
                cart.hurt(h.getLevel().damageSources().playerAttack(player), 100)
                        && cart.isRemoved(),
                "The tank cart should be destroyed by a survival attack");
        var drops =
                h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(1));
        var items = drops.stream().map(ItemEntity::getItem).toList();
        h.assertTrue(
                items.stream().anyMatch(item -> item.is(Items.MINECART)),
                "The minecart item did not drop");
        var tanks = items.stream().filter(item -> item.is(ModBlocks.TANK.asItem())).toList();
        h.assertTrue(
                tanks.size() == 1 && !tanks.getFirst().has(ModDataComponents.TANK_CONTENTS.get()),
                "Breaking the cart should drop one empty tank");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cartLoaderInstallsTankButLeavesBucketsUntouched(GameTestHelper h) {
        Minecart empty = spawnEmptyCart(h);
        BlockPos loaderPos = TANK.above(2);
        h.setBlock(loaderPos, ModBlocks.CART_LOADER.get());
        var loader = (CartLoaderBlockEntity) h.getBlockEntity(loaderPos);
        var slot = loader.itemHandler();
        slot.insertItem(0, tankItem(h, new FluidStack(Fluids.WATER, 3000)), false);
        BlockPos power = loaderPos.east();
        h.setBlock(power, Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(
                3,
                () -> {
                    TankMinecart cart = tankCart(h);
                    h.assertTrue(
                            empty.isRemoved()
                                    && cart.storage().getFluidAmount() == 3000
                                    && slot.getStackInSlot(0).isEmpty(),
                            "Loader did not install the packed tank");
                    var capability =
                            cart.getCapability(Capabilities.FluidHandler.ENTITY, Direction.UP);
                    h.assertTrue(
                            capability != null
                                    && capability.getFluidInTank(0).getFluid() == Fluids.WATER,
                            "Tank cart did not expose its fluid");
                    slot.insertItem(0, new ItemStack(Items.WATER_BUCKET), false);
                    h.setBlock(power, Blocks.AIR);
                    h.setBlock(power, Blocks.REDSTONE_BLOCK);
                    h.runAfterDelay(
                            3,
                            () -> {
                                h.assertTrue(
                                        slot.getStackInSlot(0).is(Items.WATER_BUCKET)
                                                && cart.storage().getFluidAmount() == 3000,
                                        "Cart Loader transferred fluid from a bucket");
                                var player = playerNear(h, h.absolutePos(TANK));
                                player.setItemInHand(
                                        InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                                h.assertTrue(
                                        player.interactOn(cart, InteractionHand.MAIN_HAND)
                                                        .consumesAction()
                                                && player.getMainHandItem().is(Items.WATER_BUCKET)
                                                && cart.storage().getFluidAmount() == 2000,
                                        "Player could not drain a bucket from the cart tank");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "empty")
    public static void filledTankCanBeMountedSavedAndDetached(GameTestHelper h) {
        Minecart empty = spawnEmptyCart(h);
        var player = playerNear(h, h.absolutePos(TANK));
        player.setPose(Pose.CROUCHING);
        player.setShiftKeyDown(true);
        ItemStack tank = tankItem(h, new FluidStack(Fluids.LAVA, 4500));
        player.setItemInHand(InteractionHand.MAIN_HAND, tank);
        h.assertTrue(
                player.interactOn(empty, InteractionHand.MAIN_HAND).consumesAction()
                        && tank.isEmpty(),
                "Sneak-right-click did not mount the tank");
        TankMinecart cart = tankCart(h);
        var restored = ModEntityType.TANK_MINECART.get().create(h.getLevel());
        restored.load(cart.saveWithoutId(new CompoundTag()));
        h.assertTrue(
                restored.storage().getFluid().getFluid() == Fluids.LAVA
                        && restored.storage().getFluidAmount() == 4500,
                "Tank cart lost its liquid when saved and restored");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(
                player.interactOn(cart, InteractionHand.MAIN_HAND).consumesAction()
                        && cart.isRemoved(),
                "Sneak-right-click did not detach the tank");
        ItemStack returned =
                player.getInventory().items.stream()
                        .filter(stack -> stack.is(ModBlocks.TANK.asItem()))
                        .findFirst()
                        .orElse(ItemStack.EMPTY);
        var contents = returned.get(ModDataComponents.TANK_CONTENTS.get());
        h.assertTrue(contents != null, "Detached tank did not keep its fluid data");
        var storage = new TankStorage(() -> {});
        storage.readFromNBT(h.getLevel().registryAccess(), contents.copyTag());
        h.assertTrue(
                storage.getFluid().getFluid() == Fluids.LAVA && storage.getFluidAmount() == 4500,
                "Detaching changed the fluid or amount");
        h.succeed();
    }

    private static InteractionResult useBucket(ServerPlayer player, BlockPos tank, Item bucket) {
        ItemStack stack = new ItemStack(bucket);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(tank), Direction.UP, tank, false);
        return player.gameMode.useItemOn(
                player, player.level(), stack, InteractionHand.MAIN_HAND, hit);
    }

    private static ServerPlayer playerNear(GameTestHelper h, BlockPos position) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(position.getX() + .5, position.getY(), position.getZ() + 2, 180, 0);
        return player;
    }

    private static Minecart spawnEmptyCart(GameTestHelper h) {
        h.setBlock(TANK.below(), Blocks.STONE);
        h.setBlock(TANK, Blocks.RAIL);
        Vec3 pos = Vec3.atBottomCenterOf(h.absolutePos(TANK)).add(0, .08, 0);
        var empty = new Minecart(h.getLevel(), pos.x, pos.y, pos.z);
        h.getLevel().addFreshEntity(empty);
        return empty;
    }

    private static TankMinecart tankCart(GameTestHelper h) {
        var carts =
                h.getLevel().getEntitiesOfClass(TankMinecart.class, new AABB(h.absolutePos(TANK)));
        h.assertTrue(carts.size() == 1, "Expected a single tank cart");
        return carts.getFirst();
    }

    private static ItemStack tankItem(GameTestHelper h, FluidStack fluid) {
        var storage = new TankStorage(() -> {});
        storage.fill(fluid, FluidAction.EXECUTE);
        ItemStack tank = ModBlocks.TANK.toStack();
        tank.set(
                ModDataComponents.TANK_CONTENTS.get(), storage.pack(h.getLevel().registryAccess()));
        return tank;
    }
}
