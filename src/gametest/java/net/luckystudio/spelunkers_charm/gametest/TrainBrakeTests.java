// Train brake tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TrainBrakeTests {
    @GameTest(template = "rail_large", timeoutTicks = 150)
    public static void twentyWagonsBrakeTogetherOnPoweredRails(GameTestHelper h) {
        moving(h, 20, false, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 150)
    public static void thirtyTwoWagonsBrakeWithEngineTickedLast(GameTestHelper h) {
        moving(h, 32, true, false);
    }

    @GameTest(template = "rail_large", timeoutTicks = 150)
    public static void furnaceBrakesAllTwentyWagonsOnPoweredRails(GameTestHelper h) {
        moving(h, 20, true, true);
    }

    private static void moving(GameTestHelper h, int count, boolean engineLast, boolean furnace) {
        lane(h);
        var carts = new ArrayList<AbstractMinecart>();
        AbstractMinecart engine = null;
        if (!engineLast) {
            engine = engine(h, 53.5, furnace);
            carts.add(engine);
        }
        for (int i = 1; i <= count; i++) {
            Vec3 p = h.absoluteVec(new Vec3(53.5 - i * 1.55, 2.0625, 8.5));
            var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
            h.getLevel().addFreshEntity(cart);
            carts.add(cart);
        }
        if (engineLast) {
            engine = engine(h, 53.5, furnace);
            carts.addFirst(engine);
        }
        final AbstractMinecart root = engine;
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 1; i < carts.size(); i++) links.link(carts.get(i - 1), carts.get(i));
        for (var cart : carts) cart.setDeltaMovement(.37, 0, 0);
        List<Vec3> parked = new ArrayList<>();
        h.onEachTick(
                () -> {
                    long tick = h.getTick();
                    if (tick >= 74) return;
                    for (int i = 1; i < carts.size(); i++) {
                        MinecartRailPath gap =
                                MinecartRailPath.between(carts.get(i - 1), carts.get(i));
                        h.assertTrue(
                                gap != null && gap.distance() > 1.295 && gap.distance() < 2.4,
                                "Train compressed into a vehicle or stretched apart: tick="
                                        + tick
                                        + ", cart="
                                        + i
                                        + ", gap="
                                        + gap);
                        if (tick >= 18 && tick < 65)
                            h.assertTrue(
                                    Math.abs(gap.distance() - 1.55) < .035,
                                    "Uneven stopped coupling: tick="
                                            + tick
                                            + ", cart="
                                            + i
                                            + ", gap="
                                            + gap.distance());
                        if (tick >= 9 && tick < 69)
                            h.assertTrue(
                                    MinecartHandbrake.active(carts.get(i)),
                                    "Powered wagon did not inherit engine brake at tick " + tick);
                    }
                    if (tick == 18) for (var cart : carts) parked.add(cart.position());
                    if (tick > 18 && tick < 65) {
                        for (int i = 0; i < carts.size(); i++) {
                            h.assertTrue(
                                    carts.get(i).position().distanceTo(parked.get(i)) < .002,
                                    "Powered wagon crept away while engine held brake");
                            h.assertTrue(
                                    carts.get(i).getDeltaMovement().horizontalDistance() < .003,
                                    "Braked train kept rolling");
                        }
                    }
                });
        h.runAfterDelay(8, () -> MinecartHandbrake.engage(root));
        h.runAfterDelay(
                65,
                () -> {
                    var tail = carts.getLast();
                    Vec3 anchor = tail.position();
                    tail.push(.8, 0, 0);
                    tail.move(MoverType.PISTON, new Vec3(.5, 0, 0));
                    tail.setDeltaMovement(.8, 0, 0);
                    h.runAfterDelay(
                            4,
                            () -> {
                                h.assertTrue(
                                        tail.position().distanceTo(anchor) < .002,
                                        "Collision or piston moved a linked braked wagon");
                                MinecartHandbrake.release(root);
                            });
                });
        h.runAfterDelay(
                74,
                () -> {
                    for (var cart : carts)
                        h.assertFalse(
                                MinecartHandbrake.active(cart),
                                "Releasing engine left a wagon brake latched");
                    h.assertTrue(
                            root.getDeltaMovement().x > .01,
                            "Engine failed to restart after releasing whole-train brake");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void detachingTailReleasesOnlyDetachedWagons(GameTestHelper h) {
        lane(h);
        var root = engine(h, 35.5, false);
        var carts = new ArrayList<AbstractMinecart>();
        carts.add(root);
        for (int i = 1; i <= 4; i++) {
            Vec3 p = h.absoluteVec(new Vec3(35.5 - i * 1.55, 2.0625, 8.5));
            var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
            h.getLevel().addFreshEntity(cart);
            carts.add(cart);
        }
        var links = MinecartCouplings.get(h.getLevel());
        for (int i = 1; i < carts.size(); i++) links.link(carts.get(i - 1), carts.get(i));
        MinecartHandbrake.engage(root);
        h.runAfterDelay(
                8,
                () -> {
                    for (var cart : carts)
                        h.assertTrue(
                                MinecartHandbrake.active(cart),
                                "Brake did not reach complete train");
                    Vec3 front = carts.get(1).position(), tail = carts.get(3).position();
                    links.unlink(carts.get(2).getUUID(), carts.get(3).getUUID());
                    h.assertTrue(
                            MinecartHandbrake.active(carts.get(1))
                                    && MinecartHandbrake.active(carts.get(2)),
                            "Disconnect released the still-connected half");
                    h.assertFalse(
                            MinecartHandbrake.active(carts.get(3))
                                    || MinecartHandbrake.active(carts.get(4)),
                            "Detached tail retained inherited brake");
                    carts.get(3).setDeltaMovement(-.18, 0, 0);
                    carts.get(4).setDeltaMovement(-.18, 0, 0);
                    h.runAfterDelay(
                            10,
                            () -> {
                                h.assertTrue(
                                        carts.get(1).position().distanceTo(front) < .002,
                                        "Connected front escaped brake");
                                h.assertTrue(
                                        carts.get(3).getX() < tail.x - .5,
                                        "Detached wagons stayed locked on powered rail");
                                MinecartHandbrake.release(root);
                                h.runAfterDelay(
                                        8,
                                        () -> {
                                            h.assertFalse(
                                                    MinecartHandbrake.active(carts.get(1)),
                                                    "Engine release did not free remaining train");
                                            h.assertTrue(
                                                    carts.get(1).getDeltaMovement().x > .01,
                                                    "Released connected wagon failed to move");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large")
    public static void trainBrakePersistsWithoutLoadedEngineAndClearsOnUnlink(GameTestHelper h) {
        var root = engine(h, 35.5, false);
        Vec3 p = h.absoluteVec(new Vec3(33.95, 2.0625, 8.5));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(root, cart);
        MinecartHandbrake.engage(root);
        var saved = links.save(new CompoundTag(), h.getLevel().registryAccess());
        var restored = MinecartCouplings.load(saved, h.getLevel().registryAccess());
        h.assertTrue(
                root.getUUID().equals(restored.brakeSource(cart.getUUID())),
                "Saved train brake depends on engine being loaded");
        restored.unlink(root.getUUID(), cart.getUUID());
        h.assertTrue(
                restored.brakeSource(cart.getUUID()) == null, "Saved brake survived unlinking");
        h.succeed();
    }

    private static void lane(GameTestHelper h) {
        for (int x = 1; x <= 62; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.REDSTONE_BLOCK);
            h.getLevel()
                    .setBlock(
                            h.absolutePos(new BlockPos(x, 2, 8)),
                            Blocks.POWERED_RAIL
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                                    .setValue(PoweredRailBlock.POWERED, true),
                            Block.UPDATE_CLIENTS);
        }
    }

    private static AbstractMinecart engine(GameTestHelper h, double x, boolean furnace) {
        Vec3 p = h.absoluteVec(new Vec3(x, 2.0625, 8.5));
        if (furnace) {
            var cart = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
            cart.fuel = 20000;
            cart.xPush = 1;
            h.getLevel().addFreshEntity(cart);
            return cart;
        }
        var cart = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        cart.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < 3; slot++) cart.setItem(slot, new ItemStack(Items.COAL, 16));
        h.getLevel().addFreshEntity(cart);
        return cart;
    }
}
