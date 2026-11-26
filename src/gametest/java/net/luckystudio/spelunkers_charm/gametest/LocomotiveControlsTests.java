// Locomotive controls tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class LocomotiveControlsTests {
    @GameTest(template = "rail_large", timeoutTicks = 100)
    public static void shiftClicksStopBeforeEveryDirectionChange(GameTestHelper h) {
        var engine = engine(h, 8);
        var player = h.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        h.runAfterDelay(
                20,
                () -> {
                    float facing = engine.bodyYaw();
                    double speed = engine.getDeltaMovement().x;
                    h.assertTrue(speed > .03, "Engine did not start before shift interaction");
                    engine.interact(player, InteractionHand.MAIN_HAND);
                    h.assertTrue(
                            engine.emergencyBrake()
                                    && !engine.isReversed()
                                    && engine.getDeltaMovement().x == speed,
                            "First click reversed or instantly stopped engine");
                    h.runAfterDelay(
                            5,
                            () -> {
                                h.assertTrue(
                                        engine.getDeltaMovement().horizontalDistance() < .003,
                                        "Stop click did not brake engine");
                                engine.interact(player, InteractionHand.MAIN_HAND);
                                h.assertTrue(
                                        !engine.emergencyBrake() && engine.isReversed(),
                                        "Second click failed to select reverse");
                                h.runAfterDelay(
                                        20,
                                        () -> {
                                            h.assertTrue(
                                                    engine.getDeltaMovement().x < -.03,
                                                    "Reverse gear did not drive backwards");
                                            engine.interact(player, InteractionHand.MAIN_HAND);
                                            h.assertTrue(
                                                    engine.emergencyBrake() && engine.isReversed(),
                                                    "Third click did not stop reverse gear");
                                            engine.interact(player, InteractionHand.MAIN_HAND);
                                            h.assertTrue(
                                                    !engine.emergencyBrake()
                                                            && !engine.isReversed(),
                                                    "Fourth click did not restore forward gear");
                                            h.assertTrue(
                                                    Math.abs(
                                                                    net.minecraft.util.Mth
                                                                            .wrapDegrees(
                                                                                    engine.bodyYaw()
                                                                                            - facing))
                                                            < .01,
                                                    "Drive cycle swapped physical front and rear");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large")
    public static void stoppedReverseCycleSurvivesSaveAndLoad(GameTestHelper h) {
        var engine = engine(h, 8);
        engine.selectDirection(true);
        engine.toggleEmergencyBrake();
        var saved = engine.saveWithoutId(new CompoundTag());
        var restored = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
        restored.load(saved);
        var player = h.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        float facing = restored.bodyYaw();
        restored.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(
                !restored.emergencyBrake()
                        && !restored.isReversed()
                        && restored.driveVector().x > .9,
                "Saved stop/reverse state resumed wrong gear");
        h.assertTrue(restored.bodyYaw() == facing, "Loading and cycling rotated body");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void coldLocomotiveStartsForwardOnFirstShiftClick(GameTestHelper h) {
        var engine = engine(h, 8);
        var tag = engine.saveWithoutId(new CompoundTag());
        tag.putDouble("PushX", 0);
        tag.putDouble("PushZ", 0);
        engine.load(tag);
        var player = h.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        engine.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(
                !engine.emergencyBrake() && !engine.isReversed() && engine.driveVector().x > .9,
                "First cold click selected stop or reverse");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void guiSelectsExplicitGearAndStopNeverReleasesBrake(GameTestHelper h) {
        var engine = engine(h, 8);
        var player = h.makeMockServerPlayerInLevel();
        player.setPos(engine.position());
        var menu = new LocomotiveMenu(1, player.getInventory(), engine, engine.menuData());
        player.containerMenu = menu;
        h.assertTrue(
                menu.clickMenuButton(player, LocomotiveMenu.BACKWARD_BUTTON)
                        && engine.isReversed()
                        && !engine.emergencyBrake(),
                "Reverse button failed");
        h.assertTrue(
                menu.clickMenuButton(player, LocomotiveMenu.STOP_BUTTON) && engine.emergencyBrake(),
                "Stop button failed");
        h.assertTrue(
                menu.clickMenuButton(player, LocomotiveMenu.STOP_BUTTON) && engine.emergencyBrake(),
                "Repeated stop released handbrake");
        h.assertTrue(
                menu.clickMenuButton(player, LocomotiveMenu.FORWARD_BUTTON)
                        && !engine.isReversed()
                        && !engine.emergencyBrake(),
                "Forward button failed");
        h.assertTrue(menu.slots.size() == 39, "New GUI lost inventory slots");
        for (int i = 0; i < 3; i++)
            h.assertTrue(
                    menu.slots.get(i).x == 55 + 25 * i && menu.slots.get(i).y == 34,
                    "Fuel slot missed supplied artwork");
        h.assertTrue(
                menu.slots.get(3).y == 84 && menu.slots.get(30).y == 142,
                "Inventory missed supplied artwork");
        player.setPos(engine.getX() + 30, engine.getY(), engine.getZ());
        h.assertFalse(
                menu.clickMenuButton(player, LocomotiveMenu.BACKWARD_BUTTON),
                "Distant direction click accepted");
        h.succeed();
    }

    @GameTest(template = "rail_large", timeoutTicks = 70)
    public static void fuelIndicatorsBurnDownIndependentlyAndLocksPauseFuel(GameTestHelper h) {
        var engine = engine(h, 8);
        h.runAfterDelay(
                5,
                () -> {
                    int before = engine.fuelRemaining(0);
                    engine.toggleBurner(0);
                    h.runAfterDelay(
                            30,
                            () -> {
                                h.assertTrue(
                                        engine.fuelRemaining(0) == before
                                                && engine.fuelRemaining(1) < before,
                                        "Lock failed to pause only its firebox");
                                h.assertTrue(
                                        engine.menuData().get(0) > engine.menuData().get(1),
                                        "GUI fuel levels did not reflect independent consumption");
                                engine.toggleBurner(0);
                                h.runAfterDelay(
                                        5,
                                        () -> {
                                            h.assertTrue(
                                                    engine.fuelRemaining(0) < before,
                                                    "Unlock failed to resume remaining fuel");
                                            h.succeed();
                                        });
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 120)
    public static void slowerLocomotiveAccelerationKeepsExistingMaximum(GameTestHelper h) {
        var engine = engine(h, 8);
        engine.toggleBurner(1);
        engine.toggleBurner(2);
        lane(h, 14);
        var p = h.absoluteVec(new Vec3(8.5, 2.0625, 14.5));
        var furnace = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
        furnace.fuel = 10000;
        furnace.xPush = 1;
        h.getLevel().addFreshEntity(furnace);
        h.runAfterDelay(
                20,
                () ->
                        h.assertTrue(
                                engine.getDeltaMovement().horizontalDistance()
                                        < furnace.getDeltaMovement().horizontalDistance() * .85,
                                "Locomotive did not accelerate more gently than furnace cart"));
        h.runAfterDelay(
                95,
                () -> {
                    double cap = MinecartSpeedRules.limit(engine);
                    h.assertTrue(
                            engine.getDeltaMovement().horizontalDistance() > cap * .98
                                    && Math.abs(cap - MinecartSpeedRules.limit(furnace)) < .001,
                            "Longer acceleration changed top speed");
                    h.succeed();
                });
    }

    private static void lane(GameTestHelper h, int z) {
        for (int x = 1; x < 62; x++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            h.setBlock(
                    new BlockPos(x, 2, z),
                    Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
    }

    private static LocomotiveMinecart engine(GameTestHelper h, int z) {
        lane(h, z);
        var p = h.absoluteVec(new Vec3(8.5, 2.0625, z + .5));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        engine.setDriveDirection(Direction.EAST);
        for (int slot = 0; slot < 3; slot++) engine.setItem(slot, new ItemStack(Items.COAL, 4));
        h.getLevel().addFreshEntity(engine);
        return engine;
    }
}
