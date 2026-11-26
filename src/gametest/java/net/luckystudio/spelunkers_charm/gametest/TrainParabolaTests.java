// Train parabola tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("spelunkers_parabola_tests")
@PrefixGameTestTemplate(false)
public final class TrainParabolaTests {
    private static Minecart falling(GameTestHelper h, double vertical) {
        var p = h.absoluteVec(new Vec3(8, 28, 8));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        cart.setDeltaMovement(.3, vertical, 0);
        h.getLevel().addFreshEntity(cart);
        MinecartFlightPhysics.launch(cart);
        ((MinecartMotionVisuals) cart).spelunkersCharm$setAirborne(true);
        return cart;
    }

    @GameTest(template = "rail_large", timeoutTicks = 40)
    public static void descendingCartGainsSpeedWhileGravityContinues(GameTestHelper h) {
        var cart = falling(h, 0);
        double start = cart.getY();
        h.runAfterDelay(
                15,
                () -> {
                    h.assertTrue(
                            cart.getDeltaMovement().length() > .60,
                            "Falling did not increase total speed: " + cart.getDeltaMovement());
                    h.assertTrue(
                            cart.getDeltaMovement().y < -.4 && cart.getY() < start - 3,
                            "Descent acceleration suspended gravity");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 30)
    public static void risingCartKeepsItsLaunchSpeedWithoutDescentBoost(GameTestHelper h) {
        var cart = falling(h, .4);
        h.runAfterDelay(
                6,
                () -> {
                    h.assertTrue(cart.getDeltaMovement().y > 0, "Ascent ended too early");
                    h.assertTrue(
                            Math.abs(cart.getDeltaMovement().x - .3) < .015,
                            "Ascent received a falling speed boost");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void savedAndConvertedCartsKeepTheirFlightMomentum(GameTestHelper h) {
        var cart = falling(h, -.2);
        var state = ((MinecartMotionVisuals) cart).spelunkersCharm$motion();
        state.physicalPitch = -.7;
        state.pitchRate = -.03;
        state.hingeSides.put(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), -1);
        var saved = cart.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var restored = new Minecart(h.getLevel(), 0, 0, 0);
        restored.load(saved);
        var reloaded = ((MinecartMotionVisuals) restored).spelunkersCharm$motion();
        h.assertTrue(
                reloaded.flightLaunched
                        && Math.abs(reloaded.physicalPitch + .7) < 1e-8
                        && restored.getDeltaMovement().distanceTo(cart.getDeltaMovement()) < 1e-8,
                "Save discarded flight momentum");
        var converted = new MinecartMotionState();
        state.copyMotionTo(converted);
        h.assertTrue(
                Math.abs(converted.pitchRate + .03) < 1e-8
                        && converted.hingeSides.equals(state.hingeSides),
                "Cargo conversion discarded flight momentum");
        MinecartCrash.clear(restored);
        h.assertFalse(reloaded.flightLaunched, "Landing retained flight state");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void brakedTrainDoesNotKeepAcceleratingItsAirborneTail(GameTestHelper h) {
        var p = h.absoluteVec(new Vec3(8, 20, 8));
        var engine = new LocomotiveMinecart(h.getLevel(), p.x, p.y, p.z);
        var cart = new Minecart(h.getLevel(), p.x - 1.55, p.y, p.z);
        h.getLevel().addFreshEntity(engine);
        h.getLevel().addFreshEntity(cart);
        MinecartCouplings.get(h.getLevel()).link(engine, cart);
        MinecartFlightPhysics.launch(cart);
        MinecartHandbrake.engage(engine);
        h.assertFalse(
                MinecartHandbrake.active(cart),
                "Flight acceleration added a handbrake to the airborne tail");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void locomotiveAdvancementRequiresCraftingAndAwardsOnlyOnce(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var server = h.getLevel().getServer();
        var advancement = server.getAdvancements().get(SpelunkersCharm.id("craft_locomotive"));
        h.assertTrue(advancement != null, "Locomotive advancement did not load");
        player.getAdvancements().revoke(advancement, "craft_locomotive");
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        var stack = new ItemStack(ModItems.LOCOMOTIVE.asItem());
        player.getInventory().add(stack.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
        h.assertFalse(
                progress.isDone(), "Picking up a locomotive granted the crafting advancement");
        var other =
                h.getLevel()
                        .getRecipeManager()
                        .byKey(
                                net.minecraft.resources.ResourceLocation.withDefaultNamespace(
                                        "minecart"))
                        .orElseThrow();
        player.triggerRecipeCrafted(other, List.of(new ItemStack(Items.IRON_INGOT, 5)));
        h.assertFalse(progress.isDone(), "A different recipe granted the locomotive advancement");
        var recipe =
                h.getLevel()
                        .getRecipeManager()
                        .byKey(SpelunkersCharm.id("locomotive"))
                        .orElseThrow();
        var ingredients =
                List.of(
                        new ItemStack(Items.MINECART),
                        new ItemStack(Items.BLAST_FURNACE),
                        new ItemStack(Items.IRON_INGOT, 4));
        player.triggerRecipeCrafted(recipe, ingredients);
        h.assertTrue(progress.isDone(), "First locomotive craft did not grant the advancement");
        var earned = progress.getCriterion("craft_locomotive").getObtained();
        player.triggerRecipeCrafted(recipe, ingredients);
        h.assertTrue(
                earned.equals(progress.getCriterion("craft_locomotive").getObtained()),
                "Repeated crafting replaced the first award");
        h.succeed();
    }

    @GameTest(template = "rail_flight_large", timeoutTicks = 400)
    public static void longTrainFinishesFallingAfterLeadEngineBrakesOnLanding(GameTestHelper h) {
        var fixture = TrainFlightOrderTests.ramp(h);
        var carts = fixture.train(24.5, 12);
        var engine = (LocomotiveMinecart) carts.getFirst();
        var links = MinecartCouplings.get(h.getLevel());
        boolean[] flew = {false}, braked = {false};
        long[] at = {0};
        int[] airAtBrake = {0};
        var positions = new Vec3[carts.size()];
        var velocities = new Vec3[carts.size()];
        var trace = new StringBuilder("tick,cart,x,y,vx,vy,rail,brake,anchor,pitch\n");
        h.onEachTick(
                () -> {
                    if (h.getTick() >= 350) return;
                    flew[0] |= ((MinecartMotionVisuals) engine).spelunkersCharm$airborne();
                    if (!braked[0]
                            && flew[0]
                            && engine.isOnRails()
                            && engine.getX() > fixture.origin.getX() + 58) {
                        for (var cart : carts)
                            if (((MinecartMotionVisuals) cart).spelunkersCharm$airborne())
                                airAtBrake[0]++;
                        MinecartHandbrake.engage(engine);
                        braked[0] = true;
                        at[0] = h.getTick();
                    }
                    for (int i = 0; i < carts.size(); i++) {
                        var cart = carts.get(i);
                        h.assertTrue(
                                cart.isAlive(),
                                "Mixed landing destroyed wagon "
                                        + i
                                        + ", position="
                                        + cart.position()
                                        + ", peak="
                                        + ((MinecartMotionVisuals) cart)
                                                .spelunkersCharm$motion()
                                                .fallPeak);
                        if (positions[i] != null)
                            h.assertTrue(
                                    cart.position().distanceTo(positions[i])
                                            < Math.max(
                                                    1.65,
                                                    Math.max(
                                                                    velocities[i].length(),
                                                                    cart.getDeltaMovement()
                                                                            .length())
                                                            + .75),
                                    "Mixed landing teleported wagon " + i);
                        positions[i] = cart.position();
                        velocities[i] = cart.getDeltaMovement();
                        trace.append(h.getTick())
                                .append(',')
                                .append(i)
                                .append(',')
                                .append(cart.getX() - fixture.origin.getX())
                                .append(',')
                                .append(cart.getY() - fixture.origin.getY())
                                .append(',')
                                .append(cart.getDeltaMovement().x)
                                .append(',')
                                .append(cart.getDeltaMovement().y)
                                .append(',')
                                .append(cart.isOnRails())
                                .append(',')
                                .append(MinecartHandbrake.active(cart))
                                .append(',')
                                .append(MinecartHandbrake.anchored(cart))
                                .append(',')
                                .append(
                                        ((MinecartMotionVisuals) cart)
                                                .spelunkersCharm$motion()
                                                .physicalPitch)
                                .append('\n');
                        if (i > 0)
                            h.assertTrue(
                                    links.linked(carts.get(i - 1).getUUID(), cart.getUUID()),
                                    "Mixed landing broke link "
                                            + i
                                            + ", tick="
                                            + h.getTick()
                                            + ", gap="
                                            + cart.distanceTo(carts.get(i - 1))
                                            + ", position="
                                            + cart.position()
                                            + ", parent="
                                            + carts.get(i - 1).position());
                        if (braked[0]
                                && i > 0
                                && ((MinecartMotionVisuals) cart).spelunkersCharm$airborne())
                            h.assertFalse(
                                    MinecartHandbrake.active(cart),
                                    "Airborne tail froze under train brake " + i);
                    }
                });
        h.runAfterDelay(
                350,
                () -> {
                    try {
                        java.nio.file.Files.writeString(
                                java.nio.file.Path.of("parabola-braked-tail.csv"), trace);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    h.assertTrue(
                            braked[0] && airAtBrake[0] >= 3,
                            "Fixture did not brake while long tail was airborne");
                    for (int i = 0; i < carts.size(); i++)
                        h.assertTrue(
                                carts.get(i).getY() < fixture.origin.getY() + 2.3,
                                "Wagon remained suspended: "
                                        + i
                                        + ", position="
                                        + carts.get(i).position()
                                        + ", brakeTick="
                                        + at[0]);
                    h.succeed();
                });
    }
}
