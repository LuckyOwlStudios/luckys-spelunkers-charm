// Minecart renderer mixin.
package net.luckystudio.spelunkers_charm.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartBodyOrientation;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartBodyPose;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartSpeedRules;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = MinecartRenderer.class, remap = false)
public abstract class MinecartRendererMixin {
    @ModifyVariable(
            method =
                    "render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("LOAD"),
            argsOnly = true,
            ordinal = 0)
    private float spelunkersCharm$stableBody(
            float yaw,
            AbstractMinecart cart,
            float entityYaw,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        return MinecartSpeedRules.managed(cart)
                ? MinecartBodyOrientation.yaw(cart, partialTick)
                : yaw;
    }

    @ModifyVariable(
            method =
                    "render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("LOAD"),
            ordinal = 5)
    private float spelunkersCharm$flightBodyPitch(
            float pitch,
            AbstractMinecart cart,
            float entityYaw,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        return MinecartSpeedRules.managed(cart)
                ? MinecartBodyPose.pitch(cart, MinecartBodyPose.yaw(cart, partialTick), partialTick)
                : pitch;
    }
}
