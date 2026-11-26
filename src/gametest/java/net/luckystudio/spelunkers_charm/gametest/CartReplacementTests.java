// Cart replacement tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.UUID;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = LiftTestMod.ID)
public final class CartReplacementTests {
    private static final HashMap<UUID, Runnable> AFTER_TICK = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterTick(EntityTickEvent.Post event) {
        Runnable action = AFTER_TICK.remove(event.getEntity().getUUID());
        if (action != null) action.run();
    }

    @GameTest(template = "rail_large", timeoutTicks = 75)
    public static void loadingBlockAfterCartTickKeepsMovingTrain(GameTestHelper h) {
        var train = train(h);
        var engine = train[0];
        var old = train[1];
        var loaded = new AbstractMinecart[1];
        var before = new Vec3[1];
        var velocity = new Vec3[1];
        var exact = new boolean[1];
        var trace = new StringBuilder();
        h.runAfterDelay(
                20,
                () ->
                        AFTER_TICK.put(
                                old.getUUID(),
                                () -> {
                                    before[0] = old.position();
                                    velocity[0] = old.getDeltaMovement();
                                    var track =
                                            ((MinecartMotionVisuals) old)
                                                    .spelunkersCharm$motion()
                                                    .railPosition;
                                    var stack = new ItemStack(Items.STONE, 3);
                                    boolean success = MinecartBlockLoading.load(old, stack, null);
                                    loaded[0] =
                                            h
                                                    .getLevel()
                                                    .getEntitiesOfClass(
                                                            BlockMinecart.class,
                                                            old.getBoundingBox().inflate(1))
                                                    .stream()
                                                    .findFirst()
                                                    .orElse(null);
                                    var cart = loaded[0];
                                    exact[0] =
                                            success
                                                    && stack.isEmpty()
                                                    && cart != null
                                                    && cart.position().equals(before[0])
                                                    && cart.getDeltaMovement().equals(velocity[0])
                                                    && track != null
                                                    && track.equals(
                                                            ((MinecartMotionVisuals) cart)
                                                                    .spelunkersCharm$motion()
                                                                    .railPosition)
                                                    && cart.tickCount == old.tickCount
                                                    && cart.xOld == old.xOld
                                                    && cart.xo == old.xo;
                                }));
        var previous = new Vec3[1];
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 38) return;
                    var tracked = loaded[0] == null ? old : loaded[0];
                    trace.append(h.getTick())
                            .append(": engine=")
                            .append(engine.position())
                            .append(" removed=")
                            .append(engine.getRemovalReason())
                            .append(" wagon=")
                            .append(tracked.position())
                            .append(" removed=")
                            .append(tracked.getRemovalReason())
                            .append(" peers=")
                            .append(MinecartCouplings.get(h.getLevel()).neighbors(engine.getUUID()))
                            .append('\n');
                    if (h.getTick() < 22 || loaded[0] == null) return;
                    var cart = loaded[0];
                    h.assertTrue(exact[0], "Loading lost position, velocity or rail cursor");
                    h.assertTrue(
                            MinecartCouplings.get(h.getLevel())
                                    .linked(engine.getUUID(), cart.getUUID()),
                            "Loading broke train link:\n" + trace);
                    if (previous[0] != null) {
                        double distance = cart.position().distanceTo(previous[0]);
                        h.assertTrue(
                                distance > .03 && distance < .70,
                                "Loaded moving wagon stalled or jumped: " + distance);
                    }
                    previous[0] = cart.position();
                });
        h.runAfterDelay(
                38,
                () -> {
                    h.assertTrue(
                            loaded[0] != null && loaded[0].getX() > before[0].x + 3,
                            "Loaded cart did not continue with train");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 75)
    public static void loadingBrakedWagonPreservesWholeTrainHold(GameTestHelper h) {
        var train = train(h);
        var engine = train[0];
        var old = train[1];
        MinecartHandbrake.engage(engine);
        h.runAfterDelay(
                12,
                () -> {
                    Vec3 anchor = old.position();
                    h.assertTrue(
                            MinecartBlockLoading.load(old, new ItemStack(Items.CHEST), null),
                            "Chest load failed");
                    var cart =
                            h.getLevel()
                                    .getEntitiesOfClass(
                                            net.minecraft.world.entity.vehicle.MinecartChest.class,
                                            old.getBoundingBox().inflate(1))
                                    .getFirst();
                    h.assertTrue(
                            MinecartHandbrake.active(cart),
                            "Loading released inherited train brake");
                    h.runAfterDelay(
                            10,
                            () -> {
                                h.assertTrue(
                                        cart.position().distanceTo(anchor) < .002,
                                        "Loaded braked wagon moved on powered rail");
                                MinecartHandbrake.release(engine);
                                h.runAfterDelay(
                                        15,
                                        () -> {
                                            h.assertFalse(
                                                    MinecartHandbrake.active(cart),
                                                    "Loaded wagon stayed braked after engine release");
                                            h.assertTrue(
                                                    cart.getX() > anchor.x + .5,
                                                    "Loaded wagon did not restart");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void repeatedSameTickReplacementKeepsTrainAndClientSource(GameTestHelper h) {
        var train = train(h);
        var engine = train[0];
        var old = train[1];
        h.runAfterDelay(
                15,
                () -> {
                    var first = new Minecart(h.getLevel(), 0, 0, 0);
                    h.assertTrue(
                            MinecartCouplings.replaceEntity(old, first),
                            "First replacement failed");
                    var second = new BlockMinecart(h.getLevel(), 0, 0, 0);
                    second.addBlocks(Blocks.STONE.defaultBlockState(), 1);
                    h.assertTrue(
                            MinecartCouplings.replaceEntity(first, second),
                            "Second replacement failed");
                    h.assertTrue(
                            ((MinecartMotionVisuals) second).spelunkersCharm$replacedCartId()
                                    == old.getId(),
                            "Chained replacement lost original client source");
                    h.assertTrue(
                            MinecartCouplings.get(h.getLevel())
                                    .linked(engine.getUUID(), second.getUUID()),
                            "Chained conversion broke train");
                    Vec3 start = second.position();
                    h.runAfterDelay(
                            12,
                            () -> {
                                h.assertTrue(
                                        second.getX() > start.x + 2,
                                        "Chained replacement stopped moving");
                                h.succeed();
                            });
                });
    }

    private static AbstractMinecart[] train(GameTestHelper h) {
        for (int x = 1; x <= 62; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.REDSTONE_BLOCK);
            h.getLevel()
                    .setBlock(
                            h.absolutePos(new BlockPos(x, 2, 8)),
                            Blocks.POWERED_RAIL
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                                    .setValue(PoweredRailBlock.POWERED, true),
                            2);
        }
        Vec3 p = h.absoluteVec(new Vec3(12.5, 2.0625, 8.5));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        engine.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < 3; slot++) engine.setItem(slot, new ItemStack(Items.COAL, 4));
        var cart = new Minecart(h.getLevel(), p.x - 1.55, p.y, p.z);
        h.getLevel().addFreshEntity(engine);
        h.getLevel().addFreshEntity(cart);
        MinecartCouplings.get(h.getLevel()).link(engine, cart);
        return new AbstractMinecart[] {engine, cart};
    }
}
