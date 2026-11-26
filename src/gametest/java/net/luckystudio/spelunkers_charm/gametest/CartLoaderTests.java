// Cart loader tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderBlock;
import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderBlockEntity;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.BlockMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLanterns;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class CartLoaderTests {
    private static final BlockPos LOADER = new BlockPos(8, 3, 8);
    private static final BlockPos POWER = LOADER.east();

    @GameTest(template = "empty")
    public static void loadsFullStackAndPreservesCouplingAndLantern(GameTestHelper h) {
        IItemHandler slot = setup(h);
        Minecart original = spawnCart(h, 8.5);
        Minecart neighbor = spawnCart(h, 6.85);
        MinecartCouplings links = MinecartCouplings.get(h.getLevel());
        links.link(original.getUUID(), neighbor.getUUID());
        CartLanterns.hang(original, new ItemStack(Items.LANTERN), h.makeMockServerPlayerInLevel());
        h.assertTrue(
                slot.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false).isEmpty(),
                "Loader should accept 64 blocks");
        h.assertTrue(
                slot.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1,
                "Loader should refuse a 65th block");
        h.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(
                3,
                () -> {
                    BlockMinecart loaded = loadedCart(h);
                    h.assertTrue(
                            original.isRemoved()
                                    && loaded.getBlockAmount() == 64
                                    && loaded.getDisplayBlockState().is(Blocks.COBBLESTONE),
                            "One pulse should load all 64 blocks");
                    h.assertTrue(slot.getStackInSlot(0).isEmpty(), "Loader should be empty");
                    h.assertTrue(
                            links.linked(loaded.getUUID(), neighbor.getUUID()),
                            "Coupling was lost");
                    h.assertTrue(
                            CartLanterns.getLantern(loaded) == CartLantern.LANTERN,
                            "Lantern was lost");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void hopperFeedsLoaderAndSteadyPowerDoesNotRepeat(GameTestHelper h) {
        IItemHandler slot = setup(h);
        spawnCart(h, 8.5);
        h.setBlock(LOADER.above(), Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(LOADER.above());
        hopper.setItem(0, new ItemStack(Items.STONE, 3));
        h.runAfterDelay(
                28,
                () -> {
                    h.assertTrue(
                            slot.getStackInSlot(0).getCount() == 3,
                            "Hopper did not feed the loader");
                    h.setBlock(POWER, Blocks.REDSTONE_BLOCK);
                    h.runAfterDelay(
                            3,
                            () -> {
                                h.assertTrue(
                                        loadedCart(h).getBlockAmount() == 3,
                                        "Initial pulse did not load the cart");
                                slot.insertItem(0, new ItemStack(Items.STONE, 5), false);
                            });
                    h.runAfterDelay(
                            9,
                            () ->
                                    h.assertTrue(
                                            h.getBlockState(LOADER)
                                                    .getValue(CartLoaderBlock.POWERED),
                                            "Loader stopped glowing before ten ticks"));
                    h.runAfterDelay(
                            11,
                            () -> {
                                BlockMinecart loaded = loadedCart(h);
                                h.assertFalse(
                                        h.getBlockState(LOADER).getValue(CartLoaderBlock.POWERED),
                                        "Loader stayed on after its pulse with a steady input");
                                h.assertTrue(
                                        loaded.getBlockAmount() == 3
                                                && slot.getStackInSlot(0).getCount() == 5,
                                        "Steady power repeated the transfer");
                                h.setBlock(POWER, Blocks.AIR);
                                h.setBlock(POWER, Blocks.REDSTONE_BLOCK);
                                h.setBlock(POWER, Blocks.AIR);
                                h.runAfterDelay(
                                        3,
                                        () -> {
                                            h.assertTrue(
                                                    h.getBlockState(LOADER)
                                                            .getValue(CartLoaderBlock.POWERED),
                                                    "A short input should still light the full"
                                                            + " pulse");
                                            h.assertTrue(
                                                    loaded.getBlockAmount() == 8
                                                            && slot.getStackInSlot(0).isEmpty(),
                                                    "A fresh pulse should top up the matching"
                                                            + " cargo");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "empty")
    public static void comparatorReadsSlotFill(GameTestHelper h) {
        IItemHandler slot = setup(h);
        BlockPos output = LOADER.north();
        h.setBlock(output.below(), Blocks.STONE);
        h.setBlock(
                output,
                Blocks.COMPARATOR
                        .defaultBlockState()
                        .setValue(ComparatorBlock.FACING, Direction.SOUTH));
        ComparatorBlockEntity comparator = (ComparatorBlockEntity) h.getBlockEntity(output);
        h.runAfterDelay(
                3,
                () -> {
                    h.assertTrue(
                            comparator.getOutputSignal() == 0, "Empty slot should output zero");
                    slot.insertItem(0, new ItemStack(Items.COBBLESTONE, 32), false);
                });
        h.runAfterDelay(
                7,
                () -> {
                    h.assertTrue(
                            comparator.getOutputSignal() == 8,
                            "Half-full slot should output eight");
                    var player = h.makeMockServerPlayerInLevel();
                    player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 32));
                    var loader = (CartLoaderBlockEntity) h.getBlockEntity(LOADER);
                    var menu = loader.createMenu(1, player.getInventory(), player);
                    h.assertTrue(
                            menu.quickMoveStack(player, 28).getCount() == 32,
                            "Could not top up the slot through the menu");
                });
        h.runAfterDelay(
                11,
                () -> {
                    h.assertTrue(
                            comparator.getOutputSignal() == 15, "Full slot should output fifteen");
                    slot.extractItem(0, 64, false);
                });
        h.runAfterDelay(
                15,
                () -> {
                    h.assertTrue(
                            comparator.getOutputSignal() == 0,
                            "Extracting the stack should reset the comparator");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void preservesStackWithoutCartAndConsumesOneSpecialBlock(GameTestHelper h) {
        IItemHandler slot = setup(h);
        slot.insertItem(0, new ItemStack(Items.FURNACE, 8), false);
        h.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(
                3,
                () -> {
                    h.assertTrue(
                            slot.getStackInSlot(0).getCount() == 8,
                            "An empty track consumed cargo");
                    spawnCart(h, 8.5);
                    h.setBlock(POWER, Blocks.AIR);
                    h.setBlock(POWER, Blocks.REDSTONE_BLOCK);
                    h.runAfterDelay(
                            3,
                            () -> {
                                h.assertTrue(
                                        h.getLevel()
                                                        .getEntitiesOfClass(
                                                                MinecartFurnace.class, cartArea(h))
                                                        .size()
                                                == 1,
                                        "Furnace was not used for the specialized cart");
                                h.assertTrue(
                                        slot.getStackInSlot(0).getCount() == 7,
                                        "A furnace cart should cost exactly one furnace");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "empty")
    public static void detectorRailLoadsCartWhileItMovesThrough(GameTestHelper h) {
        IItemHandler slot = setup(h);
        h.setBlock(
                LOADER.below(),
                Blocks.DETECTOR_RAIL
                        .defaultBlockState()
                        .setValue(DetectorRailBlock.SHAPE, RailShape.EAST_WEST));
        slot.insertItem(0, new ItemStack(Items.STONE, 16), false);
        Minecart cart = spawnCart(h, 7.2);
        cart.setDeltaMovement(0.24, 0, 0);
        h.runAfterDelay(
                12,
                () -> {
                    BlockMinecart loaded = loadedCart(h);
                    h.assertTrue(
                            cart.isRemoved()
                                    && loaded.getBlockAmount() == 16
                                    && slot.getStackInSlot(0).isEmpty(),
                            "Detector rail did not load the passing cart");
                    h.assertTrue(
                            loaded.getX() > h.absolutePos(LOADER).getX() + 1,
                            "Loading stopped the cart");
                    h.succeed();
                });
    }

    private static IItemHandler setup(GameTestHelper h) {
        for (int x = 5; x <= 14; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 2, 8),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        h.setBlock(LOADER, ModBlocks.CART_LOADER.get());
        IItemHandler slot =
                h.getLevel()
                        .getCapability(
                                Capabilities.ItemHandler.BLOCK,
                                h.absolutePos(LOADER),
                                Direction.UP);
        h.assertTrue(
                slot != null && slot.getSlots() == 1, "Loader needs one hopper-accessible slot");
        return slot;
    }

    private static Minecart spawnCart(GameTestHelper h, double x) {
        Vec3 pos = h.absoluteVec(new Vec3(x, 2.08, 8.5));
        Minecart cart = new Minecart(h.getLevel(), pos.x, pos.y, pos.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static AABB cartArea(GameTestHelper h) {
        return new AABB(h.absolutePos(LOADER.below())).inflate(4, 0, 0);
    }

    private static BlockMinecart loadedCart(GameTestHelper h) {
        var carts = h.getLevel().getEntitiesOfClass(BlockMinecart.class, cartArea(h));
        h.assertTrue(carts.size() == 1, "Expected a single cargo cart");
        return carts.getFirst();
    }
}
