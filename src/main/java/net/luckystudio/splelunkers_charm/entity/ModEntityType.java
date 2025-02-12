package net.luckystudio.splelunkers_charm.entity;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.entity.custom.tremor.Tremor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntityType {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SpelunkersCharm.MODID);

    public static final Supplier<EntityType<Tremor>> TREMOR =
            ENTITY_TYPES.register("tremor", () -> EntityType.Builder.of(Tremor::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(0)
//                    .noSummon()
                    .build("tremor"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
