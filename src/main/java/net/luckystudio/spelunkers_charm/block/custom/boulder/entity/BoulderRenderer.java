package net.luckystudio.spelunkers_charm.block.custom.boulder.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BoulderRenderer<T extends Boulder> extends EntityRenderer<Boulder> {

    private static final ResourceLocation BOULDER_LOCATION = SpelunkersCharm.id("textures/entity/boulder/boulder.png");
    private static final ResourceLocation LUSH_BOULDER_LOCATION = SpelunkersCharm.id("textures/entity/boulder/lush_boulder.png");
    EntityModel<T> boulderModel;

    public BoulderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.boulderModel = new BoulderModel<>(context.bakeLayer(SpelunkersCharmClient.BOULDER));
    }

    @Override
    public ResourceLocation getTextureLocation(Boulder entity) {
        return entity.getBoulderType().ordinal() < 4 ? BOULDER_LOCATION : LUSH_BOULDER_LOCATION;
    }

    @Override
    public void render(Boulder entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.scale(1.0F, 1.0F, 1.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        VertexConsumer woodVertexConsumer = buffer.getBuffer(boulderModel.renderType(getTextureLocation(entity)));
        boulderModel.renderToBuffer(poseStack, woodVertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}