package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Client only. Draws a stretched chain between coupled carts. */
public final class CartLinkRenderer {

    private static final double CHAIN_HEIGHT = 0.35;

    private CartLinkRenderer() {
    }

    /** Call with the pose stack at the cart's render position, before the cart itself is rotated. */
    public static void renderLinks(AbstractMinecart cart, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int[] linked = ClientCartData.get(cart.getId());
        if (linked.length == 0) return;

        for (int otherId : linked) {
            Entity other = cart.level().getEntity(otherId);
            if (!(other instanceof AbstractMinecart)) continue;
            // Draw each chain once: from the cart with the lower id, unless only this cart knows about the link.
            if (cart.getId() > otherId && ClientCartData.isLinked(otherId, cart.getId())) continue;
            renderChain(cart.getPosition(partialTicks), other.getPosition(partialTicks), poseStack, buffer, packedLight);
        }
    }

    private static void renderChain(Vec3 from, Vec3 to, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3) return;
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        // A chain block lying along Z, rotated to point at the other cart and stretched to reach it.
        BlockState chain = Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        poseStack.pushPose();
        poseStack.translate(0.0, CHAIN_HEIGHT, 0.0);
        poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(delta.x, delta.z)));
        poseStack.mulPose(Axis.XP.rotation((float) -Math.atan2(delta.y, horizontal)));
        poseStack.scale(1.0F, 1.0F, (float) length);
        poseStack.translate(-0.5, -0.5, 0.0);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(chain, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
