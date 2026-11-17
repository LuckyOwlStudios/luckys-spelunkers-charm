package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.ClientCartData;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLinkRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class OverrideMinecartRenderer extends MinecartRenderer<AbstractMinecart> {

    private static final ResourceLocation MINECART_LOCATION = SpelunkersCharm.id("textures/entity/minecart/minecart.png");
    private final NewMinecartModel<AbstractMinecart> cartModel;

    public OverrideMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, SpelunkersCharmClient.NEW_MINECART_MODEL);
        // Swap in our own model so the lamp can be hidden and drawn glowing.
        this.cartModel = new NewMinecartModel<>(context.bakeLayer(SpelunkersCharmClient.NEW_MINECART_MODEL));
        this.model = this.cartModel;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractMinecart entity) {
        return MINECART_LOCATION;
    }

    @Override
    public void render(AbstractMinecart entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.cartModel.setLightVisible(ClientCartData.getLantern(entity.getId()) != CartLantern.NONE);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        CartLinkRenderer.renderLinks(entity, partialTicks, poseStack, buffer, packedLight);
    }
}
