package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;

public class OverrideMinecartRenderer extends MinecartRenderer<AbstractMinecart> {

    private static final ResourceLocation MINECART_LOCATION = SpelunkersCharm.id("textures/entity/minecart/minecart.png");
    protected final EntityModel<Minecart> newMinecartModel;

    public OverrideMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, SpelunkersCharmClient.NEW_MINECART_MODEL);
        this.newMinecartModel = new NewMinecartModel<>(context.bakeLayer(SpelunkersCharmClient.NEW_MINECART_MODEL));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractMinecart entity) {
        return MINECART_LOCATION;
    }

    @Override
    public void render(AbstractMinecart entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
