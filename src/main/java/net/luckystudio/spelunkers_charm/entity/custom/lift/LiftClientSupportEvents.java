package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "spelunkers_charm", value = Dist.CLIENT)
public final class LiftClientSupportEvents {
    private LiftClientSupportEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterEntityInterpolation(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level != null) LiftClientSupport.alignRemoteBounds(level);
    }
}
