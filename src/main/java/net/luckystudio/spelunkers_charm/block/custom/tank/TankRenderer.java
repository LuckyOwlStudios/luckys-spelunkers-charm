// Tank renderer.
package net.luckystudio.spelunkers_charm.block.custom.tank;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public final class TankRenderer implements BlockEntityRenderer<TankBlockEntity> {
    private static final float INNER_MIN = 0.115F;
    private static final float INNER_MAX = 1.0F - INNER_MIN;

    public TankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            TankBlockEntity tank,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        renderFluid(
                tank.storage().getFluid(),
                tank.getLevel(),
                tank.getBlockPos(),
                poses,
                buffers,
                light);
    }

    public static void renderFluid(
            FluidStack fluid,
            Level level,
            BlockPos pos,
            PoseStack poses,
            MultiBufferSource buffers,
            int light) {
        if (fluid.isEmpty() || level == null) return;
        var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture = extension.getStillTexture(fluid);
        if (texture == null) texture = extension.getFlowingTexture(fluid);
        if (texture == null) return;

        TextureAtlasSprite sprite =
                Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color =
                fluid.getFluid().isSame(Fluids.WATER)
                        ? extension.getTintColor(fluid.getFluid().defaultFluidState(), level, pos)
                        : extension.getTintColor(fluid);
        int fluidLight =
                LightTexture.pack(
                        Math.max(
                                LightTexture.block(light),
                                fluid.getFluidType().getLightLevel(fluid)),
                        LightTexture.sky(light));
        var vertices =
                buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        var pose = poses.last();
        float fill = Math.min(1.0F, fluid.getAmount() / (float) TankStorage.CAPACITY);
        float n = INNER_MIN;
        float f = INNER_MAX;
        float top = Mth.lerp(fill, n, f);
        float sideV = Mth.lerp(fill, sprite.getV1(), sprite.getV0());
        float flatV = sprite.getV0();

        quad(
                vertices,
                pose,
                sprite,
                color,
                fluidLight,
                Direction.UP,
                flatV,
                n,
                top,
                f,
                f,
                top,
                f,
                f,
                top,
                n,
                n,
                top,
                n);
        quad(
                vertices,
                pose,
                sprite,
                color,
                fluidLight,
                Direction.DOWN,
                flatV,
                n,
                n,
                n,
                f,
                n,
                n,
                f,
                n,
                f,
                n,
                n,
                f);
        quad(
                vertices,
                pose,
                sprite,
                color,
                fluidLight,
                Direction.NORTH,
                sideV,
                f,
                n,
                n,
                n,
                n,
                n,
                n,
                top,
                n,
                f,
                top,
                n);
        quad(
                vertices,
                pose,
                sprite,
                color,
                fluidLight,
                Direction.SOUTH,
                sideV,
                n,
                n,
                f,
                f,
                n,
                f,
                f,
                top,
                f,
                n,
                top,
                f);
        quad(
                vertices,
                pose,
                sprite,
                color,
                fluidLight,
                Direction.WEST,
                sideV,
                n,
                n,
                n,
                n,
                n,
                f,
                n,
                top,
                f,
                n,
                top,
                n);
        quad(
                vertices,
                pose,
                sprite,
                color,
                fluidLight,
                Direction.EAST,
                sideV,
                f,
                n,
                f,
                f,
                n,
                n,
                f,
                top,
                n,
                f,
                top,
                f);
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int color,
            int light,
            Direction normal,
            float topV,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz) {
        vertex(vertices, pose, color, light, normal, ax, ay, az, sprite.getU0(), sprite.getV1());
        vertex(vertices, pose, color, light, normal, bx, by, bz, sprite.getU1(), sprite.getV1());
        vertex(vertices, pose, color, light, normal, cx, cy, cz, sprite.getU1(), topV);
        vertex(vertices, pose, color, light, normal, dx, dy, dz, sprite.getU0(), topV);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            int color,
            int light,
            Direction normal,
            float x,
            float y,
            float z,
            float u,
            float v) {
        vertices.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }
}
