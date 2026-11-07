package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.worldgen.feature.placement.ConfigFilter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPlacementModifiers {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, SpelunkersCharm.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ConfigFilter>> CONFIG_FILTER =
            PLACEMENT_MODIFIERS.register("config", () -> () -> ConfigFilter.CODEC);

    public static void register(IEventBus eventBus) {
        PLACEMENT_MODIFIERS.register(eventBus);
    }
}
