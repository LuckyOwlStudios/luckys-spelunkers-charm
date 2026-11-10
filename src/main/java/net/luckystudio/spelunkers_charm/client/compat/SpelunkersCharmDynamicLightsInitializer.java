package net.luckystudio.spelunkers_charm.client.compat;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.ClientCartData;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * LambDynamicLights support, loaded by LambDynamicLights itself through the entrypoint in neoforge.mods.toml.
 * Minecarts with a lantern hung on them glow as a smooth dynamic light.
 */
public class SpelunkersCharmDynamicLightsInitializer implements DynamicLightsInitializer {

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        context.entityLightSourceManager().onRegisterEvent().register(registerContext -> {
            for (EntityType<?> type : minecartTypes()) {
                registerContext.register(type, CartLanternLuminance.INSTANCE);
            }
        });
    }

    @Override
    @SuppressWarnings("removal")
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
        // Old entrypoint, replaced by the one above.
    }

    private static List<EntityType<?>> minecartTypes() {
        return List.of(
                EntityType.MINECART,
                EntityType.CHEST_MINECART,
                EntityType.FURNACE_MINECART,
                EntityType.TNT_MINECART,
                EntityType.HOPPER_MINECART,
                EntityType.COMMAND_BLOCK_MINECART,
                EntityType.SPAWNER_MINECART,
                ModEntityType.BLOCK_MINECART.get(),
                ModEntityType.LOCOMOTIVE.get(),
                ModEntityType.JUKEBOX_MINECART.get()
        );
    }

    /** Light from the lantern hung on a minecart. */
    public static final class CartLanternLuminance implements EntityLuminance {
        public static final CartLanternLuminance INSTANCE = new CartLanternLuminance();
        public static final Type TYPE = Type.registerSimple(SpelunkersCharm.id("minecart_lantern"), INSTANCE);

        private CartLanternLuminance() {
        }

        @Override
        public @NotNull Type type() {
            return TYPE;
        }

        @Override
        public int getLuminance(@NotNull ItemLightSourceManager itemLightSourceManager, @NotNull Entity entity) {
            if (entity instanceof AbstractMinecart cart) {
                return ClientCartData.getLantern(cart.getId()).lightLevel();
            }
            return 0;
        }
    }
}
