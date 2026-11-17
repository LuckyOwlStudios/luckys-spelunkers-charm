package net.luckystudio.spelunkers_charm.entity.custom.lift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.spelunkers_charm.block.custom.LiftControllerBlockEntity;
import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLiftModel;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLiftModel;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLiftModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;

/**
 * Draws a Sable lift with the lift entity's own model, textures and animations. Sable moves (and
 * tips) the whole thing along with the deck, so this only has to draw it as if standing still.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LiftControllerRenderer implements BlockEntityRenderer<LiftControllerBlockEntity> {
    private final EntityModel small;
    private final EntityModel medium;
    private final EntityModel large;

    public LiftControllerRenderer(BlockEntityRendererProvider.Context context) {
        this.small = new SmallLiftModel(context.bakeLayer(SpelunkersCharmClient.SMALL_LIFT));
        this.medium = new MediumLiftModel(context.bakeLayer(SpelunkersCharmClient.MEDIUM_LIFT));
        this.large = new LargeLiftModel(context.bakeLayer(SpelunkersCharmClient.LARGE_LIFT));
    }

    @Override
    public void render(
            LiftControllerBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        AbstractLift.Type type = be.getLiftType();
        AbstractLift lift = be.getClientLift();
        if (type == null || lift == null) return;
        EntityModel model =
                switch (type) {
                    case SMALL -> small;
                    case MEDIUM -> medium;
                    case LARGE -> large;
                };

        poseStack.pushPose();
        // Same transform as AbstractLiftRenderer, from the bottom center of the controller block.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-be.getFacing().toYRot()));

        // limbSwing carries the partial tick: the models turn their gears by distance travelled.
        model.setupAnim(lift, partialTick, 0.0F, 0.1F, 0.0F, 0.0F);
        model.renderToBuffer(
                poseStack,
                buffers.getBuffer(model.renderType(AbstractLiftRenderer.woodTexture(type, be.getWood()))),
                packedLight,
                OverlayTexture.NO_OVERLAY);
        model.renderToBuffer(
                poseStack,
                buffers.getBuffer(
                        model.renderType(AbstractLiftRenderer.mineralTexture(type, be.getMineral()))),
                packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LiftControllerBlockEntity be) {
        // The model's gears reach out to the tracks, past the deck itself.
        int r = be.radius() + 2;
        return new AABB(be.getBlockPos()).inflate(r, 1, r);
    }

    @Override
    public boolean shouldRenderOffScreen(LiftControllerBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
