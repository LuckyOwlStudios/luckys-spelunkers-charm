package net.luckystudio.spelunkers_charm.client.helmet;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws the mining helmet's beam from the head of whoever wears it (players in third person, other players,
 * and any humanoid mob or armor stand). The lamp position and direction come from the head model itself,
 * so the beam follows looking around, crouching, swimming and every other pose.
 */
public class MiningHelmetBeamLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    public MiningHelmetBeamLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!HelmetBeamRenderer.isDrawingWorld() || !HelmetBeamRenderer.enabled()) return;
        if (!MiningHelmetBeam.hasLitLamp(entity) || entity.isSleeping()) return;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        // Your own beam isn't drawn in first person, even with mods that draw your body there.
        if (entity == camera.getEntity() && !camera.isDetached()) return;

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        poseStack.popPose();

        // Model space has -Y up and -Z forward. One unit is one block (scaled with the entity).
        Vector3f origin = pose.transformPosition(0.0F, -MiningHelmetBeam.LAMP_UP_PX / 16.0F,
                -MiningHelmetBeam.LAMP_FORWARD_PX / 16.0F, new Vector3f());
        Vector3f forward = pose.transformDirection(0.0F, 0.0F, -1.0F, new Vector3f());
        float scale = forward.length();
        if (scale < 1.0E-4F) return;
        forward.div(scale);
        Vector3f up = pose.transformDirection(0.0F, -1.0F, 0.0F, new Vector3f()).normalize();

        // Positions here are relative to the camera; trace the hit in world space.
        Vec3 cameraPos = camera.getPosition();
        Vec3 lampWorld = new Vec3(cameraPos.x + origin.x, cameraPos.y + origin.y, cameraPos.z + origin.z);
        Vec3 forwardWorld = new Vec3(forward.x, forward.y, forward.z);
        double length = MiningHelmetBeam.traceDistance(entity.level(), lampWorld, forwardWorld,
                HelmetBeamRenderer.VISIBLE_LENGTH, entity);

        HelmetBeamRenderer.drawBeam(buffer.getBuffer(HelmetBeamRenderType.BEAM), origin, forward, up,
                (float) length, scale, HelmetBeamRenderer.opacity());
    }
}
