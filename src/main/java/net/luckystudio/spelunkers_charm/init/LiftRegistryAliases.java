package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Keep existing worlds and inventory stacks readable after the rack ID cleanup. */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class LiftRegistryAliases {
    private LiftRegistryAliases() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerAliases(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)
                || event.getRegistryKey().equals(Registries.ITEM)) {
            event.getRegistry()
                    .addAlias(
                            SpelunkersCharm.id("lift_track"),
                            SpelunkersCharm.id("basic_lift_track"));
            event.getRegistry()
                    .addAlias(
                            SpelunkersCharm.id("wooden_lift_track"),
                            SpelunkersCharm.id("directional_lift_track"));
        }
    }
}
