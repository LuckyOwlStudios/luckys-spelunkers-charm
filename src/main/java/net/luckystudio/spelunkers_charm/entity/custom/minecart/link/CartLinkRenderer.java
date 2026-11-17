package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

/** Client only. Draws a chain between coupled carts, with the links repeated rather than stretched. */
public final class CartLinkRenderer {

    private static final ResourceLocation CHAIN_TEXTURE = ResourceLocation.withDefaultNamespace("block/chain");
    private static final double CHAIN_HEIGHT = 0.35;
    /** How far in from each cart's center the chain starts, so it comes out from the cart's edge. */
    private static final double END_INSET = 0.4;
    /** Half the width of the chain, 3 pixels wide like the chain block. */
    private static final float HALF_WIDTH = 1.5F / 16.0F;

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
        // The ends are hidden inside the carts, so only draw the part between them.
        double visible = length - 2.0 * END_INSET;
        if (visible < 0.05) return;
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(CHAIN_TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));

        poseStack.pushPose();
        poseStack.translate(0.0, CHAIN_HEIGHT, 0.0);
        poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(delta.x, delta.z)));
        poseStack.mulPose(Axis.XP.rotation((float) -Math.atan2(delta.y, horizontal)));
        poseStack.translate(0.0, 0.0, END_INSET);
        PoseStack.Pose pose = poseStack.last();

        // Repeat the chain texture one block at a time, with the last link cut short, instead of stretching it.
        for (double start = 0.0; start < visible - 1.0E-4; start += 1.0) {
            float segment = (float) Math.min(1.0, visible - start);
            float z0 = (float) start;
            float z1 = z0 + segment;
            // Flat plane (links seen from above): the left half of the chain texture.
            plane(consumer, pose, packedLight, sprite, 0.0F, 3.0F / 16.0F, segment,
                    -HALF_WIDTH, 0.0F, z0, HALF_WIDTH, 0.0F, z0, HALF_WIDTH, 0.0F, z1, -HALF_WIDTH, 0.0F, z1, 0.0F, 1.0F, 0.0F);
            // Upright plane (links seen from the side): the right half.
            plane(consumer, pose, packedLight, sprite, 3.0F / 16.0F, 6.0F / 16.0F, segment,
                    0.0F, -HALF_WIDTH, z0, 0.0F, HALF_WIDTH, z0, 0.0F, HALF_WIDTH, z1, 0.0F, -HALF_WIDTH, z1, 1.0F, 0.0F, 0.0F);
        }
        poseStack.popPose();
    }

    /** One textured quad. The texture runs along the quad's length, and only {@code segment} of a block of it is used. */
    private static void plane(VertexConsumer consumer, PoseStack.Pose pose, int light, TextureAtlasSprite sprite,
                              float uFrom, float uTo, float segment,
                              float x0, float y0, float z0, float x1, float y1, float z1,
                              float x2, float y2, float z2, float x3, float y3, float z3,
                              float nx, float ny, float nz) {
        float u0 = sprite.getU(uFrom);
        float u1 = sprite.getU(uTo);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(segment);
        vertex(consumer, pose, light, x0, y0, z0, u0, v0, nx, ny, nz);
        vertex(consumer, pose, light, x1, y1, z1, u1, v0, nx, ny, nz);
        vertex(consumer, pose, light, x2, y2, z2, u1, v1, nx, ny, nz);
        vertex(consumer, pose, light, x3, y3, z3, u0, v1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
