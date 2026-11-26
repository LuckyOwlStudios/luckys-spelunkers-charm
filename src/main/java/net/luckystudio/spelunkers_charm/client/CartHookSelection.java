// Cart hook selection.
package net.luckystudio.spelunkers_charm.client;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.item.custom.CartCouplingHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, value = Dist.CLIENT)
public final class CartHookSelection {
    private CartHookSelection() {}

    public static AbstractMinecart selected(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;
        var id = CartCouplingHook.selectedCart(mc.player.getMainHandItem(), mc.level);
        if (id == null) id = CartCouplingHook.selectedCart(mc.player.getOffhandItem(), mc.level);
        if (id == null) return null;
        for (var entity : mc.level.entitiesForRendering())
            if (entity instanceof AbstractMinecart cart
                    && cart.isAlive()
                    && id.equals(cart.getUUID())) return cart;
        return null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var mc = Minecraft.getInstance();
        var cart = selected(mc);
        if (cart == null) return;
        var p = cart.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(p.x - camera.x, p.y + cart.getBbHeight() + .35 - camera.y, p.z - camera.z);
        pose.mulPose(event.getCamera().rotation());
        var buffers = mc.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(RenderType.debugQuads());
        var matrix = pose.last().pose();
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12, b = (i + 1) * Math.PI / 12;
            vertices.addVertex(matrix, 0, 0, 0).setColor(255, 220, 24, 255);
            vertices.addVertex(matrix, (float) Math.cos(a) * .065f, (float) Math.sin(a) * .065f, 0)
                    .setColor(255, 220, 24, 255);
            vertices.addVertex(matrix, (float) Math.cos(b) * .065f, (float) Math.sin(b) * .065f, 0)
                    .setColor(255, 220, 24, 255);
            vertices.addVertex(matrix, 0, 0, 0).setColor(255, 220, 24, 255);
        }
        buffers.endBatch(RenderType.debugQuads());
        pose.popPose();
    }
}
