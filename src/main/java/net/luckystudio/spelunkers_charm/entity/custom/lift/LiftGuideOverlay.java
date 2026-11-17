package net.luckystudio.spelunkers_charm.entity.custom.lift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.luckystudio.spelunkers_charm.block.custom.LiftControllerBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.LiftControllerClient;
import net.luckystudio.spelunkers_charm.block.custom.LiftTrackBlock;
import net.luckystudio.spelunkers_charm.item.custom.LiftItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Shows where lift tracks go. While holding a lift, outlines the spot it would go and each of its
 * track spots. While holding a lift or a lift track, outlines the missing track spots of lifts that
 * are already placed. Green means a track is there and facing the right way; yellow means it isn't.
 */
@EventBusSubscriber(modid = "spelunkers_charm", value = Dist.CLIENT)
public final class LiftGuideOverlay {
    private static final double RANGE = 48;

    private LiftGuideOverlay() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        InteractionHand liftHand = handHolding(player, true);
        boolean holdingTrack = handHolding(player, false) != null;
        if (liftHand == null && !holdingTrack) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        if (liftHand != null
                && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            ItemStack stack = player.getItemInHand(liftHand);
            AbstractLift.Type type = ((LiftItem) stack.getItem()).getType();
            AbstractLift lift =
                    LiftPlacement.find(mc.level, new UseOnContext(player, liftHand, hit), type).lift();
            if (lift != null) {
                LevelRenderer.renderLineBox(
                        poseStack, lines, lift.getBoundingBox().deflate(0.002), 1F, 1F, 1F, 0.8F);
                drawGuides(poseStack, lines, mc.level, lift, Math.rint(lift.getY()), false);
            }
        }

        for (LiftControllerBlockEntity be : LiftControllerClient.active()) {
            AbstractLift lift = be.getClientLift();
            if (lift == null || lift.distanceToSqr(player) > RANGE * RANGE) continue;
            drawGuides(poseStack, lines, mc.level, lift, Math.rint(lift.getY()), true);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** Outline each track spot of {@code lift} on the row {@code row}. */
    private static void drawGuides(
            PoseStack poseStack, VertexConsumer lines, Level level, AbstractLift lift, double row,
            boolean onlyMissing) {
        LiftGuideSystem guides = new LiftGuideSystem(lift);
        boolean anyMissing = false;
        for (BlockPos spot : lift.getGuidePositions())
            if (!hasTrack(level, guides, spot.atY((int) row))) anyMissing = true;
        if (onlyMissing && !anyMissing) return;
        for (BlockPos spot : lift.getGuidePositions()) {
            BlockPos pos = spot.atY((int) row);
            boolean ok = hasTrack(level, guides, pos);
            if (onlyMissing && ok) continue;
            AABB box = new AABB(pos).deflate(0.01);
            if (ok) LevelRenderer.renderLineBox(poseStack, lines, box, 0.3F, 1F, 0.3F, 1F);
            else LevelRenderer.renderLineBox(poseStack, lines, box, 1F, 0.85F, 0.2F, 1F);
        }
    }

    private static boolean hasTrack(Level level, LiftGuideSystem guides, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof LiftTrackBlock
                && state.getValue(LiftTrackBlock.FACING) == guides.inwardFacing(pos);
    }

    /** The hand holding a lift item ({@code lift}) or a lift track ({@code !lift}), if any. */
    private static InteractionHand handHolding(LocalPlayer player, boolean lift) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (lift ? stack.getItem() instanceof LiftItem
                    : stack.getItem() instanceof BlockItem block
                            && block.getBlock() instanceof LiftTrackBlock) return hand;
        }
        return null;
    }
}
