// Mine crate minecart renderer.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;

import net.luckystudio.spelunkers_charm.block.custom.minecrate.MineCrateRenderer;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MineCrateMinecart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;

public final class MineCrateMinecartRenderer extends OverrideMinecartRenderer {
    private final ItemRenderer items;

    public MineCrateMinecartRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
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
        if (entity instanceof MineCrateMinecart crate) {
            MineCrateRenderer.renderLabel(
                    items, crate.displayItem(), crate.level(), poses, buffers, light);
        }
    }
}
