package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class LiftPoseTests {
    private static Player supportedPlayer(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(12, 5, 12));
        AbstractLift lift =
                AbstractLift.createLift(
                        helper.getLevel(),
                        center.getX() + 0.5,
                        center.getY() + 0.5,
                        center.getZ() + 0.5,
                        AbstractLift.Type.SMALL,
                        ItemStack.EMPTY,
                        null);
        helper.getLevel().addFreshEntity(lift);
        Player player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().flying = false;
        player.setPos(lift.getX(), lift.getBoundingBox().maxY - 0.15, lift.getZ());
        player.setDeltaMovement(Vec3.ZERO);
        player.setShiftKeyDown(true);
        return player;
    }

    private static boolean fits(Player player, Pose pose) {
        try {
            Method method =
                    Player.class.getDeclaredMethod(
                            "canPlayerFitWithinBlocksAndEntitiesWhen", Pose.class);
            method.setAccessible(true);
            return (boolean) method.invoke(player, pose);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    @GameTest(template = "empty")
    public static void supportingDeckDoesNotRejectCrouchingPose(GameTestHelper helper) {
        Player player = supportedPlayer(helper);
        helper.assertFalse(
                helper.getLevel()
                        .noCollision(
                                player,
                                player.getDimensions(Pose.CROUCHING)
                                        .makeBoundingBox(player.position())
                                        .deflate(1.0E-7)),
                "Test did not reproduce deck pose collision");
        helper.assertTrue(fits(player, Pose.CROUCHING), "Moving deck still rejects crouching");
        helper.assertTrue(fits(player, Pose.STANDING), "Release cannot restore standing on deck");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void poseFixRetainsLowCeilingCollision(GameTestHelper helper) {
        Player player = supportedPlayer(helper);
        BlockPos ceiling = helper.absolutePos(new BlockPos(12, 8, 12));
        helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
        helper.assertTrue(fits(player, Pose.CROUCHING), "Low ceiling should allow crouching");
        helper.assertFalse(fits(player, Pose.STANDING), "Pose fix ignored a solid ceiling");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void poseFixRetainsOtherVehicleCollision(GameTestHelper helper) {
        Player player = supportedPlayer(helper);
        var boat = EntityType.BOAT.create(helper.getLevel());
        boat.setPos(player.position());
        helper.getLevel().addFreshEntity(boat);
        helper.assertFalse(fits(player, Pose.CROUCHING), "Pose fix ignored an unrelated vehicle");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void poseFixDoesNotTreatJumpingPlayerAsSupported(GameTestHelper helper) {
        Player player = supportedPlayer(helper);
        player.setDeltaMovement(0, 0.42, 0);
        helper.assertFalse(
                fits(player, Pose.CROUCHING),
                "Airborne player received a deck collision exception");
        helper.succeed();
    }
}
