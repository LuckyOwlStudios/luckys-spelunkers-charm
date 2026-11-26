// Minecart coupling tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.BlockMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplingVisuals;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartRailPath;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class MinecartCouplingTests {
    @GameTest(template = "empty")
    public static void hookTogglesCouplingsWithoutUsingCargoOrTnt(GameTestHelper h) {
        MinecartTNT tnt = cart(h, EntityType.TNT_MINECART, 4.5, 5.5);
        Minecart plain = cart(h, EntityType.MINECART, 6, 5.5);
        BlockMinecart cargo =
                new BlockMinecart(h.getLevel(), plain.getX() + 1.5, plain.getY(), plain.getZ());
        cargo.setDisplayBlockState(Blocks.STONE.defaultBlockState());
        cargo.setBlockAmount(8);
        h.getLevel().addFreshEntity(cargo);
        Minecart extra = cart(h, EntityType.MINECART, 6, 7);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.CART_COUPLING_HOOK.toStack());

        use(h, player, tnt);
        use(h, player, plain);
        use(h, player, plain);
        use(h, player, cargo);
        MinecartCouplings links = MinecartCouplings.get(h.getLevel());
        h.assertTrue(links.linked(tnt.getUUID(), plain.getUUID()), "First coupling missing");
        h.assertTrue(links.linked(cargo.getUUID(), plain.getUUID()), "Second coupling missing");
        var visuals = (MinecartCouplingVisuals) plain;
        h.assertTrue(
                visuals.spelunkersCharm$firstCouplingId() == tnt.getId()
                        && visuals.spelunkersCharm$secondCouplingId() == cargo.getId(),
                "Cart IDs were not synchronized");
        h.assertFalse(tnt.isPrimed(), "Hook primed TNT");
        h.assertTrue(cargo.getBlockAmount() == 8, "Hook unloaded cargo");

        use(h, player, tnt);
        use(h, player, cargo);
        h.assertFalse(links.linked(tnt.getUUID(), cargo.getUUID()), "Hook created a cycle");
        use(h, player, tnt);
        use(h, player, plain);
        use(h, player, extra);
        h.assertFalse(
                links.linked(plain.getUUID(), extra.getUUID()), "Cart accepted a third coupling");
        use(h, player, tnt);
        h.assertFalse(
                links.linked(tnt.getUUID(), plain.getUUID()), "Repeat use did not remove coupling");
        h.assertTrue(
                links.linked(cargo.getUUID(), plain.getUUID()),
                "Removing one coupling removed its neighbor");
        h.assertTrue(
                h.getLevel()
                        .getRecipeManager()
                        .byKey(SpelunkersCharm.id("cart_coupling_hook"))
                        .isPresent(),
                "Hook recipe missing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cartConversionKeepsBothCouplings(GameTestHelper h) {
        Minecart middle = cart(h, EntityType.MINECART, 6, 5.5);
        Minecart left = cart(h, EntityType.MINECART, 4.5, 5.5);
        Minecart right = cart(h, EntityType.MINECART, 7.5, 5.5);
        MinecartCouplings links = MinecartCouplings.get(h.getLevel());
        links.link(left.getUUID(), middle.getUUID());
        links.link(right.getUUID(), middle.getUUID());
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPose(Pose.CROUCHING);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 8));
        use(h, player, middle);

        AABB atMiddle = middle.getBoundingBox().inflate(0.2);
        BlockMinecart cargo =
                h.getLevel().getEntitiesOfClass(BlockMinecart.class, atMiddle).getFirst();
        h.assertTrue(
                cargo.getBlockAmount() == 8 && middle.isRemoved(),
                "Crouch load did not replace the cart");
        h.assertTrue(
                links.linked(left.getUUID(), cargo.getUUID())
                        && links.linked(right.getUUID(), cargo.getUUID()),
                "Loading blocks lost a coupling");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        use(h, player, cargo);
        Minecart empty = h.getLevel().getEntitiesOfClass(Minecart.class, atMiddle).getFirst();
        h.assertTrue(
                cargo.isRemoved()
                        && links.linked(left.getUUID(), empty.getUUID())
                        && links.linked(right.getUUID(), empty.getUUID()),
                "Unloading blocks lost a coupling");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FURNACE));
        use(h, player, empty);
        MinecartFurnace furnace =
                h.getLevel().getEntitiesOfClass(MinecartFurnace.class, atMiddle).getFirst();
        h.assertTrue(
                links.linked(left.getUUID(), furnace.getUUID())
                        && links.linked(right.getUUID(), furnace.getUUID()),
                "Furnace conversion lost a coupling");

        var loaded =
                MinecartCouplings.load(
                        links.save(new CompoundTag(), h.getLevel().registryAccess()),
                        h.getLevel().registryAccess());
        h.assertTrue(
                loaded.linked(furnace.getUUID(), left.getUUID())
                        && loaded.linked(right.getUUID(), furnace.getUUID()),
                "Serialized couplings were not restored");
        furnace.discard();
        h.assertTrue(
                links.neighbors(left.getUUID()).isEmpty()
                        && links.neighbors(right.getUUID()).isEmpty(),
                "Destroying a cart left dangling couplings");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void furnacePullsTwoCartsAlongTrack(GameTestHelper h) {
        for (int x = 2; x < 29; x++) rail(h, x, 5, RailShape.EAST_WEST);
        MinecartFurnace engine = cart(h, EntityType.FURNACE_MINECART, 8.5, 5.5);
        Minecart middle = cart(h, EntityType.MINECART, 6.95, 5.5);
        Minecart tail = cart(h, EntityType.MINECART, 5.4, 5.5);
        MinecartCouplings links = MinecartCouplings.get(h.getLevel());
        links.link(engine.getUUID(), middle.getUUID());
        links.link(middle.getUUID(), tail.getUUID());
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.moveTo(engine.getX() - 1, engine.getY(), engine.getZ(), 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL));
        use(h, player, engine);
        player.moveTo(engine.getX(), engine.getY() + 5, engine.getZ(), 0, 0);
        double start = tail.getX();
        for (int tick = 6; tick <= 36; tick++) {
            h.runAfterDelay(
                    tick,
                    () -> {
                        gap(h, engine, middle, 1.35, 2.05);
                        gap(h, middle, tail, 1.35, 2.05);
                        h.assertTrue(
                                tail.getDeltaMovement().x > -0.01,
                                "Tail reversed while being pulled forward");
                    });
        }
        h.runAfterDelay(
                36,
                () -> {
                    h.assertTrue(tail.getX() - start > 2.5, "Furnace did not pull the whole train");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void trainFollowsCurve(GameTestHelper h) {
        for (int x = 2; x <= 8; x++) rail(h, x, 5, RailShape.EAST_WEST);
        rail(h, 9, 5, RailShape.SOUTH_WEST);
        for (int z = 6; z < 21; z++) rail(h, 9, z, RailShape.NORTH_SOUTH);
        forceRail(h, 9, 5, RailShape.SOUTH_WEST);
        for (int z = 7; z < 18; z++) {
            h.setBlock(new BlockPos(9, 1, z), Blocks.REDSTONE_BLOCK);
            h.setBlock(
                    new BlockPos(9, 2, z),
                    Blocks.POWERED_RAIL
                            .defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE, RailShape.NORTH_SOUTH)
                            .setValue(PoweredRailBlock.POWERED, true));
        }
        Minecart lead = cart(h, EntityType.MINECART, 8.3, 5.5);
        Minecart tail = cart(h, EntityType.MINECART, 6.75, 5.5);
        MinecartCouplings.get(h.getLevel()).link(lead.getUUID(), tail.getUUID());
        double turn = lead.getZ();
        lead.setDeltaMovement(0.36, 0, 0);
        tail.setDeltaMovement(0.36, 0, 0);
        h.runAfterDelay(
                32,
                () -> {
                    h.assertTrue(tail.getZ() - turn > 2, "Trailing cart did not turn");
                    gap(h, lead, tail, 1.20, 2.65);
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void trainPullsCartOntoAnUphillRail(GameTestHelper h) {
        for (int x = 2; x < 10; x++) rail(h, x, 5, RailShape.EAST_WEST);
        for (int x = 10; x < 25; x++) {
            h.setBlock(new BlockPos(x, 2, 5), Blocks.REDSTONE_BLOCK);
            h.setBlock(
                    new BlockPos(x, 3, 5),
                    Blocks.POWERED_RAIL
                            .defaultBlockState()
                            .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                            .setValue(PoweredRailBlock.POWERED, true));
        }
        forceRail(h, 9, 5, RailShape.ASCENDING_EAST);
        Minecart lead = cart(h, EntityType.MINECART, 8.7, 5.5);
        Minecart tail = cart(h, EntityType.MINECART, 7.15, 5.5);
        MinecartCouplings links = MinecartCouplings.get(h.getLevel());
        links.link(lead.getUUID(), tail.getUUID());
        double startHeight = tail.getY();
        lead.setDeltaMovement(0.39, 0, 0);
        tail.setDeltaMovement(0.33, 0, 0);
        h.runAfterDelay(
                32,
                () -> {
                    h.assertTrue(
                            tail.getY() - startHeight > 0.9,
                            "Trailing cart did not climb the ramp");
                    h.assertTrue(
                            links.linked(lead.getUUID(), tail.getUUID()),
                            "Coupling snapped on the ramp");
                    gap(h, lead, tail, 1.20, 2.65);
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void buffersEngageBeforeVanillaContactWithoutTouchingParallelRail(
            GameTestHelper h) {
        for (int x = 2; x <= 13; x++) {
            rail(h, x, 5, RailShape.EAST_WEST);
            rail(h, x, 6, RailShape.EAST_WEST);
        }
        for (int x = 2; x <= 13; x++) {
            forceRail(h, x, 5, RailShape.EAST_WEST);
            forceRail(h, x, 6, RailShape.EAST_WEST);
        }
        Minecart first = cart(h, EntityType.MINECART, 5.5, 5.5);
        Minecart second = cart(h, EntityType.MINECART, 7.14, 5.5);
        Minecart parallel = cart(h, EntityType.MINECART, 7.14, 6.5);
        first.setDeltaMovement(0.36, 0, 0);
        double initial = second.getX();
        h.runAfterDelay(
                3,
                () -> {
                    h.assertTrue(
                            second.getX() > initial + 0.04,
                            "Buffer did not transfer momentum. "
                                    + "Path="
                                    + MinecartRailPath.between(first, second)
                                    + ", first speed="
                                    + first.getDeltaMovement()
                                    + ", second speed="
                                    + second.getDeltaMovement());
                    h.assertTrue(
                            first.distanceTo(second) > 1.18, "Carts overlapped the wider buffers");
                    h.assertTrue(
                            Math.abs(parallel.getX() - initial) < 0.001,
                            "Buffer pushed a cart on a parallel track");
                    h.succeed();
                });
    }

    private static void use(GameTestHelper h, ServerPlayer player, AbstractMinecart cart) {
        h.assertTrue(
                player.interactOn(cart, InteractionHand.MAIN_HAND).consumesAction(),
                "Minecart interaction was not handled");
    }

    private static void gap(
            GameTestHelper h,
            AbstractMinecart first,
            AbstractMinecart second,
            double min,
            double max) {
        MinecartRailPath path = MinecartRailPath.between(first, second);
        h.assertTrue(
                path != null && path.distance() >= min && path.distance() <= max,
                "Train spacing outside range: " + path);
    }

    private static <T extends AbstractMinecart> T cart(
            GameTestHelper h, EntityType<T> type, double x, double z) {
        T cart = type.create(h.getLevel());
        Vec3 absolute = h.absoluteVec(new Vec3(x, 2.08, z));
        cart.setPos(absolute);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static void rail(GameTestHelper h, int x, int z, RailShape shape) {
        h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(
                new BlockPos(x, 2, z),
                Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape));
    }

    private static void forceRail(GameTestHelper h, int x, int z, RailShape shape) {
        h.getLevel()
                .setBlock(
                        h.absolutePos(new BlockPos(x, 2, z)),
                        Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape),
                        Block.UPDATE_CLIENTS);
    }
}
