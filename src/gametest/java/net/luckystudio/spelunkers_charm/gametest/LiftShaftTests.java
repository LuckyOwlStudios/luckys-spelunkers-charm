package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class LiftShaftTests {
    private static AbstractLift platform(
            GameTestHelper helper, AbstractLift.Type type, int x, int y) {
        BlockPos center = helper.absolutePos(new BlockPos(x, y, 12));
        AbstractLift lift =
                AbstractLift.createLift(
                        helper.getLevel(),
                        center.getX() + 0.5,
                        center.getY(),
                        center.getZ() + 0.5,
                        type,
                        ItemStack.EMPTY,
                        null);
        lift.setYRot(Direction.NORTH.toYRot());
        return lift;
    }

    private static void guides(GameTestHelper helper, AbstractLift lift, int bottom, int top) {
        int base = helper.absolutePos(BlockPos.ZERO).getY();
        for (BlockPos guide : lift.getGuidePositions()) {
            Direction facing = inward(lift, guide);
            for (int y = bottom; y <= top; ++y) {
                helper.getLevel()
                        .setBlockAndUpdate(
                                guide.atY(base + y),
                                ModBlocks.BASIC_LIFT_TRACK
                                        .get()
                                        .defaultBlockState()
                                        .setValue(HorizontalDirectionalBlock.FACING, facing));
            }
        }
    }

    private static Direction inward(AbstractLift lift, BlockPos guide) {
        double dx = lift.getX() - guide.getX() - 0.5, dz = lift.getZ() - guide.getZ() - 0.5;
        return Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private static InteractionResult place(
            GameTestHelper helper, AbstractLift candidate, ItemStack stack) {
        var player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setPos(candidate.getX() + 8, candidate.getY(), candidate.getZ() + 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos guide = candidate.getGuidePositions().getFirst();
        return stack.getItem()
                .useOn(
                        new UseOnContext(
                                player,
                                InteractionHand.MAIN_HAND,
                                new BlockHitResult(
                                        Vec3.atCenterOf(guide),
                                        inward(candidate, guide),
                                        guide,
                                        false)));
    }

    private static Item item(AbstractLift.Type type) {
        return switch (type) {
            case SMALL -> ModItems.SMALL_LIFT.get();
            case MEDIUM -> ModItems.MEDIUM_LIFT.get();
            case LARGE -> ModItems.LARGE_LIFT.get();
        };
    }

    private static void occupiedShaft(
            GameTestHelper helper, AbstractLift.Type type, boolean below) {
        AbstractLift existing = platform(helper, type, 12, below ? 12 : 4);
        guides(helper, existing, 2, 18);
        helper.getLevel().addFreshEntity(existing);
        existing.setMovementType(MovementType.DOWN);
        AbstractLift candidate = platform(helper, type, 12, below ? 4 : 12);
        ItemStack stack = new ItemStack(item(type), 2);
        helper.assertTrue(
                place(helper, candidate, stack) == InteractionResult.FAIL,
                "Second platform accepted in occupied shaft");
        helper.assertTrue(stack.getCount() == 2, "Failed placement consumed an item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void smallShaftRejectsSecondPlatformAbove(GameTestHelper h) {
        occupiedShaft(h, AbstractLift.Type.SMALL, false);
    }

    @GameTest(template = "empty")
    public static void mediumShaftRejectsSecondPlatformAbove(GameTestHelper h) {
        occupiedShaft(h, AbstractLift.Type.MEDIUM, false);
    }

    @GameTest(template = "empty")
    public static void largeShaftRejectsSecondPlatformAbove(GameTestHelper h) {
        occupiedShaft(h, AbstractLift.Type.LARGE, false);
    }

    @GameTest(template = "empty")
    public static void shaftRejectsSecondPlatformBelow(GameTestHelper h) {
        occupiedShaft(h, AbstractLift.Type.SMALL, true);
    }

    @GameTest(template = "empty")
    public static void removingPlatformReleasesShaft(GameTestHelper helper) {
        AbstractLift existing = platform(helper, AbstractLift.Type.SMALL, 12, 4);
        guides(helper, existing, 2, 18);
        helper.getLevel().addFreshEntity(existing);
        existing.discard();
        ItemStack stack = new ItemStack(ModItems.SMALL_LIFT.get(), 2);
        helper.assertTrue(
                place(helper, platform(helper, AbstractLift.Type.SMALL, 12, 12), stack)
                        .consumesAction(),
                "Removed platform kept shaft reserved");
        helper.assertTrue(stack.getCount() == 1, "Successful placement consumed wrong item count");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void neighboringShaftCanHaveItsOwnPlatform(GameTestHelper helper) {
        AbstractLift existing = platform(helper, AbstractLift.Type.SMALL, 8, 4);
        guides(helper, existing, 2, 18);
        helper.getLevel().addFreshEntity(existing);
        AbstractLift neighbor = platform(helper, AbstractLift.Type.SMALL, 20, 12);
        guides(helper, neighbor, 2, 18);
        helper.assertTrue(
                place(helper, neighbor, new ItemStack(ModItems.SMALL_LIFT.get())).consumesAction(),
                "Occupied shaft blocked independent neighbor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disconnectedGuideRoutesAreSeparateShafts(GameTestHelper helper) {
        AbstractLift existing = platform(helper, AbstractLift.Type.SMALL, 12, 4);
        guides(helper, existing, 2, 18);
        helper.getLevel().addFreshEntity(existing);
        int base = helper.absolutePos(BlockPos.ZERO).getY();
        for (BlockPos guide : existing.getGuidePositions()) {
            helper.getLevel()
                    .setBlockAndUpdate(guide.atY(base + 9), Blocks.AIR.defaultBlockState());
            helper.getLevel()
                    .setBlockAndUpdate(guide.atY(base + 10), Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(
                place(
                                helper,
                                platform(helper, AbstractLift.Type.SMALL, 12, 13),
                                new ItemStack(ModItems.SMALL_LIFT.get()))
                        .consumesAction(),
                "Separate route was treated as occupied");
        helper.succeed();
    }
}
