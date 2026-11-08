package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

/** Client only. Draws a hung lantern on the front handle of a minecart. */
public final class CartLanternRenderer {

    private static final float SCALE = 0.5F;
    /** On top of the cart's end handle, in cart space (the cart's length runs along X). */
    private static final double OFFSET_X = 0.55;
    private static final double OFFSET_Y = 0.8125;

    private CartLanternRenderer() {
    }

    /** Call with the pose stack at the cart's render position, before the cart itself is rotated. */
    public static void renderLantern(AbstractMinecart cart, float entityYaw, PoseStack poseStack, MultiBufferSource buffer) {
        CartLantern lantern = ClientCartData.getLantern(cart.getId());
        if (lantern == CartLantern.NONE) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.translate(OFFSET_X, OFFSET_Y, 0.0);
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-0.5, 0.0, -0.5);
        // The lantern is a light source, so it's drawn fully bright.
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(lantern.block().defaultBlockState(), poseStack, buffer,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
