// Tank minecart renderer.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;

import net.luckystudio.spelunkers_charm.block.custom.tank.TankRenderer;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.TankMinecart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;

public final class TankMinecartRenderer extends OverrideMinecartRenderer {
    public TankMinecartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderMinecartContents(
            AbstractMinecart entity,
            float partialTicks,
            BlockState state,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        super.renderMinecartContents(entity, partialTicks, state, poses, buffers, light);
        if (entity instanceof TankMinecart tank) {
            TankRenderer.renderFluid(
                    tank.storage().getFluid(),
                    tank.level(),
                    tank.blockPosition(),
                    poses,
                    buffers,
                    light);
        }
    }
}
