// Minecart coupling renderer.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientSupport;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplerAnchors;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplingVisuals;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartHingeGeometry;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartRailPath;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public final class MinecartCouplingRenderer {
    private static final RenderType METAL =
            RenderType.entityCutoutNoCull(
                    SpelunkersCharm.id("textures/entity/minecart/coupling_metal.png"));
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 SIDE = new Vec3(1, 0, 0);
    private static final double PIXEL = 1.0 / 16;
    private static final double COUPLER_HEIGHT = 0.34;

    private MinecartCouplingRenderer() {}

    public static void render(
            AbstractMinecart cart,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource buffers,
            int light) {
        MinecartCouplingVisuals links = (MinecartCouplingVisuals) cart;
        renderPeer(
                cart, links.spelunkersCharm$firstCouplingId(), partialTick, pose, buffers, light);
        renderPeer(
                cart, links.spelunkersCharm$secondCouplingId(), partialTick, pose, buffers, light);
    }

    private static void renderPeer(
            AbstractMinecart cart,
            int peerId,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource buffers,
            int light) {
        if (peerId <= cart.getId()
                || !(cart.level().getEntity(peerId) instanceof AbstractMinecart peer)) return;
        Vec3 raw = cart.getPosition(partialTick);
        Vec3 origin = raw.add(0, LiftClientSupport.getRenderOffset(cart, partialTick), 0);
        Vec3 firstCenter = couplingCenter(cart, partialTick);
        Vec3 secondCenter = couplingCenter(peer, partialTick);
        MinecartRailPath path = MinecartRailPath.between(cart, peer);
        Vec3 firstToward = path == null ? secondCenter.subtract(firstCenter) : path.firstToward();
        Vec3 secondToward = path == null ? firstCenter.subtract(secondCenter) : path.secondToward();
        Vec3 first =
                MinecartCouplerAnchors.endpoint(cart, peer, partialTick, firstCenter)
                        .subtract(origin);
        Vec3 second =
                MinecartCouplerAnchors.endpoint(peer, cart, partialTick, secondCenter)
                        .subtract(origin);
        Vec3 difference = second.subtract(first);
        double length = difference.length();
        if (length < 0.04 || length > 7) return;

        Vec3 along = difference.scale(1 / length);
        Vec3 across = along.cross(UP).normalize();
        if (across.lengthSqr() < 0.1) across = SIDE;
        Vec3 vertical = across.cross(along).normalize();
        Vec3 center = first.add(difference.scale(0.5));
        int reach = Math.max(3, Mth.ceil(length * 0.5 / PIXEL));
        VertexConsumer vertices = buffers.getBuffer(METAL);
        renderMount(
                vertices,
                pose,
                first,
                MinecartCouplerAnchors.axis(cart, partialTick),
                MinecartCouplerAnchors.up(cart, partialTick),
                MinecartHingeGeometry.side(cart, peer),
                light);
        renderMount(
                vertices,
                pose,
                second,
                MinecartCouplerAnchors.axis(peer, partialTick),
                MinecartCouplerAnchors.up(peer, partialTick),
                MinecartHingeGeometry.side(peer, cart),
                light);
        renderHook(vertices, pose, new Frame(center, along, vertical, across), reach, light);
        renderHook(
                vertices,
                pose,
                new Frame(center, along.scale(-1), vertical, across.scale(-1)),
                reach,
                light);
    }

    private static void renderMount(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vec3 point,
            Vec3 axis,
            Vec3 up,
            int hookSide,
            int light) {
        Vec3 outward = axis.scale(hookSide);
        Vec3 side = outward.cross(up).normalize();
        if (side.lengthSqr() < 0.1) side = SIDE;
        cuboid(
                vertices,
                pose,
                new Frame(point, outward, side.cross(outward).normalize(), side),
                -1,
                1,
                1,
                -2,
                2,
                Skin.BASE,
                light);
    }

    private static Vec3 couplingCenter(AbstractMinecart cart, float partialTick) {
        Vec3 raw = cart.getPosition(partialTick);
        Vec3 track = cart.getPos(raw.x, raw.y, raw.z);
        Vec3 position = track == null ? raw : track;
        if (track != null) {
            Vec3 front = cart.getPosOffs(raw.x, raw.y, raw.z, 0.3);
            Vec3 back = cart.getPosOffs(raw.x, raw.y, raw.z, -0.3);
            if (front != null && back != null)
                position = new Vec3(track.x, (front.y + back.y) / 2, track.z);
        }
        return MinecartCouplerAnchors.center(cart, partialTick, position)
                .add(0, LiftClientSupport.getRenderOffset(cart, partialTick), 0);
    }

    private static void renderHook(
            VertexConsumer vertices, PoseStack.Pose pose, Frame frame, int reach, int light) {
        cuboid(vertices, pose, frame, -reach, -3, 1, -1, 1, Skin.BAR, light);
        cuboid(vertices, pose, frame, -3, -1, 2, -2, 2, Skin.BASE, light);
        cuboid(vertices, pose, frame, -1, 1, 1, 0, 2, Skin.JAW, light);
    }

    private static void cuboid(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Frame frame,
            int rear,
            int front,
            int height,
            int left,
            int right,
            Skin skin,
            int light) {
        Vec3 a = frame.point(rear, -height, left);
        Vec3 b = frame.point(front, -height, left);
        Vec3 c = frame.point(front, -height, right);
        Vec3 d = frame.point(rear, -height, right);
        Vec3 e = frame.point(rear, height, left);
        Vec3 f = frame.point(front, height, left);
        Vec3 g = frame.point(front, height, right);
        Vec3 h = frame.point(rear, height, right);
        quad(vertices, pose, h, g, f, e, frame.up, skin.top, light);
        quad(vertices, pose, a, b, c, d, frame.up.scale(-1), skin.top, light);
        quad(vertices, pose, b, a, e, f, frame.side.scale(-1), skin.side, light);
        quad(vertices, pose, d, c, g, h, frame.side, skin.side, light);
        quad(vertices, pose, a, d, h, e, frame.along.scale(-1), skin.end, light);
        quad(vertices, pose, c, b, f, g, frame.along, skin.end, light);
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            Vec3 normal,
            Tile tile,
            int light) {
        float u0 = (tile.x + 0.01F) / 16;
        float v0 = (tile.y + 0.01F) / 16;
        float u1 = (tile.x + tile.width - 0.01F) / 16;
        float v1 = (tile.y + tile.height - 0.01F) / 16;
        vertex(vertices, pose, a, normal, u0, v1, light);
        vertex(vertices, pose, b, normal, u1, v1, light);
        vertex(vertices, pose, c, normal, u1, v0, light);
        vertex(vertices, pose, d, normal, u0, v0, light);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            Vec3 point,
            Vec3 normal,
            float u,
            float v,
            int light) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    /** Stores the attachment frame used to draw a coupling. */
    private record Frame(Vec3 center, Vec3 along, Vec3 up, Vec3 side) {
        private Vec3 point(int x, int y, int z) {
            return center.add(along.scale(x * PIXEL))
                    .add(up.scale(y * PIXEL))
                    .add(side.scale(z * PIXEL));
        }
    }

    /** Stores one visible coupling texture section. */
    private record Tile(int x, int y, int width, int height) {}

    /** Stores the textures used by a coupling material. */
    private record Skin(Tile top, Tile side, Tile end) {
        private static final Tile BAR_SIDE = new Tile(0, 2, 2, 2);
        private static final Tile JAW_SIDE = new Tile(2, 4, 2, 2);
        private static final Skin BAR = new Skin(new Tile(0, 0, 2, 2), BAR_SIDE, BAR_SIDE);
        private static final Skin BASE =
                new Skin(new Tile(4, 0, 2, 4), new Tile(2, 0, 2, 4), new Tile(6, 0, 4, 4));
        private static final Skin JAW = new Skin(new Tile(4, 4, 2, 2), JAW_SIDE, JAW_SIDE);
    }
}
