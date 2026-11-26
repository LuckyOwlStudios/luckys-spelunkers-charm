// Locomotive renderer.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import com.mojang.blaze3d.vertex.PoseStack;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.override.NewLocomotiveModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;

public final class LocomotiveRenderer extends MinecartRenderer<AbstractMinecart> {
    private static final ResourceLocation[] TEXTURES = {
        texture("locomotive"),
        texture("locomotive_1"),
        texture("locomotive_2"),
        texture("locomotive_3")
    };
    private final NewLocomotiveModel<AbstractMinecart> locomotiveModel;

    private static ResourceLocation texture(String name) {
        return SpelunkersCharm.id("textures/entity/minecart/" + name + ".png");
    }

    public LocomotiveRenderer(EntityRendererProvider.Context context) {
        super(context, SpelunkersCharmClient.LOCOMOTIVE_MODEL);
        locomotiveModel =
                new NewLocomotiveModel<>(context.bakeLayer(SpelunkersCharmClient.LOCOMOTIVE_MODEL));
        model = locomotiveModel;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractMinecart cart) {
        return TEXTURES[
                cart instanceof LocomotiveMinecart engine
                        ? Math.clamp(engine.activeBurners(), 0, 3)
                        : 0];
    }

    @Override
    public void render(
            AbstractMinecart cart,
            float yaw,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        if (cart instanceof LocomotiveMinecart engine) {
            var links = (MinecartCouplingVisuals) cart;
            locomotiveModel.setState(
                    links.spelunkersCharm$firstCouplingId() >= 0
                            || links.spelunkersCharm$secondCouplingId() >= 0,
                    engine.emergencyBrake(),
                    engine.isReversed());
        }
        super.render(cart, yaw, partialTick, poses, buffers, light);
    }

    @Override
    protected void renderMinecartContents(
            AbstractMinecart cart,
            float partialTick,
            BlockState state,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {}
}
