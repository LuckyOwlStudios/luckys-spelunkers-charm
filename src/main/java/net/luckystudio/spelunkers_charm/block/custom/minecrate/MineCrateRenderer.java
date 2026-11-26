// Mine crate renderer.
package net.luckystudio.spelunkers_charm.block.custom.minecrate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.CargoCountLabel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class MineCrateRenderer implements BlockEntityRenderer<MineCrateBlockEntity> {
    private final ItemRenderer items;
    private final Font font;

    public MineCrateRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
        font = context.getFont();
    }

    @Override
    public void render(
            MineCrateBlockEntity crate,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        Level level = crate.getLevel();
        if (level == null) return;
        int lidLight = LevelRenderer.getLightColor(level, crate.getBlockPos().above());
        renderLabel(items, crate.storage().displayItem(), level, poses, buffers, lidLight);
        renderCount(crate, poses, buffers, lidLight);
    }

    private void renderCount(
            MineCrateBlockEntity crate, PoseStack poses, MultiBufferSource buffers, int light) {
        var minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !hit.getBlockPos().equals(crate.getBlockPos())) return;
        var label = CargoCountLabel.forAmount(crate.storage().count());
        if (label == null) return;

        poses.pushPose();
        poses.translate(0.5, 1.3, 0.5);
        poses.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poses.scale(0.025F, -0.025F, 0.025F);
        var matrix = poses.last().pose();
        float x = -font.width(label) / 2.0F;
        int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255) << 24;
        font.drawInBatch(
                label,
                x,
                0,
                0x20FFFFFF,
                false,
                matrix,
                buffers,
                Font.DisplayMode.SEE_THROUGH,
                background,
                light);
        font.drawInBatch(
                label, x, 0, -1, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
        poses.popPose();
    }

    public static void renderLabel(
            ItemRenderer items,
            ItemStack stack,
            Level level,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        if (stack.isEmpty()) return;
        poses.pushPose();
        poses.translate(0.5, 1.016, 0.5);
        poses.mulPose(Axis.XP.rotationDegrees(-90));
        // Flatten the depth of 3D item models to keep the label on the lid.
        poses.scale(0.65F, 0.65F, 0.025F);
        items.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                poses,
                buffers,
                level,
                0);
        poses.popPose();
    }
}
