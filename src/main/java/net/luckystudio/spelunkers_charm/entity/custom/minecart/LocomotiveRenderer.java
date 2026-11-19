package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import com.mojang.blaze3d.vertex.PoseStack;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.override.OverrideMinecartRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the locomotive with its own model and texture. The lever on its side moves with the direction. */
public class LocomotiveRenderer extends OverrideMinecartRenderer {

    private static final ResourceLocation LOCOMOTIVE_LOCATION = SpelunkersCharm.id("textures/entity/minecart/locomotive.png");

    public LocomotiveRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LocomotiveModel(context.bakeLayer(SpelunkersCharmClient.LOCOMOTIVE_MODEL));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractMinecart entity) {
        return LOCOMOTIVE_LOCATION;
    }

    /** The model is the whole locomotive, so no block is drawn inside it. */
    @Override
    protected void renderMinecartContents(AbstractMinecart entity, float partialTicks, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    }
}
