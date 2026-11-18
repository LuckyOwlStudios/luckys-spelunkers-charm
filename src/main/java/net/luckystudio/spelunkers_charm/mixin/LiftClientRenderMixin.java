package net.luckystudio.spelunkers_charm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientSupport;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps remote passengers on the same rendered deck without fighting their lerp. */
@Mixin(value = EntityRenderDispatcher.class, remap = false)
public abstract class LiftClientRenderMixin {
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void spelunkersCharm$anchorRemoteRider(
            Entity entity,
            double x,
            double y,
            double z,
            float yaw,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            CallbackInfo info) {
        poses.pushPose();
        poses.translate(0, LiftClientSupport.getRenderOffset(entity, partialTick), 0);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void spelunkersCharm$restoreRiderRender(
            Entity entity,
            double x,
            double y,
            double z,
            float yaw,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            CallbackInfo info) {
        poses.popPose();
    }
}
