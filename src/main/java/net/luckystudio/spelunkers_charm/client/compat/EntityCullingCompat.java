// Entity culling compat.
package net.luckystudio.spelunkers_charm.client.compat;

import com.mojang.logging.LogUtils;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

import org.slf4j.Logger;

import java.util.Set;

@EventBusSubscriber(
        modid = SpelunkersCharm.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class EntityCullingCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private EntityCullingCompat() {}

    @SubscribeEvent
    public static void loadComplete(FMLLoadCompleteEvent event) {
        if (ModList.get().isLoaded("entityculling"))
            event.enqueueWork(EntityCullingCompat::registerLifts);
    }

    @SuppressWarnings("unchecked")
    private static void registerLifts() {
        try {
            Class<?> mod = Class.forName("dev.tr7zw.entityculling.EntityCullingModBase");
            Object instance = mod.getField("instance").get(null);
            if (instance == null) {
                LOGGER.warn(
                        "EntityCulling did not initialize its client; the lift tick exemption could not be registered");
                return;
            }

            // EntityCulling 1.12 exposes its runtime tick whitelist separately from render culling.
            Set<EntityType<?>> tickWhitelist =
                    (Set<EntityType<?>>) mod.getField("tickCullWhitelists").get(instance);
            tickWhitelist.add(ModEntityType.SMALL_LIFT.get());
            tickWhitelist.add(ModEntityType.MEDIUM_LIFT.get());
            tickWhitelist.add(ModEntityType.LARGE_LIFT.get());
            tickWhitelist.add(EntityType.MINECART);
            tickWhitelist.add(EntityType.CHEST_MINECART);
            tickWhitelist.add(EntityType.FURNACE_MINECART);
            tickWhitelist.add(EntityType.HOPPER_MINECART);
            tickWhitelist.add(EntityType.TNT_MINECART);
            tickWhitelist.add(EntityType.SPAWNER_MINECART);
            tickWhitelist.add(EntityType.COMMAND_BLOCK_MINECART);
            tickWhitelist.add(ModEntityType.LOCOMOTIVE.get());
            tickWhitelist.add(ModEntityType.BLOCK_MINECART.get());
            tickWhitelist.add(ModEntityType.JUKEBOX_MINECART.get());
            tickWhitelist.add(ModEntityType.MINE_CRATE_MINECART.get());
            tickWhitelist.add(ModEntityType.TANK_MINECART.get());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            LOGGER.warn("Could not register the lift tick exemption with EntityCulling", error);
        }
    }
}
