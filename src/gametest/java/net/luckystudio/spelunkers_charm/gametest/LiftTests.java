package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.DirectionalLiftTrackBlock;
import net.luckystudio.spelunkers_charm.block.custom.LiftTrackBlock;
import net.luckystudio.spelunkers_charm.block.custom.PoweredLiftTrackBlock;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftCollision;
import net.luckystudio.spelunkers_charm.entity.util.ControlType;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class LiftTests {
    private static final BlockPos CENTER = new BlockPos(12, 5, 12);
    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private static void near(
            GameTestHelper h, double actual, double expected, double tolerance, String what) {
        h.assertTrue(
                Math.abs(actual - expected) <= tolerance,
                what + ": expected " + expected + ", got " + actual);
    }

    private static AbstractLift lift(
            GameTestHelper h, AbstractLift.Type type, Direction facing, int y) {
        BlockPos p = h.absolutePos(new BlockPos(CENTER.getX(), y, CENTER.getZ()));
        AbstractLift e =
                AbstractLift.createLift(
                        h.getLevel(),
                        p.getX() + 0.5,
                        p.getY(),
                        p.getZ() + 0.5,
                        type,
                        ItemStack.EMPTY,
                        null);
        e.setYRot(facing.toYRot());
        e.setControlType(ControlType.LEVER);
        h.getLevel().addFreshEntity(e);
        return e;
    }

    @SuppressWarnings("unchecked")
    private static List<BlockPos> tracks(AbstractLift lift) {
        try {
            Method m = lift.getClass().getDeclaredMethod("trackPositions");
            m.setAccessible(true);
            return (List<BlockPos>) m.invoke(lift);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Direction inward(AbstractLift lift, BlockPos p) {
        int dx = lift.blockPosition().getX() - p.getX(),
                dz = lift.blockPosition().getZ() - p.getZ();
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static List<BlockPos> rails(GameTestHelper h, AbstractLift lift, int minY, int maxY) {
        return rails(h, lift, minY, maxY, true);
    }

    private static List<BlockPos> rails(
            GameTestHelper h, AbstractLift lift, int minY, int maxY, boolean power) {
        List<BlockPos> result = new ArrayList<>();
        int base = h.absolutePos(BlockPos.ZERO).getY();
        for (BlockPos p : tracks(lift))
            for (int y = minY; y <= maxY; y++) {
                BlockPos r = new BlockPos(p.getX(), base + y, p.getZ());
                result.add(r);
                h.getLevel()
                        .setBlockAndUpdate(
                                r,
                                ModBlocks.POWERED_LIFT_TRACK
                                        .get()
                                        .defaultBlockState()
                                        .setValue(
                                                HorizontalDirectionalBlock.FACING,
                                                inward(lift, r)));
            }
        if (power)
            for (BlockPos p : result)
                h.getLevel()
                        .setBlockAndUpdate(
                                p.relative(inward(lift, p).getOpposite()),
                                Blocks.REDSTONE_BLOCK.defaultBlockState());
        return result;
    }

    private static Sheep sheep(GameTestHelper h, AbstractLift lift, double offsetX) {
        Sheep e = EntityType.SHEEP.create(h.getLevel());
        e.setNoAi(true);
        e.setSilent(true);
        e.setPos(lift.getX() + offsetX, lift.getBoundingBox().maxY, lift.getZ());
        e.setOnGround(true);
        e.setDeltaMovement(Vec3.ZERO);
        e.fallDistance = 0;
        h.getLevel().addFreshEntity(e);
        return e;
    }

    private static void carry(GameTestHelper h, AbstractLift lift, double delta) {
        AABB old = lift.getBoundingBox();
        lift.setPos(lift.getX(), lift.getY() + delta, lift.getZ());
        LiftCollision.carry(lift, old, lift.getBoundingBox());
    }

    @GameTest(template = "empty")
    public static void everySizeAndFacingRecognizesInwardRails(GameTestHelper h) {
        for (AbstractLift.Type type : AbstractLift.Type.values())
            for (Direction facing : HORIZONTAL) {
                AbstractLift e = lift(h, type, facing, 5);
                List<BlockPos> all = rails(h, e, 5, 6);
                h.assertTrue(
                        e.hasValidTracks(), type + " " + facing + " rejected inward guide ring");
                BlockPos bad = tracks(e).getFirst();
                h.getLevel()
                        .setBlockAndUpdate(
                                bad,
                                h.getLevel()
                                        .getBlockState(bad)
                                        .setValue(
                                                HorizontalDirectionalBlock.FACING,
                                                inward(e, bad).getOpposite()));
                h.assertFalse(e.hasValidTracks(), type + " " + facing + " accepted outward guide");
                for (BlockPos p : all) {
                    h.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    h.getLevel()
                            .setBlockAndUpdate(
                                    p.relative(inward(e, p).getOpposite()),
                                    Blocks.AIR.defaultBlockState());
                }
                e.discard();
            }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void carryingSupportsAllSizesAndOrientations(GameTestHelper h) {
        for (AbstractLift.Type type : AbstractLift.Type.values())
            for (Direction facing : HORIZONTAL) {
                AbstractLift e = lift(h, type, facing, 5);
                Sheep s = sheep(h, e, 0);
                float health = s.getHealth();
                carry(h, e, 0.15);
                near(
                        h,
                        s.getY(),
                        e.getBoundingBox().maxY,
                        0.003,
                        "Upward sheep support " + type + facing);
                carry(h, e, -0.15);
                near(
                        h,
                        s.getY(),
                        e.getBoundingBox().maxY,
                        0.003,
                        "Downward sheep support " + type + facing);
                near(h, s.getHealth(), health, 0, "Sheep health");
                s.discard();
                e.discard();
            }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void itemsAndPlayersMoveUpAndDown(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.MEDIUM, Direction.EAST, 5);
        ItemEntity item =
                new ItemEntity(
                        h.getLevel(),
                        e.getX() + 1,
                        e.getBoundingBox().maxY,
                        e.getZ(),
                        new ItemStack(Items.DIAMOND));
        item.setDeltaMovement(Vec3.ZERO);
        item.setOnGround(true);
        h.getLevel().addFreshEntity(item);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPos(e.getX() - 1, e.getBoundingBox().maxY, e.getZ());
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        carry(h, e, 0.15);
        near(h, item.getY(), e.getBoundingBox().maxY, 0.003, "Upward item support");
        near(h, player.getY(), e.getBoundingBox().maxY, 0.003, "Upward player support");
        carry(h, e, -0.15);
        near(h, item.getY(), e.getBoundingBox().maxY, 0.003, "Downward item support");
        near(h, player.getY(), e.getBoundingBox().maxY, 0.003, "Downward player support");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void jumpingAndFlyingEntitiesDetach(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.MEDIUM, Direction.NORTH, 5);
        Sheep jumping = sheep(h, e, -1);
        Sheep flying = sheep(h, e, 1);
        jumping.setDeltaMovement(0, 0.42, 0);
        flying.setNoGravity(true);
        double jy = jumping.getY(), fy = flying.getY();
        carry(h, e, -0.15);
        near(h, jumping.getY(), jy, 0.003, "Jumping rider dragged down");
        near(h, flying.getY(), fy, 0.003, "Flying rider dragged down");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void entitiesBesideAndUnderDeckAreNotCarried(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 5);
        Sheep side = sheep(h, e, 3);
        Sheep below = sheep(h, e, 0);
        below.setPos(e.getX(), e.getY() - 2, e.getZ());
        double sy = side.getY(), by = below.getY();
        carry(h, e, 0.15);
        near(h, side.getY(), sy, 0.003, "Side entity carried");
        near(h, below.getY(), by, 0.003, "Entity below deck carried");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mountedEntitiesMoveOnlyOnce(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.LARGE, Direction.WEST, 5);
        Boat boat = EntityType.BOAT.create(h.getLevel());
        boat.setPos(e.getX(), e.getBoundingBox().maxY, e.getZ());
        boat.setDeltaMovement(Vec3.ZERO);
        boat.setOnGround(true);
        h.getLevel().addFreshEntity(boat);
        Sheep rider = sheep(h, e, 0);
        rider.startRiding(boat, true);
        boat.positionRider(rider);
        double by = boat.getY(), ry = rider.getY();
        carry(h, e, 0.15);
        near(h, boat.getY(), by + 0.15, 0.003, "Boat support");
        near(h, rider.getY(), ry + 0.15, 0.03, "Mounted passenger carried twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fallingEntitiesLandOnRisingDeck(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 5);
        Sheep s = sheep(h, e, 0);
        s.setPos(s.getX(), s.getY() + 0.1, s.getZ());
        s.setOnGround(false);
        s.setDeltaMovement(0, -0.25, 0);
        s.fallDistance = 10;
        carry(h, e, 0.15);
        near(h, s.getY(), e.getBoundingBox().maxY, 0.003, "Falling rider passed through deck");
        near(h, s.fallDistance, 0, 0, "Fall distance not reset");
        h.succeed();
    }

    private static void moving(
            GameTestHelper h, AbstractLift.Type type, Direction facing, boolean up) {
        AbstractLift e = lift(h, type, facing, up ? 4 : 9);
        rails(h, e, 2, 14);
        Sheep s = sheep(h, e, 0);
        float health = s.getHealth();
        s.setPos(e.getX() - 0.65, e.getBoundingBox().maxY, e.getZ() + 0.65);
        ItemEntity item =
                new ItemEntity(
                        h.getLevel(),
                        e.getX() + 0.8,
                        e.getBoundingBox().maxY,
                        e.getZ() - 0.8,
                        new ItemStack(Items.IRON_INGOT));
        item.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(item);
        ArmorStand stand = EntityType.ARMOR_STAND.create(h.getLevel());
        stand.setPos(e.getX() - 0.8, e.getBoundingBox().maxY, e.getZ() - 0.8);
        stand.setDeltaMovement(Vec3.ZERO);
        stand.setOnGround(true);
        h.getLevel().addFreshEntity(stand);
        Minecart cart = EntityType.MINECART.create(h.getLevel());
        cart.setPos(e.getX() + 0.65, e.getBoundingBox().maxY, e.getZ() + 0.65);
        cart.setDeltaMovement(Vec3.ZERO);
        cart.setOnGround(true);
        h.getLevel().addFreshEntity(cart);
        double start = e.getY();
        e.setMovementType(up ? MovementType.UP : MovementType.DOWN);
        for (int tick = 1; tick <= 18; tick++) {
            final int frame = tick;
            h.runAfterDelay(
                    tick,
                    () -> {
                        near(
                                h,
                                item.getY(),
                                e.getBoundingBox().maxY,
                                0.05,
                                "Item left deck on travel tick "
                                        + frame
                                        + " (vy="
                                        + item.getDeltaMovement().y
                                        + ", grounded="
                                        + item.onGround()
                                        + ", noPhysics="
                                        + item.noPhysics
                                        + ")");
                        h.assertFalse(
                                item.noPhysics, "Item was ejected from deck on tick " + frame);
                        near(
                                h,
                                stand.getBoundingBox().minY,
                                e.getBoundingBox().maxY,
                                0.035,
                                "Armor stand left deck on travel tick " + frame);
                        near(
                                h,
                                cart.getBoundingBox().minY,
                                e.getBoundingBox().maxY,
                                0.035,
                                "Minecart left deck on travel tick " + frame);
                    });
        }
        h.runAfterDelay(
                18,
                () -> {
                    h.assertTrue(
                            (e.getY() - start) * (up ? 1 : -1) > 2.0,
                            "Lift did not travel " + type + facing);
                    near(h, s.getY(), e.getBoundingBox().maxY, 0.04, "Sheep lost moving deck");
                    near(
                            h,
                            item.getY(),
                            e.getBoundingBox().maxY,
                            0.05,
                            "Item lost moving deck (vy="
                                    + item.getDeltaMovement().y
                                    + ", grounded="
                                    + item.onGround()
                                    + ", noPhysics="
                                    + item.noPhysics
                                    + ")");
                    near(h, s.getHealth(), health, 0, "Travel damaged sheep");
                    near(h, s.fallDistance, 0, 0.01, "Travel accumulated fall damage");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void smallLiftMovesUp(GameTestHelper h) {
        moving(h, AbstractLift.Type.SMALL, Direction.NORTH, true);
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void smallLiftMovesDown(GameTestHelper h) {
        moving(h, AbstractLift.Type.SMALL, Direction.EAST, false);
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void mediumLiftMovesUp(GameTestHelper h) {
        moving(h, AbstractLift.Type.MEDIUM, Direction.SOUTH, true);
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void mediumLiftMovesDown(GameTestHelper h) {
        moving(h, AbstractLift.Type.MEDIUM, Direction.WEST, false);
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void largeLiftMovesUp(GameTestHelper h) {
        moving(h, AbstractLift.Type.LARGE, Direction.EAST, true);
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void largeLiftMovesDown(GameTestHelper h) {
        moving(h, AbstractLift.Type.LARGE, Direction.NORTH, false);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void upperEndpointStopsExactlyWithoutOvershoot(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.MEDIUM, Direction.SOUTH, 4);
        rails(h, e, 3, 8);
        e.setMovementType(MovementType.UP);
        int base = h.absolutePos(BlockPos.ZERO).getY();
        h.runAfterDelay(
                50,
                () -> {
                    near(h, e.getY(), base + 8, 0.0001, "Upper rail endpoint");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Lift still moving past endpoint");
                    near(h, e.getDeltaMovement().y, 0, 0.0001, "Residual upward velocity");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void lowerEndpointStopsExactlyWithoutOvershoot(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.WEST, 8);
        rails(h, e, 3, 9);
        e.setMovementType(MovementType.DOWN);
        int base = h.absolutePos(BlockPos.ZERO).getY();
        h.runAfterDelay(
                50,
                () -> {
                    near(h, e.getY(), base + 3, 0.0001, "Lower rail endpoint");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Lift still moving past endpoint");
                    near(h, e.getDeltaMovement().y, 0, 0.0001, "Residual downward velocity");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void missingOrWrongFacingNextRailPreventsDeparture(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.LARGE, Direction.EAST, 5);
        rails(h, e, 5, 6);
        BlockPos p = tracks(e).getFirst().above();
        h.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        double start = e.getY();
        e.setMovementType(MovementType.UP);
        h.runAfterDelay(
                4,
                () -> {
                    near(h, e.getY(), start, 0.0001, "Missing next rail allowed departure");
                    h.getLevel()
                            .setBlockAndUpdate(
                                    p,
                                    ModBlocks.POWERED_LIFT_TRACK
                                            .get()
                                            .defaultBlockState()
                                            .setValue(
                                                    HorizontalDirectionalBlock.FACING,
                                                    inward(e, p).getOpposite()));
                    e.setMovementType(MovementType.UP);
                });
        h.runAfterDelay(
                9,
                () -> {
                    near(h, e.getY(), start, 0.0001, "Outward next rail allowed departure");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void riderCeilingStopsLiftBeforeCrushing(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.MEDIUM, Direction.NORTH, 4);
        rails(h, e, 2, 12);
        Sheep s = sheep(h, e, 0);
        float health = s.getHealth();
        BlockPos ceiling =
                new BlockPos(
                        e.blockPosition().getX(),
                        h.absolutePos(BlockPos.ZERO).getY() + 8,
                        e.blockPosition().getZ());
        h.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
        e.setMovementType(MovementType.UP);
        h.runAfterDelay(
                30,
                () -> {
                    h.assertTrue(
                            s.getBoundingBox().maxY <= ceiling.getY() + 0.002,
                            "Sheep entered ceiling");
                    near(h, s.getHealth(), health, 0, "Ceiling crushed sheep");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Lift pushing rider into ceiling");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void controlsSurviveSaveAndLegacyReload(GameTestHelper h) {
        for (ControlType control : ControlType.values()) {
            AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.WEST, 5);
            e.setControlType(control);
            e.setMovementType(MovementType.DOWN);
            CompoundTag tag = new CompoundTag();
            e.saveWithoutId(tag);
            AbstractLift restored =
                    AbstractLift.createLift(
                            h.getLevel(),
                            e.getX(),
                            e.getY(),
                            e.getZ(),
                            AbstractLift.Type.SMALL,
                            ItemStack.EMPTY,
                            null);
            restored.load(tag);
            h.assertTrue(restored.getControlType() == control, "Saved control changed: " + control);
            h.assertTrue(restored.getMovementType() == MovementType.DOWN, "Saved movement changed");
            tag.putInt("controlType", control.ordinal());
            restored.load(tag);
            h.assertTrue(
                    restored.getControlType() == control,
                    "Legacy integer control not loaded: " + control);
            e.discard();
        }
        h.succeed();
    }

    private static AABB arena(GameTestHelper h) {
        return AABB.encapsulatingFullBlocks(
                h.absolutePos(BlockPos.ZERO), h.absolutePos(new BlockPos(23, 19, 23)));
    }

    private static UseOnContext context(
            GameTestHelper h,
            ServerPlayer player,
            ItemStack stack,
            BlockPos clicked,
            Direction face) {
        return new UseOnContext(
                h.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                new BlockHitResult(Vec3.atCenterOf(clicked), face, clicked, false));
    }

    private static List<BlockPos> placementRails(GameTestHelper h) {
        AbstractLift layout = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 5);
        List<BlockPos> p = rails(h, layout, 5, 5);
        layout.discard();
        return p;
    }

    @GameTest(template = "empty")
    public static void nullPlayerCanPlaceFromEveryTrackFace(GameTestHelper h) {
        List<BlockPos> ring = placementRails(h);
        BlockPos clicked = ring.getFirst();
        for (Direction face : Direction.values()) {
            ItemStack stack = new ItemStack(ModItems.SMALL_LIFT.get());
            InteractionResult result =
                    stack.getItem().useOn(context(h, null, stack, clicked, face));
            h.assertTrue(result.consumesAction(), "Null-player track placement failed on " + face);
            var lifts = h.getLevel().getEntitiesOfClass(AbstractLift.class, arena(h));
            h.assertTrue(lifts.size() == 1, "Expected exactly one placed lift on " + face);
            h.assertTrue(
                    lifts.getFirst().hasValidTracks(), "Placed lift has invalid rails on " + face);
            lifts.getFirst().discard();
            h.assertTrue(stack.isEmpty(), "Successful placement did not consume item");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void placementPreservesCreativeStackAndRejectsOverlap(GameTestHelper h) {
        List<BlockPos> ring = placementRails(h);
        BlockPos clicked = ring.getFirst();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = true;
        player.setPos(clicked.getX() + 8, clicked.getY(), clicked.getZ() + 8);
        ItemStack stack = new ItemStack(ModItems.SMALL_LIFT.get());
        InteractionResult first =
                stack.getItem().useOn(context(h, player, stack, clicked, Direction.UP));
        h.assertTrue(first.consumesAction(), "Creative placement failed");
        h.assertTrue(stack.getCount() == 1, "Creative item consumed");
        InteractionResult second =
                stack.getItem().useOn(context(h, player, stack, clicked, Direction.UP));
        h.assertTrue(second == InteractionResult.FAIL, "Overlapping lift placement was accepted");
        h.assertTrue(
                h.getLevel().getEntitiesOfClass(AbstractLift.class, arena(h)).size() == 1,
                "Overlapping lift duplicated");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void placementWithoutGuidesFailsWithoutConsumption(GameTestHelper h) {
        BlockPos clicked = h.absolutePos(CENTER.below());
        h.getLevel().setBlockAndUpdate(clicked, Blocks.STONE.defaultBlockState());
        ItemStack stack = new ItemStack(ModItems.SMALL_LIFT.get());
        InteractionResult result =
                stack.getItem().useOn(context(h, null, stack, clicked, Direction.UP));
        h.assertTrue(result == InteractionResult.FAIL, "Placement without rails accepted");
        h.assertTrue(stack.getCount() == 1, "Failed placement consumed lift");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void woodenStationStopsAtItsOwnIntegerFloor(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 4);
        rails(h, e, 3, 12);
        BlockPos target = tracks(e).getFirst().above(3);
        station(h, e, 7, Direction.UP, false);
        e.setMovementType(MovementType.UP);
        h.runAfterDelay(
                30,
                () -> {
                    near(h, e.getY(), target.getY(), 0.0001, "Powered station floor");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Powered station did not stop lift");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void motorPowerPropagatesAtMostSevenRows(GameTestHelper h) {
        BlockPos bottom = h.absolutePos(new BlockPos(5, 3, 5));
        for (int y = 0; y < 10; y++)
            h.getLevel()
                    .setBlockAndUpdate(
                            bottom.above(y),
                            ModBlocks.POWERED_LIFT_TRACK
                                    .get()
                                    .defaultBlockState()
                                    .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        h.getLevel().setBlockAndUpdate(bottom.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(
                12,
                () -> {
                    h.assertTrue(
                            h.getLevel()
                                    .getBlockState(bottom)
                                    .getValue(PoweredLiftTrackBlock.POWERED),
                            "Local track not powered");
                    h.assertTrue(
                            h.getLevel()
                                    .getBlockState(bottom.above(7))
                                    .getValue(PoweredLiftTrackBlock.POWERED),
                            "Power did not reach seventh row");
                    h.assertFalse(
                            h.getLevel()
                                    .getBlockState(bottom.above(8))
                                    .getValue(PoweredLiftTrackBlock.POWERED),
                            "Power propagated beyond seventh row");
                    h.succeed();
                });
    }

    private static void station(
            GameTestHelper h, AbstractLift lift, int relativeY, Direction command, boolean power) {
        int y = h.absolutePos(BlockPos.ZERO).getY() + relativeY;
        for (BlockPos p : tracks(lift)) {
            p = p.atY(y);

            if (!power)
                h.getLevel()
                        .setBlockAndUpdate(
                                p.relative(inward(lift, p).getOpposite()),
                                Blocks.AIR.defaultBlockState());
            h.getLevel()
                    .setBlockAndUpdate(
                            p,
                            ModBlocks.DIRECTIONAL_LIFT_TRACK
                                    .get()
                                    .defaultBlockState()
                                    .setValue(HorizontalDirectionalBlock.FACING, inward(lift, p))
                                    .setValue(
                                            DirectionalLiftTrackBlock.VERTICAL_DIRECTION, command));
            h.getLevel()
                    .setBlockAndUpdate(
                            p.relative(inward(lift, p).getOpposite()),
                            (power ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState());
        }
    }

    private static void startByStation(GameTestHelper h, Direction direction) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        rails(h, e, 2, 14);
        double start = e.getY();
        station(h, e, 6, direction, false);
        h.runAfterDelay(
                5,
                () -> {
                    near(h, e.getY(), start, 0.0001, "Unpowered station did not hold lift");
                    station(h, e, 6, direction, true);
                });
        h.runAfterDelay(
                15,
                () -> {
                    h.assertTrue(
                            (e.getY() - start) * (direction == Direction.UP ? 1 : -1) > 1,
                            "Powered " + direction + " station did not start lift");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void poweredUpStationStartsLift(GameTestHelper h) {
        startByStation(h, Direction.UP);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void poweredDownStationStartsLift(GameTestHelper h) {
        startByStation(h, Direction.DOWN);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void oppositePoweredStationArrowsHoldLiftStill(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        rails(h, e, 2, 14);
        double start = e.getY();
        station(h, e, 6, Direction.UP, true);
        BlockPos p = tracks(e).getFirst();
        h.getLevel()
                .setBlockAndUpdate(
                        p,
                        h.getLevel()
                                .getBlockState(p)
                                .setValue(
                                        DirectionalLiftTrackBlock.VERTICAL_DIRECTION,
                                        Direction.DOWN));
        h.runAfterDelay(
                8,
                () -> {
                    near(h, e.getY(), start, 0.0001, "Conflicting station arrows moved lift");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Conflicting station arrows did not stop movement");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void unpoweredMotorDoesNotMoveLift(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        rails(h, e, 2, 14, false);
        double start = e.getY();
        e.setMovementType(MovementType.UP);
        h.runAfterDelay(
                8,
                () -> {
                    near(h, e.getY(), start, 0.0001, "Unpowered motor moved lift");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void motorPowerDoesNotCrossWrongFacingGuide(GameTestHelper h) {
        BlockPos bottom = h.absolutePos(new BlockPos(5, 3, 5));
        for (int y = 0; y < 7; y++)
            h.getLevel()
                    .setBlockAndUpdate(
                            bottom.above(y),
                            ModBlocks.POWERED_LIFT_TRACK
                                    .get()
                                    .defaultBlockState()
                                    .setValue(
                                            HorizontalDirectionalBlock.FACING,
                                            y == 3 ? Direction.WEST : Direction.EAST));
        h.getLevel().setBlockAndUpdate(bottom.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(
                10,
                () -> {
                    h.assertFalse(
                            h.getLevel()
                                    .getBlockState(bottom.above(4))
                                    .getValue(PoweredLiftTrackBlock.POWERED),
                            "Motor power crossed opposite facing rail");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void motorResumesAfterRedstoneReturns(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        List<BlockPos> all = rails(h, e, 2, 14);
        e.setMovementType(MovementType.UP);
        double[] paused = {0};
        h.runAfterDelay(
                4,
                () -> {
                    for (BlockPos p : all)
                        h.getLevel()
                                .setBlockAndUpdate(
                                        p.relative(inward(e, p).getOpposite()),
                                        Blocks.AIR.defaultBlockState());
                });
        h.runAfterDelay(
                16,
                () -> {
                    paused[0] = e.getY();
                    near(h, e.getDeltaMovement().y, 0, 0.0001, "Unpowered motor retained velocity");
                });
        h.runAfterDelay(
                20,
                () -> {
                    near(h, e.getY(), paused[0], 0.0001, "Unpowered motor drifted");
                    for (BlockPos p : all)
                        h.getLevel()
                                .setBlockAndUpdate(
                                        p.relative(inward(e, p).getOpposite()),
                                        Blocks.REDSTONE_BLOCK.defaultBlockState());
                });
        h.runAfterDelay(
                32,
                () -> {
                    h.assertTrue(
                            e.getY() > paused[0] + 1.0,
                            "Motor did not resume after power returned");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 110)
    public static void observerPulsesOnceOnArrivalAndOnceOnDeparture(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 4);
        rails(h, e, 3, 8);
        BlockPos target = tracks(e).getFirst().atY(h.absolutePos(BlockPos.ZERO).getY() + 8);
        Direction outward = inward(e, target).getOpposite();
        BlockPos observer = target.relative(outward);
        h.getLevel()
                .setBlockAndUpdate(
                        observer,
                        Blocks.OBSERVER
                                .defaultBlockState()
                                .setValue(ObserverBlock.FACING, outward.getOpposite()));
        int[] pulses = {0};
        boolean[] previous = {false};
        for (int tick = 10; tick <= 85; tick++) {
            h.runAfterDelay(
                    tick,
                    () -> {
                        boolean powered =
                                h.getLevel()
                                        .getBlockState(observer)
                                        .getValue(ObserverBlock.POWERED);
                        if (powered && !previous[0]) pulses[0]++;
                        previous[0] = powered;
                    });
        }
        h.runAfterDelay(
                10,
                () -> {
                    pulses[0] = 0;
                    previous[0] =
                            h.getLevel().getBlockState(observer).getValue(ObserverBlock.POWERED);
                    e.setMovementType(MovementType.UP);
                });
        h.runAfterDelay(
                53,
                () -> {
                    near(h, e.getY(), target.getY(), 0.0001, "Arrival floor");
                    h.assertTrue(
                            h.getLevel()
                                    .getBlockState(target)
                                    .getValue(LiftTrackBlock.LIFT_PRESENT),
                            "Guide did not publish stopped lift presence");
                    h.assertTrue(
                            pulses[0] == 1,
                            "Observer did not emit one arrival pulse: " + pulses[0]);
                });
        h.runAfterDelay(
                60,
                () -> {
                    h.assertTrue(pulses[0] == 1, "Stationary lift repeatedly pulsed observer");
                    e.setMovementType(MovementType.DOWN);
                });
        h.runAfterDelay(
                72,
                () -> {
                    h.assertFalse(
                            h.getLevel()
                                    .getBlockState(target)
                                    .getValue(LiftTrackBlock.LIFT_PRESENT),
                            "Guide presence did not clear after departure");
                    h.assertTrue(
                            pulses[0] == 2,
                            "Observer did not emit exactly one departure pulse: " + pulses[0]);
                });
        h.runAfterDelay(
                85,
                () -> {
                    h.assertTrue(
                            pulses[0] == 2,
                            "Observer emitted per-tick movement pulses: " + pulses[0]);
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void observerDetectsManualStopWithoutRepeatedPulses(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 4);
        rails(h, e, 3, 13);
        BlockPos target = tracks(e).getFirst().atY(h.absolutePos(BlockPos.ZERO).getY() + 6);
        Direction outward = inward(e, target).getOpposite();
        BlockPos observer = target.relative(outward);
        h.getLevel()
                .setBlockAndUpdate(
                        observer,
                        Blocks.OBSERVER
                                .defaultBlockState()
                                .setValue(ObserverBlock.FACING, outward.getOpposite()));
        int[] pulses = {0};
        boolean[] previous = {false}, stopped = {false};
        h.runAfterDelay(
                10,
                () -> {
                    pulses[0] = 0;
                    previous[0] = false;
                    e.setMovementType(MovementType.UP);
                });
        for (int tick = 11; tick <= 65; tick++) {
            h.runAfterDelay(
                    tick,
                    () -> {
                        if (!stopped[0] && Math.abs(e.getY() - target.getY()) < 0.0001) {
                            e.setMovementType(MovementType.STATIONARY);
                            stopped[0] = true;
                        }
                        boolean powered =
                                h.getLevel()
                                        .getBlockState(observer)
                                        .getValue(ObserverBlock.POWERED);
                        if (powered && !previous[0]) pulses[0]++;
                        previous[0] = powered;
                    });
        }
        h.runAfterDelay(
                65,
                () -> {
                    h.assertTrue(stopped[0], "Test never reached manual stop floor");
                    h.assertTrue(
                            h.getLevel()
                                    .getBlockState(target)
                                    .getValue(LiftTrackBlock.LIFT_PRESENT),
                            "Manual stop did not publish presence");
                    h.assertTrue(
                            pulses[0] == 1,
                            "Manual stop did not emit exactly one observer pulse: " + pulses[0]);
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void manualStopOverridesContinuouslyPoweredStation(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        rails(h, e, 2, 14);
        station(h, e, 6, Direction.UP, true);
        double start = e.getY();
        e.setMovementType(MovementType.UP);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPos(e.getX() + 8, e.getY(), e.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        e.interact(player, InteractionHand.MAIN_HAND);
        h.runAfterDelay(
                8,
                () -> {
                    near(h, e.getY(), start, 0.0001, "Active arrow undid manual stop");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Manual stop was not retained");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void manualReverseOverridesPoweredStationArrow(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        rails(h, e, 2, 14);
        station(h, e, 6, Direction.UP, true);
        double start = e.getY();
        e.setMovementType(MovementType.UP);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPos(e.getX() + 8, e.getY(), e.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        e.interact(player, InteractionHand.MAIN_HAND);
        h.runAfterDelay(
                8,
                () -> {
                    h.assertTrue(
                            e.getY() < start - 0.9,
                            "Active up arrow undid manual downward reversal");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void manualClickCanLeaveUnpoweredStation(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        rails(h, e, 2, 14);
        station(h, e, 6, Direction.UP, false);
        double start = e.getY();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPos(e.getX() + 8, e.getY(), e.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        e.interact(player, InteractionHand.MAIN_HAND);
        h.runAfterDelay(
                8,
                () -> {
                    h.assertTrue(
                            e.getY() > start + 0.9,
                            "Manual start could not leave unpowered station");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void descendingDeckCannotCrushCreatureAgainstFloor(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 7);
        Sheep s = sheep(h, e, 0);
        BlockPos floor = h.absolutePos(new BlockPos(12, 4, 12));
        h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        s.setPos(e.getX(), floor.getY() + 1, e.getZ());
        s.setOnGround(true);
        e.setPos(e.getX(), s.getBoundingBox().maxY + 0.05, e.getZ());
        double allowed = LiftCollision.limitMovementForRiders(e, e.getBoundingBox(), -0.15);
        near(h, allowed, -0.05, 0.003, "Descending deck ignores creature floor collision");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void liftRecipesLoadWithExpectedOutputs(GameTestHelper h) {
        String[] ids = {
            "small_lift",
            "medium_lift",
            "large_lift",
            "directional_lift_track",
            "powered_lift_track",
            "basic_lift_track",
            "detector_lift_track"
        };
        int[] counts = {1, 1, 1, 1, 8, 8, 8};
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            var recipe =
                    h.getLevel()
                            .getRecipeManager()
                            .byKey(ResourceLocation.fromNamespaceAndPath("spelunkers_charm", id));
            h.assertTrue(recipe.isPresent(), "Missing loaded recipe " + id);
            ItemStack result =
                    recipe.orElseThrow().value().getResultItem(h.getLevel().registryAccess());
            h.assertTrue(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(result.getItem())
                            .equals(ResourceLocation.fromNamespaceAndPath("spelunkers_charm", id)),
                    "Wrong recipe output for " + id);
            h.assertTrue(
                    result.getCount() == counts[i],
                    "Wrong recipe count for " + id + ": " + result.getCount());
        }
        h.succeed();
    }

    private static void hybrid(GameTestHelper h, AbstractLift lift, int min, int max, int motors) {
        int base = h.absolutePos(BlockPos.ZERO).getY(), column = 0;
        for (BlockPos guide : tracks(lift)) {
            boolean motor = column++ < motors;
            for (int y = min; y <= max; y++) {
                BlockPos pos = guide.atY(base + y);
                Direction face = inward(lift, pos);
                h.getLevel()
                        .setBlockAndUpdate(
                                pos,
                                (motor
                                                ? ModBlocks.POWERED_LIFT_TRACK.get()
                                                : ModBlocks.BASIC_LIFT_TRACK.get())
                                        .defaultBlockState()
                                        .setValue(HorizontalDirectionalBlock.FACING, face));
                h.getLevel()
                        .setBlockAndUpdate(
                                pos.relative(face.getOpposite()),
                                (motor ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState());
            }
        }
    }

    private static void threshold(
            GameTestHelper h, AbstractLift.Type type, boolean enough, boolean up) {
        AbstractLift e = lift(h, type, Direction.NORTH, up ? 4 : 9);
        int required = tracks(e).size() / 2;
        int motors = up ? (enough ? required : required - 1) : 0;
        hybrid(h, e, 2, 14, motors);
        double start = e.getY();
        e.setMovementType(up ? MovementType.UP : MovementType.DOWN);
        h.runAfterDelay(
                15,
                () -> {
                    if (up && !enough)
                        near(h, e.getY(), start, 0.0001, "Insufficient motors raised " + type);
                    else
                        h.assertTrue(
                                (e.getY() - start) * (up ? 1 : -1) > 1.5,
                                "Wrong 50%/passive descent rule for " + type);
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void smallOneMotorRaises(GameTestHelper h) {
        threshold(h, AbstractLift.Type.SMALL, true, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void smallZeroMotorsCannotRaise(GameTestHelper h) {
        threshold(h, AbstractLift.Type.SMALL, false, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mediumTwoMotorsRaise(GameTestHelper h) {
        threshold(h, AbstractLift.Type.MEDIUM, true, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mediumOneMotorCannotRaise(GameTestHelper h) {
        threshold(h, AbstractLift.Type.MEDIUM, false, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void largeFourMotorsRaise(GameTestHelper h) {
        threshold(h, AbstractLift.Type.LARGE, true, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void largeThreeMotorsCannotRaise(GameTestHelper h) {
        threshold(h, AbstractLift.Type.LARGE, false, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void smallDescendsOnPassiveRacks(GameTestHelper h) {
        threshold(h, AbstractLift.Type.SMALL, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mediumDescendsOnPassiveRacks(GameTestHelper h) {
        threshold(h, AbstractLift.Type.MEDIUM, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void largeDescendsOnPassiveRacks(GameTestHelper h) {
        threshold(h, AbstractLift.Type.LARGE, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void detectorActivatesWhilePassingAndPowersLamp(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        hybrid(h, e, 2, 14, 1);
        BlockPos detector = tracks(e).getLast().atY(h.absolutePos(BlockPos.ZERO).getY() + 7);
        Direction face = inward(e, detector);
        BlockPos lamp = detector.relative(face.getOpposite());
        h.getLevel()
                .setBlockAndUpdate(
                        detector,
                        ModBlocks.DETECTOR_LIFT_TRACK
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, face));
        h.getLevel().setBlockAndUpdate(lamp, Blocks.REDSTONE_LAMP.defaultBlockState());
        boolean[] seen = {false, false};
        e.setMovementType(MovementType.UP);
        for (int tick = 1; tick <= 42; tick++)
            h.runAfterDelay(
                    tick,
                    () -> {
                        if (h.getLevel().getBlockState(detector).getValue(LiftTrackBlock.POWERED)) {
                            seen[0] = true;
                            if (h.getLevel()
                                    .getBlockState(lamp)
                                    .getValue(
                                            net.minecraft.world.level.block.RedstoneLampBlock.LIT))
                                seen[1] = true;
                        }
                    });
        h.runAfterDelay(
                48,
                () -> {
                    h.assertTrue(seen[0] && seen[1], "Passing lift did not operate detector/lamp");
                    h.assertFalse(
                            h.getLevel().getBlockState(detector).getValue(LiftTrackBlock.POWERED),
                            "Detector stayed pressed after passage");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void detectorRemainsPressedWhileParkedAndReleasesOnRemoval(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        hybrid(h, e, 2, 14, 0);
        BlockPos detector = tracks(e).getLast();
        h.getLevel()
                .setBlockAndUpdate(
                        detector,
                        ModBlocks.DETECTOR_LIFT_TRACK
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, inward(e, detector)));
        for (int tick = 2; tick <= 22; tick++)
            h.runAfterDelay(
                    tick,
                    () ->
                            h.assertTrue(
                                    h.getLevel()
                                            .getBlockState(detector)
                                            .getValue(LiftTrackBlock.POWERED),
                                    "Parked lift detector flickered off"));
        h.runAfterDelay(25, e::discard);
        h.runAfterDelay(
                32,
                () -> {
                    h.assertFalse(
                            h.getLevel().getBlockState(detector).getValue(LiftTrackBlock.POWERED),
                            "Removed lift did not release detector");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void pressedDetectorDoesNotCountAsMotor(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        hybrid(h, e, 2, 14, 0);
        double start = e.getY();
        BlockPos detector = tracks(e).getLast();
        h.getLevel()
                .setBlockAndUpdate(
                        detector,
                        ModBlocks.DETECTOR_LIFT_TRACK
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, inward(e, detector)));
        e.setMovementType(MovementType.UP);
        h.runAfterDelay(
                10,
                () -> {
                    near(h, e.getY(), start, 0.0001, "Detector self-powered lift");
                    h.assertTrue(
                            h.getLevel().getBlockState(detector).getValue(LiftTrackBlock.POWERED),
                            "Detector was not pressed");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void detectorRecipeUsesExactlyOneDetectorRail(GameTestHelper h) {
        var recipe =
                h.getLevel()
                        .getRecipeManager()
                        .byKey(
                                ResourceLocation.fromNamespaceAndPath(
                                        "spelunkers_charm", "detector_lift_track"))
                        .orElseThrow()
                        .value();
        long amount =
                recipe.getIngredients().stream()
                        .filter(ingredient -> ingredient.test(new ItemStack(Items.DETECTOR_RAIL)))
                        .count();
        h.assertTrue(
                amount == 1,
                "Detector recipe must contain exactly one detector rail, got " + amount);
        h.succeed();
    }

    private static void partialStation(
            GameTestHelper h, AbstractLift.Type type, boolean ascending, boolean dispatch) {
        AbstractLift e = lift(h, type, Direction.NORTH, ascending ? 4 : 10);
        hybrid(h, e, 2, 14, tracks(e).size() / 2);
        BlockPos marker = tracks(e).getFirst().atY(h.absolutePos(BlockPos.ZERO).getY() + 7);
        Direction face = inward(e, marker), command = ascending ? Direction.UP : Direction.DOWN;
        for (int offset = -1; offset <= 1; offset++)
            h.getLevel()
                    .setBlockAndUpdate(
                            marker.offset(0, offset, 0).relative(face.getOpposite()),
                            Blocks.AIR.defaultBlockState());
        h.getLevel()
                .setBlockAndUpdate(
                        marker,
                        ModBlocks.DIRECTIONAL_LIFT_TRACK
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, face)
                                .setValue(DirectionalLiftTrackBlock.VERTICAL_DIRECTION, command));
        e.setMovementType(ascending ? MovementType.UP : MovementType.DOWN);
        h.runAfterDelay(
                33,
                () -> {
                    near(
                            h,
                            e.getY(),
                            marker.getY(),
                            .0001,
                            "Partial station stopped one row too early: " + type + " " + command);
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Partial directional station did not hold lift");
                    h.assertTrue(
                            e.isStationAtY(marker.getY()),
                            "Single directional track not recognized as station");
                    if (dispatch)
                        h.getLevel()
                                .setBlockAndUpdate(
                                        marker.relative(face.getOpposite()),
                                        Blocks.REDSTONE_BLOCK.defaultBlockState());
                    else h.succeed();
                });
        if (dispatch)
            h.runAfterDelay(
                    48,
                    () -> {
                        h.assertTrue(
                                (e.getY() - marker.getY()) * (ascending ? 1 : -1) > 1.4,
                                "Single directional track could not dispatch lift " + command);
                        h.succeed();
                    });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void singleMarkerSmallUpArrives(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.SMALL, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void singleMarkerSmallDownArrives(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.SMALL, false, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void singleMarkerMediumUpArrives(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.MEDIUM, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void singleMarkerMediumDownArrives(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.MEDIUM, false, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void singleMarkerLargeUpArrives(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.LARGE, true, false);
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void singleMarkerLargeDownArrives(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.LARGE, false, false);
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void singleMarkerDispatchesUpWithOtherPoweredTracks(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.MEDIUM, true, true);
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void singleMarkerDispatchesDownWithOtherPoweredTracks(GameTestHelper h) {
        partialStation(h, AbstractLift.Type.MEDIUM, false, true);
    }

    @GameTest(template = "empty")
    public static void basicRecipeUsesLogsIronCornersAndSticks(GameTestHelper h) {
        var recipe =
                h.getLevel()
                        .getRecipeManager()
                        .byKey(
                                ResourceLocation.fromNamespaceAndPath(
                                        "spelunkers_charm", "basic_lift_track"))
                        .orElseThrow()
                        .value();
        for (Item log :
                new Item[] {
                    Items.OAK_LOG,
                    Items.SPRUCE_LOG,
                    Items.CHERRY_LOG,
                    Items.CRIMSON_STEM,
                    Items.WARPED_STEM,
                    Items.STRIPPED_BIRCH_LOG
                }) {
            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < 9; i++)
                grid.add(
                        new ItemStack(
                                i == 4
                                        ? log
                                        : (i == 0 || i == 2 || i == 6 || i == 8)
                                                ? Items.IRON_INGOT
                                                : Items.STICK));
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
            @SuppressWarnings("unchecked")
            var shaped =
                    (net.minecraft.world.item.crafting.Recipe<
                                    net.minecraft.world.item.crafting.CraftingInput>)
                            recipe;
            h.assertTrue(
                    shaped.matches(input, h.getLevel()), "Basic rack recipe rejected log " + log);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void stationPulseEndsUnderConstantInputAndRearmsAfterPowerCycle(
            GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5, 5, 5)), power = p.west();
        h.getLevel()
                .setBlockAndUpdate(
                        p,
                        ModBlocks.DIRECTIONAL_LIFT_TRACK
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        h.getLevel().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.assertTrue(
                h.getLevel().getBlockState(p).getValue(LiftTrackBlock.POWERED),
                "Rising input did not activate station pulse");
        for (int tick = 4; tick <= 18; tick++)
            h.runAfterDelay(
                    tick,
                    () -> {
                        var state = h.getLevel().getBlockState(p);
                        h.assertFalse(
                                state.getValue(LiftTrackBlock.POWERED),
                                "Station stayed active/repeated under constant input");
                        h.assertTrue(
                                state.getValue(DirectionalLiftTrackBlock.SIGNAL_HELD),
                                "Constant input latch was not held");
                        h.getLevel().updateNeighborsAt(power, Blocks.REDSTONE_BLOCK);
                    });
        h.runAfterDelay(
                21, () -> h.getLevel().setBlockAndUpdate(power, Blocks.AIR.defaultBlockState()));
        h.runAfterDelay(
                24,
                () -> {
                    h.assertFalse(
                            h.getLevel()
                                    .getBlockState(p)
                                    .getValue(DirectionalLiftTrackBlock.SIGNAL_HELD),
                            "Station did not rearm on low input");
                    h.getLevel()
                            .setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
                    h.assertTrue(
                            h.getLevel().getBlockState(p).getValue(LiftTrackBlock.POWERED),
                            "Second rising edge did not pulse");
                });
        h.runAfterDelay(
                30,
                () -> {
                    h.assertFalse(
                            h.getLevel().getBlockState(p).getValue(LiftTrackBlock.POWERED),
                            "Second pulse did not end");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 90)
    public static void oldHighSignalDoesNotDispatchNewlyArrivingLift(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        hybrid(h, e, 2, 14, 1);
        BlockPos p = tracks(e).getFirst().atY(h.absolutePos(BlockPos.ZERO).getY() + 7);
        Direction face = inward(e, p);
        for (int dy = -1; dy <= 1; dy++)
            h.getLevel()
                    .setBlockAndUpdate(
                            p.above(dy).relative(face.getOpposite()),
                            Blocks.AIR.defaultBlockState());
        h.getLevel()
                .setBlockAndUpdate(
                        p,
                        ModBlocks.DIRECTIONAL_LIFT_TRACK
                                .get()
                                .defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, face));
        h.getLevel()
                .setBlockAndUpdate(
                        p.relative(face.getOpposite()), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(5, () -> e.setMovementType(MovementType.UP));
        h.runAfterDelay(
                17,
                () -> {
                    h.assertTrue(
                            h.getLevel().getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                            "Test missed station display window");
                    near(h, e.getY(), p.getY(), .0001, "Visual afterglow dispatched arriving lift");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Afterglow did not hold arriving lift");
                });
        h.runAfterDelay(
                40,
                () -> {
                    near(h, e.getY(), p.getY(), .0001, "Old pulse dispatched an arriving lift");
                    h.assertTrue(
                            e.getMovementType() == MovementType.STATIONARY,
                            "Arriving lift did not await a new impulse");
                    h.getLevel()
                            .setBlockAndUpdate(
                                    p.relative(face.getOpposite()), Blocks.AIR.defaultBlockState());
                });
        h.runAfterDelay(
                44,
                () ->
                        h.getLevel()
                                .setBlockAndUpdate(
                                        p.relative(face.getOpposite()),
                                        Blocks.REDSTONE_BLOCK.defaultBlockState()));
        h.runAfterDelay(
                58,
                () -> {
                    h.assertTrue(
                            e.getY() > p.getY() + 1.3,
                            "Fresh pulse failed to dispatch stopped lift");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 55)
    public static void savedLatchDoesNotPulseAgainWhenReloadedHigh(GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5, 5, 5));
        h.getLevel().setBlockAndUpdate(p.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var saved =
                ModBlocks.DIRECTIONAL_LIFT_TRACK
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                        .setValue(DirectionalLiftTrackBlock.SIGNAL_HELD, true)
                        .setValue(LiftTrackBlock.POWERED, false);
        h.getLevel().setBlockAndUpdate(p, saved);
        h.getLevel().scheduleTick(p, saved.getBlock(), 1);
        h.runAfterDelay(
                7,
                () -> {
                    h.assertFalse(
                            h.getLevel().getBlockState(p).getValue(LiftTrackBlock.POWERED),
                            "Reloaded high input created a duplicate pulse");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void canonicalIdsAndLegacySavedDataResolveToSameBlocks(GameTestHelper h) {
        var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        String[][] ids = {
            {"wooden_lift_track", "directional_lift_track"}, {"lift_track", "basic_lift_track"}
        };
        for (String[] pair : ids) {
            var oldId = ResourceLocation.fromNamespaceAndPath("spelunkers_charm", pair[0]);
            var newId = ResourceLocation.fromNamespaceAndPath("spelunkers_charm", pair[1]);
            h.assertTrue(
                    blocks.get(oldId) == blocks.get(newId), "Legacy block alias failed: " + oldId);
            h.assertTrue(
                    items.get(oldId) == items.get(newId), "Legacy item alias failed: " + oldId);
            h.assertTrue(
                    blocks.getKey(blocks.get(newId)).equals(newId),
                    "Canonical block key is incorrect");
            h.assertTrue(
                    items.getKey(items.get(newId)).equals(newId),
                    "Canonical item key is incorrect");
            h.assertFalse(
                    blocks.keySet().contains(oldId),
                    "Legacy name remains an actual registered block");
            CompoundTag stateTag = new CompoundTag();
            stateTag.putString("Name", oldId.toString());
            CompoundTag properties = new CompoundTag();
            properties.putString("facing", "east");
            properties.putString("lift_present", "true");
            stateTag.put("Properties", properties);
            var decoded =
                    net.minecraft.world.level.block.state.BlockState.CODEC
                            .parse(net.minecraft.nbt.NbtOps.INSTANCE, stateTag)
                            .getOrThrow();
            h.assertTrue(
                    decoded.is(blocks.get(newId))
                            && decoded.getValue(HorizontalDirectionalBlock.FACING)
                                    == Direction.EAST,
                    "Old saved block state was not remapped");
            h.assertTrue(
                    decoded.getValue(LiftTrackBlock.LIFT_PRESENT),
                    "Saved block property lost during remap");
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("id", oldId.toString());
            itemTag.putInt("count", 3);
            ItemStack stack = ItemStack.parseOptional(h.getLevel().registryAccess(), itemTag);
            h.assertTrue(
                    stack.is(items.get(newId)) && stack.getCount() == 3,
                    "Old saved item stack failed to remap");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void briefInputStillGeneratesOneCompletePulse(GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5, 5, 5));
        h.getLevel()
                .setBlockAndUpdate(p, ModBlocks.DIRECTIONAL_LIFT_TRACK.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.east(), Blocks.AIR.defaultBlockState());
        h.assertTrue(
                h.getLevel().getBlockState(p).getValue(LiftTrackBlock.POWERED),
                "Brief input pulse was lost");
        h.runAfterDelay(
                4,
                () -> {
                    h.assertFalse(
                            h.getLevel().getBlockState(p).getValue(LiftTrackBlock.POWERED),
                            "Brief pulse did not end");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void newPulseCanDispatchAfterManualStop(GameTestHelper h) {
        AbstractLift e = lift(h, AbstractLift.Type.SMALL, Direction.NORTH, 6);
        rails(h, e, 2, 14);
        station(h, e, 6, Direction.UP, false);
        double start = e.getY();
        BlockPos p = tracks(e).getFirst(), power = p.relative(inward(e, p).getOpposite());
        h.getLevel().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        e.setMovementType(MovementType.UP);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setPos(e.getX() + 8, e.getY(), e.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        e.interact(player, InteractionHand.MAIN_HAND);
        h.runAfterDelay(
                12,
                () -> {
                    near(h, e.getY(), start, .0001, "Manual stop was overridden by same input");
                    h.getLevel().setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
                });
        h.runAfterDelay(
                16,
                () ->
                        h.getLevel()
                                .setBlockAndUpdate(
                                        power, Blocks.REDSTONE_BLOCK.defaultBlockState()));
        h.runAfterDelay(
                30,
                () -> {
                    h.assertTrue(
                            e.getY() > start + 1.3, "New pulse could not rearm after manual stop");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 35)
    public static void legacyPoweredStateIsReconciledAfterChunkLoad(GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5, 5, 5));
        var level = h.getLevel();
        level.setBlockAndUpdate(p.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        var chunk = level.getChunkAt(p);
        var old =
                ModBlocks.DIRECTIONAL_LIFT_TRACK
                        .get()
                        .defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                        .setValue(LiftTrackBlock.POWERED, true)
                        .setValue(DirectionalLiftTrackBlock.SIGNAL_HELD, false);
        chunk.getSection(chunk.getSectionIndex(p.getY()))
                .setBlockState(p.getX() & 15, p.getY() & 15, p.getZ() & 15, old);
        net.luckystudio.spelunkers_charm.block.custom.LiftTrackLoadEvents.loaded(
                new net.neoforged.neoforge.event.level.ChunkEvent.Load(chunk, false));
        h.runAfterDelay(
                7,
                () -> {
                    var state = level.getBlockState(p);
                    h.assertFalse(
                            state.getValue(LiftTrackBlock.POWERED),
                            "Legacy high-powered block stayed active after loading");
                    h.assertTrue(
                            state.getValue(DirectionalLiftTrackBlock.SIGNAL_HELD),
                            "Loaded station lost the high-input latch");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 45)
    public static void stationGlowsForOneSecondWithoutExtendingCommand(GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5, 5, 5));
        var level = h.getLevel();
        level.setBlockAndUpdate(p, ModBlocks.DIRECTIONAL_LIFT_TRACK.get().defaultBlockState());
        level.setBlockAndUpdate(p.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        h.runAfterDelay(
                4,
                () -> {
                    h.assertFalse(
                            level.getBlockState(p).getValue(LiftTrackBlock.POWERED),
                            "Display extended command pulse");
                    h.assertTrue(
                            level.getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                            "Display ended with command");
                });
        h.runAfterDelay(
                18,
                () ->
                        h.assertTrue(
                                level.getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                                "Display ended before 20 ticks"));
        h.runAfterDelay(
                22,
                () -> {
                    h.assertFalse(
                            level.getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                            "Constant power kept display lit");
                    h.assertFalse(
                            level.getBlockState(p).getValue(LiftTrackBlock.POWERED),
                            "Constant power repeated command");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void freshPulseDuringAfterglowRestartsDisplayAndEndsCommandPromptly(
            GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5, 5, 5)), power = p.east();
        var level = h.getLevel();
        level.setBlockAndUpdate(p, ModBlocks.DIRECTIONAL_LIFT_TRACK.get().defaultBlockState());
        level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(power, Blocks.AIR.defaultBlockState());
        h.runAfterDelay(
                8,
                () -> {
                    h.assertTrue(
                            level.getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                            "Brief input lost afterglow");
                    level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
                    h.assertTrue(
                            level.getBlockState(p).getValue(LiftTrackBlock.POWERED),
                            "Afterglow blocked fresh command");
                });
        h.runAfterDelay(
                12,
                () ->
                        h.assertFalse(
                                level.getBlockState(p).getValue(LiftTrackBlock.POWERED),
                                "Old display tick delayed command expiry"));
        h.runAfterDelay(
                24,
                () ->
                        h.assertTrue(
                                level.getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                                "Previous expiry cut off refreshed display"));
        h.runAfterDelay(
                30,
                () -> {
                    h.assertFalse(
                            level.getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                            "Refreshed display never ended");
                    h.succeed();
                });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void displayExpiryDoesNotSendExtraObserverPulse(GameTestHelper h) {
        BlockPos p = h.absolutePos(new BlockPos(5, 5, 5)), observer = p.north(), power = p.east();
        var level = h.getLevel();
        level.setBlockAndUpdate(p, ModBlocks.DIRECTIONAL_LIFT_TRACK.get().defaultBlockState());
        level.setBlockAndUpdate(
                observer,
                Blocks.OBSERVER
                        .defaultBlockState()
                        .setValue(ObserverBlock.FACING, Direction.SOUTH));
        h.runAfterDelay(
                10,
                () -> level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()));
        for (int tick = 24; tick <= 46; tick++)
            h.runAfterDelay(
                    tick,
                    () ->
                            h.assertFalse(
                                    level.getBlockState(observer).getValue(ObserverBlock.POWERED),
                                    "Visual-only expiry pulsed observer"));
        h.runAfterDelay(
                47,
                () -> {
                    h.assertFalse(
                            level.getBlockState(p).getValue(DirectionalLiftTrackBlock.LIT),
                            "Display did not expire");
                    h.succeed();
                });
    }
}
