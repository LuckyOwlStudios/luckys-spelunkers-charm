package net.luckystudio.spelunkers_charm.entity.custom.cave_borer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the worm one segment at a time along its body chain. Each segment faces the one ahead of it
 * and takes the light of the block it is in, so segments still inside the wall look dark.
 */
public class CaveBorerRenderer extends EntityRenderer<CaveBorer> {
    private final CaveBorerModel model;

    public CaveBorerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CaveBorerModel(context.bakeLayer(CaveBorerModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CaveBorer entity) {
        return entity.getVariant().getTexture();
    }

    @Override
    public void render(@NotNull CaveBorer entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        if (entity.isBurrowed()) return;

        // The pose stack starts at the entity's interpolated feet position
        double originX = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double originY = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double originZ = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(entity)));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        float scale = entity.getSizeScale();
        float jaw = entity.getJawOpen(partialTick) * 0.6F;
        this.model.upperJaw.xRot = jaw;
        this.model.lowerJaw.xRot = -jaw;

        float time = entity.tickCount + partialTick;
        boolean wriggle = entity.getState() == CaveBorer.State.STUNNED || entity.isDeadOrDying();
        float deathRoll = entity.deathTime > 0 ? Math.min(1.0F, (entity.deathTime + partialTick - 1.0F) / 12.0F) * 90.0F : 0.0F;

        int count = entity.getSegmentCount();
        Vec3 ahead = null;
        for (int i = 0; i < count; i++) {
            Vec3 pos = entity.getSegmentPosition(i, partialTick);
            Vec3 direction = i == 0 ? pos.subtract(entity.getSegmentPosition(1, partialTick)) : ahead.subtract(pos);
            if (direction.lengthSqr() < 1.0E-6) {
                direction = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
            }
            float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
            float pitch = (float) (-(Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG));

            poseStack.pushPose();
            poseStack.translate(pos.x - originX, pos.y - originY, pos.z - originZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            if (wriggle) {
                poseStack.translate(Mth.sin(time * 0.7F + i * 1.3F) * 0.08F * scale, 0.0F, 0.0F);
            }
            if (deathRoll > 0.0F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(deathRoll));
            }
            poseStack.scale(-scale, -scale, scale);

            ModelPart part = i == 0 ? this.model.head : i == count - 1 ? this.model.tail : this.model.body;
            int light = LevelRenderer.getLightColor(entity.level(), BlockPos.containing(pos));
            part.render(poseStack, consumer, light, overlay);
            poseStack.popPose();
            ahead = pos;
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
}
