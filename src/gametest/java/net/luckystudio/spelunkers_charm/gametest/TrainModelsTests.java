// Train models tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class TrainModelsTests {
    @GameTest(template = "empty")
    public static void locomotiveAcceptsOneRearCouplingAndPreservesFacingInReverse(
            GameTestHelper h) {
        Vec3 p = h.absoluteVec(new Vec3(10, 4, 8));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        engine.setDriveDirection(Direction.EAST);
        h.getLevel().addFreshEntity(engine);
        var front = new Minecart(h.getLevel(), p.x + 1.55, p.y, p.z);
        var rear = new Minecart(h.getLevel(), p.x - 1.55, p.y, p.z);
        var extra = new Minecart(h.getLevel(), p.x - 2, p.y, p.z);
        h.getLevel().addFreshEntity(front);
        h.getLevel().addFreshEntity(rear);
        h.getLevel().addFreshEntity(extra);
        var links = MinecartCouplings.get(h.getLevel());
        h.assertTrue(
                links.link(engine, front) == MinecartCouplings.Result.REAR_ONLY,
                "Front of locomotive still accepts coupling");
        h.assertTrue(
                links.link(engine, rear) == MinecartCouplings.Result.LINKED,
                "Rear coupling was rejected");
        h.assertTrue(
                links.link(engine, extra) == MinecartCouplings.Result.FULL,
                "Locomotive accepted two couplings");
        engine.switchDirection();
        h.assertTrue(
                engine.bodyDirection().x > .99
                        && engine.driveVector().x < -.99
                        && engine.leverAngle() == -30,
                "Reversing flipped the body or lever");
        h.assertTrue(
                MinecartCouplerSides.mask(engine, 1) == 1,
                "Reverse gear moved the rear hook to the front");
        engine.tickCount = 2;
        engine.setDriveDirection(Direction.EAST);
        h.assertTrue(
                engine.bodyDirection().x > .99 && engine.leverAngle() == 30,
                "Directional rail turned the body instead of changing gear");
        engine.toggleEmergencyBrake();
        h.assertTrue(engine.leverAngle() == 0, "Brake did not center lever");
        var saved = engine.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var restored = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
        restored.load(saved);
        h.assertTrue(
                restored.emergencyBrake()
                        && restored.leverAngle() == 0
                        && restored.bodyDirection().x > .99,
                "Saved model controls lost orientation");
        saved.remove("BodyYaw");
        var legacy = new LocomotiveMinecart(h.getLevel(), 0, 0, 0);
        legacy.load(saved);
        legacy.resolveLegacyFacing(new Vec3(1, 0, 0));
        h.assertTrue(
                legacy.bodyDirection().x < -.99 && legacy.driveVector().x > .99,
                "Legacy pushing train put its new hook at the front");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hookMaskFollowsOnlyTheConnectedPhysicalEnds(GameTestHelper h) {
        Vec3 p = h.absoluteVec(new Vec3(10, 4, 8));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        var a = new Minecart(h.getLevel(), p.x - 1.55, p.y, p.z);
        var b = new Minecart(h.getLevel(), p.x + 1.55, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        h.getLevel().addFreshEntity(a);
        h.getLevel().addFreshEntity(b);
        cart.setYRot(0);
        var links = MinecartCouplings.get(h.getLevel());
        h.assertTrue(MinecartCouplerSides.mask(cart, 1) == 0, "Uncoupled cart shows hooks");
        links.link(cart, a);
        h.assertTrue(MinecartCouplerSides.mask(cart, 1) == 1, "Wrong rear hook selected");
        links.link(cart, b);
        h.assertTrue(MinecartCouplerSides.mask(cart, 1) == 3, "Two-ended cart hides a hook");
        links.unlink(cart.getUUID(), a.getUUID());
        cart.setYRot(180);
        h.assertTrue(
                MinecartCouplerSides.mask(cart, 1) == 2,
                "Body rotation swapped the fixed hook to another model end");
        h.succeed();
    }

    private static TrainFlightFixture lane(GameTestHelper h) {
        var fixture =
                new TrainFlightFixture(h.getLevel(), h.absolutePos(BlockPos.ZERO), 55, 58, 60);
        fixture.build();
        return fixture;
    }

    private static Minecart cart(GameTestHelper h, TrainFlightFixture fixture, double x) {
        Vec3 p = fixture.point(x);
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        ((MinecartMotionVisuals) cart).spelunkersCharm$setOnRails(true);
        return cart;
    }

    @GameTest(template = "rail_large")
    public static void brakeStopsAtAirborneGapAndDoesNotFreezeUpperRails(GameTestHelper h) {
        var fixture = lane(h);
        var engine = fixture.engine(40, 0);
        ((MinecartMotionVisuals) engine).spelunkersCharm$setOnRails(true);
        var grounded = cart(h, fixture, 38.45);
        var air = cart(h, fixture, 36.9);
        var upper = cart(h, fixture, 35.35);
        air.setPos(air.getX(), air.getY() + 5, air.getZ());
        ((MinecartMotionVisuals) air).spelunkersCharm$setOnRails(false);
        ((MinecartMotionVisuals) air).spelunkersCharm$setAirborne(true);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(engine, grounded);
        links.link(grounded, air);
        links.link(air, upper);
        MinecartHandbrake.engage(engine);
        h.assertTrue(MinecartHandbrake.active(grounded), "Safe rail wagon did not brake");
        h.assertFalse(MinecartHandbrake.active(air), "Airborne wagon inherited handbrake");
        h.assertFalse(
                MinecartHandbrake.active(upper),
                "Brake crossed the flying section into upper rails");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void stretchedRailWagonKeepsRollingUntilCouplingSettles(GameTestHelper h) {
        var fixture = lane(h);
        var engine = fixture.engine(40, 0);
        ((MinecartMotionVisuals) engine).spelunkersCharm$setOnRails(true);
        var cart = cart(h, fixture, 36.8);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(engine, cart);
        MinecartHandbrake.engage(engine);
        h.assertFalse(
                MinecartHandbrake.active(cart), "Stretched rail wagon braked before settling");
        cart.setPos(fixture.point(38.45));
        MinecartTrainBrakes.invalidate(h.getLevel());
        h.assertTrue(MinecartHandbrake.active(cart), "Settled coupling did not acquire brake");
        links.unlink(engine.getUUID(), cart.getUUID());
        h.assertFalse(MinecartHandbrake.active(cart), "Detached wagon retained inherited brake");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void locomotiveRecipeConsumesMinecartBlastFurnaceAndFourIron(GameTestHelper h) {
        var recipe =
                (CraftingRecipe)
                        h.getLevel()
                                .getRecipeManager()
                                .byKey(SpelunkersCharm.id("locomotive"))
                                .orElseThrow()
                                .value();
        var input =
                CraftingInput.of(
                        3,
                        2,
                        List.of(
                                new ItemStack(Items.IRON_INGOT),
                                new ItemStack(Items.BLAST_FURNACE),
                                new ItemStack(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_INGOT),
                                new ItemStack(Items.MINECART),
                                new ItemStack(Items.IRON_INGOT)));
        h.assertTrue(recipe.matches(input, h.getLevel()), "New locomotive recipe is missing");
        h.succeed();
    }
}
