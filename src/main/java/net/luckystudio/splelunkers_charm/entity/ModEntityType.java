package net.luckystudio.splelunkers_charm.entity;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.entity.custom.rock.*;
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

    public static final Supplier<EntityType<Rock>> ROCK =
            ENTITY_TYPES.register("rock", () -> EntityType.Builder.<Rock>of(Rock::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("rock")
    );

    public static final Supplier<EntityType<DeepslateRock>> DEEPSLATE_ROCK =
            ENTITY_TYPES.register("deepslate_rock", () -> EntityType.Builder.<DeepslateRock>of(DeepslateRock::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("deepslate_rock")
    );

    public static final Supplier<EntityType<DripstoneRock>> DRIPSTONE_ROCK =
            ENTITY_TYPES.register("dripstone_rock", () -> EntityType.Builder.<DripstoneRock>of(DripstoneRock::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("dripstone_rock")
    );

    public static final Supplier<EntityType<BasaltRock>> BASALT_ROCK =
            ENTITY_TYPES.register("basalt_rock", () -> EntityType.Builder.<BasaltRock>of(BasaltRock::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("basalt_rock")
    );

    public static final Supplier<EntityType<Tremor>> TREMOR =
            ENTITY_TYPES.register("tremor", () -> EntityType.Builder.of(Tremor::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(0)
                    .build("tremor"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
