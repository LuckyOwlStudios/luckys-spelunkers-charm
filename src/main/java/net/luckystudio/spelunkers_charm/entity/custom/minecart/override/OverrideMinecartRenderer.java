// Override minecart renderer.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLanterns;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class OverrideMinecartRenderer extends MinecartRenderer<AbstractMinecart> {

    private static final ResourceLocation MINECART_LOCATION =
            SpelunkersCharm.id("textures/entity/minecart/minecart.png");
    private final NewMinecartModel<AbstractMinecart> cartModel;
    private final MinecartLampModel<AbstractMinecart> lampModel;
    private boolean lampPass;
    private static final ResourceLocation LAMP_LOCATION =
            SpelunkersCharm.id("textures/entity/minecart/minecart_lantern.png");

    public OverrideMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, SpelunkersCharmClient.NEW_MINECART_MODEL);
        // Swap in our own model so the lamp can be hidden and drawn glowing.
        this.cartModel =
                new NewMinecartModel<>(context.bakeLayer(SpelunkersCharmClient.NEW_MINECART_MODEL));
        this.model = this.cartModel;
        this.lampModel =
                new MinecartLampModel<>(
                        context.bakeLayer(SpelunkersCharmClient.MINECART_LAMP_MODEL));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractMinecart entity) {
        return lampPass ? LAMP_LOCATION : MINECART_LOCATION;
    }

    @Override
    public void render(
            AbstractMinecart entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        this.cartModel.setHooks(
                net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplerSides.mask(
                        entity, partialTicks));
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        if (CartLanterns.getLantern(entity) != CartLantern.NONE) {
            lampModel.setLightVisible(true);
            lampPass = true;
            model = lampModel;
            try {
                super.render(
                        entity,
                        entityYaw,
                        partialTicks,
                        poseStack,
                        buffer,
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
            } finally {
                lampPass = false;
                model = cartModel;
            }
        }
    }

    @Override
    protected void renderMinecartContents(
            AbstractMinecart entity,
            float partialTicks,
            net.minecraft.world.level.block.state.BlockState state,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        if (!lampPass)
            super.renderMinecartContents(entity, partialTicks, state, poses, buffers, light);
    }
}
