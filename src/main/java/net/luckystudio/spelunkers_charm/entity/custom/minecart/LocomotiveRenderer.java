package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.override.OverrideMinecartRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Placeholder look for the locomotive: the engine (a blast furnace for now) with a lever on its side
 * that flips when the direction is switched. Meant to be replaced by a proper model.
 */
public class LocomotiveRenderer extends OverrideMinecartRenderer {

    public LocomotiveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderMinecartContents(AbstractMinecart entity, float partialTicks, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.renderMinecartContents(entity, partialTicks, state, poseStack, buffer, packedLight);
        if (!(entity instanceof LocomotiveMinecart locomotive)) return;

        // The lever sits in the block next to the engine, attached to the engine's side.
        BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(LeverBlock.POWERED, locomotive.isReversed());
        poseStack.pushPose();
        poseStack.translate(0.0, 0.0, -1.0);
        super.renderMinecartContents(entity, partialTicks, lever, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
