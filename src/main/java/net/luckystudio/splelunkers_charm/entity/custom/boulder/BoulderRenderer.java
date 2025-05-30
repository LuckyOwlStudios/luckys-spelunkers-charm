package net.luckystudio.splelunkers_charm.entity.custom.boulder;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BoulderRenderer extends EntityRenderer<Boulder> {

    private static final ResourceLocation BOULDER_LOCATION = SpelunkersCharm.id("textures/block/boulder/boulder.png");

    public BoulderRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Boulder entity) {
        return BOULDER_LOCATION;
    }

    @Override
    public void render(Boulder entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}