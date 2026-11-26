// Data components.
package net.luckystudio.spelunkers_charm.init;

import com.mojang.serialization.Codec;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(
                    Registries.DATA_COMPONENT_TYPE, SpelunkersCharm.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LIGHT =
            register("light", builder -> builder.persistent(Codec.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>>
            CRATE_CONTENTS =
                    register("crate_contents", builder -> builder.persistent(CustomData.CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>>
            TANK_CONTENTS =
                    register("tank_contents", builder -> builder.persistent(CustomData.CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(
                name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
