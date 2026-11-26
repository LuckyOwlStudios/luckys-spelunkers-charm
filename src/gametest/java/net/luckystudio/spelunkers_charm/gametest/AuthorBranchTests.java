// Author branch tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.rail.CargoRailBlock;
import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class AuthorBranchTests {
    @GameTest(template = "empty")
    public static void chainLinksShareHookAndRefundOnce(GameTestHelper h) {
        Minecart first = cart(h, 5.5);
        Minecart second = cart(h, 7.05);
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack chains = new ItemStack(Items.CHAIN, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, chains);
        player.interactOn(first, InteractionHand.MAIN_HAND);
        player.interactOn(second, InteractionHand.MAIN_HAND);
        var links = MinecartCouplings.get(h.getLevel());
        h.assertTrue(
                links.linked(first.getUUID(), second.getUUID()) && chains.getCount() == 2,
                "Chain did not couple carts using one item");
        var saved = links.save(new CompoundTag(), h.getLevel().registryAccess());
        var restored = MinecartCouplings.load(saved, h.getLevel().registryAccess());
        h.assertTrue(
                restored.save(new CompoundTag(), h.getLevel().registryAccess()).equals(saved),
                "Coupling material was not preserved across save/load");
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.CART_COUPLING_HOOK.toStack());
        player.interactOn(first, InteractionHand.MAIN_HAND);
        player.interactOn(second, InteractionHand.MAIN_HAND);
        h.assertFalse(
                links.linked(first.getUUID(), second.getUUID()),
                "Hook failed to undo chain coupling");
        h.assertFalse(
                links.unlinkAndDrop(h.getLevel(), first.getUUID(), second.getUUID()),
                "Coupling refunded twice");
        long refunded =
                h
                        .getLevel()
                        .getEntitiesOfClass(ItemEntity.class, first.getBoundingBox().inflate(4))
                        .stream()
                        .filter(item -> item.getItem().is(Items.CHAIN))
                        .mapToInt(item -> item.getItem().getCount())
                        .sum();
        h.assertTrue(refunded == 1, "Expected one refunded chain, got " + refunded);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cargoRailReadsCustomStorage(GameTestHelper h) {
        MineCrateMinecart crate =
                new MineCrateMinecart(ModEntityType.MINE_CRATE_MINECART.get(), h.getLevel());
        TankMinecart tank = new TankMinecart(ModEntityType.TANK_MINECART.get(), h.getLevel());
        h.assertTrue(
                CargoRailBlock.cargoOf(crate) == 0 && CargoRailBlock.cargoOf(tank) == 0,
                "Empty storage emitted cargo signal");
        crate.storage().insertItem(0, new ItemStack(Items.COBBLESTONE, 1024), false);
        tank.storage().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(
                CargoRailBlock.cargoOf(crate) == 8 && CargoRailBlock.cargoOf(tank) == 8,
                "Half-full storage signal incorrect");
        crate.storage().insertItem(0, new ItemStack(Items.COBBLESTONE, 1024), false);
        tank.storage().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(
                CargoRailBlock.cargoOf(crate) == 15 && CargoRailBlock.cargoOf(tank) == 15,
                "Full storage signal incorrect");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hopperUnloadsBlockCartAndPreservesLinks(GameTestHelper h) {
        Minecart neighbor = cart(h, 7.05);
        var position = h.absoluteVec(new Vec3(5.5, 2.08, 8.5));
        BlockMinecart cargo = new BlockMinecart(h.getLevel(), position.x, position.y, position.z);
        cargo.addBlocks(Blocks.STONE.defaultBlockState(), 1);
        h.getLevel().addFreshEntity(cargo);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(cargo.getUUID(), neighbor.getUUID(), Items.CHAIN);
        BlockPos hopper = new BlockPos(5, 1, 8);
        h.setBlock(hopper, Blocks.HOPPER);
        h.runAfterDelay(
                12,
                () -> {
                    var container =
                            (net.minecraft.world.level.block.entity.HopperBlockEntity)
                                    h.getBlockEntity(hopper);
                    h.assertTrue(
                            container.getItem(0).is(Items.STONE),
                            "Hopper did not extract block cart contents");
                    h.assertTrue(cargo.isRemoved(), "Empty cargo cart did not convert");
                    var empty =
                            h.getLevel()
                                    .getEntitiesOfClass(
                                            Minecart.class, cargo.getBoundingBox().inflate(0.3))
                                    .getFirst();
                    h.assertTrue(
                            links.linked(empty.getUUID(), neighbor.getUUID()),
                            "Hopper unloading lost coupling");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void directionalRailHoldsCoupledCart(GameTestHelper h) {
        var position = h.absoluteVec(new Vec3(5.5, 2.0625, 8.5));
        var held = new LocomotiveMinecart(h.getLevel(), position.x, position.y, position.z);
        h.getLevel().addFreshEntity(held);
        Minecart peer = cart(h, 7.05);
        BlockPos pos = new BlockPos(5, 2, 8);
        var state =
                ModBlocks.DIRECTIONAL_RAIL
                        .get()
                        .defaultBlockState()
                        .setValue(DirectionalRailBlock.SHAPE, RailShape.EAST_WEST)
                        .setValue(DirectionalRailBlock.FACING, Direction.EAST)
                        .setValue(DirectionalRailBlock.POWERED, false);
        h.setBlock(pos, state);
        MinecartCouplings.get(h.getLevel()).link(held.getUUID(), peer.getUUID());
        held.setDeltaMovement(0.3, 0, 0);
        peer.setDeltaMovement(0.3, 0, 0);
        double center = held.getX();
        h.runAfterDelay(
                12,
                () -> {
                    h.assertTrue(
                            held.getX() >= center && held.getX() < center + 0.40,
                            "Couplings pulled cart off unpowered directional rail");
                    h.assertTrue(
                            held.getDeltaMovement().horizontalDistanceSqr() < 0.0001,
                            "Held cart kept moving");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void locomotiveSupportsFuelInsertionOnly(GameTestHelper h) {
        var engine = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
        h.assertTrue(engine.canPlaceItem(0, new ItemStack(Items.COAL)), "Locomotive rejected fuel");
        h.assertFalse(
                engine.canPlaceItem(0, new ItemStack(Items.STONE)), "Locomotive accepted non-fuel");
        h.assertFalse(
                engine.canTakeItem(null, 0, new ItemStack(Items.COAL)),
                "Hopper can steal locomotive fuel");
        h.assertTrue(
                engine.getPickResult().is(ModItems.LOCOMOTIVE.get()),
                "Locomotive pick item missing");
        h.succeed();
    }

    private static Minecart cart(GameTestHelper h, double x) {
        for (int block = 3; block <= 12; block++) {
            h.setBlock(new BlockPos(block, 1, 8), Blocks.STONE);
            h.setBlock(
                    new BlockPos(block, 2, 8),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        var position = h.absoluteVec(new Vec3(x, 2.08, 8.5));
        var cart = new Minecart(h.getLevel(), position.x, position.y, position.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }
}
