package net.luckystudio.splelunkers_charm.init;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.custom.boulder.entity.Boulder;
import net.luckystudio.splelunkers_charm.entity.custom.lift.large.LargeLift;
import net.luckystudio.splelunkers_charm.entity.custom.lift.medium.MediumLift;
import net.luckystudio.splelunkers_charm.entity.custom.lift.small.SmallLift;
import net.luckystudio.splelunkers_charm.entity.custom.minecart.BlockMinecart;
import net.luckystudio.splelunkers_charm.entity.custom.rock.*;
import net.luckystudio.splelunkers_charm.events.tremor.Tremor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntityType {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SpelunkersCharm.MOD_ID);

    public static final Supplier<EntityType<Tremor>>TREMOR =
            ENTITY_TYPES.register("tremor", () -> EntityType.Builder.of(Tremor::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(0)
                    .build("tremor"));

    public static final Supplier<EntityType<Boulder>> BOULDER =
            ENTITY_TYPES.register("boulder", () -> EntityType.Builder.<Boulder>of(Boulder::new, MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .build("boulder"));

    public static final Supplier<EntityType<BlockMinecart>> BLOCK_MINECART =
            ENTITY_TYPES.register("block_minecart", () -> EntityType.Builder.<BlockMinecart>of(BlockMinecart::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F)
                    .clientTrackingRange(8)
                    .build("block_minecart"));

    public static final Supplier<EntityType<SmallLift>> SMALL_LIFT =
            ENTITY_TYPES.register("small_lift", () -> EntityType.Builder.<SmallLift>of(SmallLift::new, MobCategory.MISC)
                    .sized(3.0F, 1.0F).eyeHeight(0.5F).clientTrackingRange(10)
                    .build("small_lift"));

    public static final Supplier<EntityType<MediumLift>> MEDIUM_LIFT =
            ENTITY_TYPES.register("medium_lift", () -> EntityType.Builder.<MediumLift>of(MediumLift::new, MobCategory.MISC)
                    .sized(5.0F, 1.0F).eyeHeight(0.5F).clientTrackingRange(10)
                    .build("medium_lift"));

    public static final Supplier<EntityType<LargeLift>> LARGE_LIFT =
            ENTITY_TYPES.register("large_lift", () -> EntityType.Builder.<LargeLift>of(LargeLift::new, MobCategory.MISC)
                    .sized(7.0F, 1.0F).eyeHeight(0.5F).clientTrackingRange(10)
                    .build("large_lift"));

    public static final Supplier<EntityType<Rock>> ROCK =
            ENTITY_TYPES.register("rock", () -> EntityType.Builder.<Rock>of(Rock::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("rock")
    );

    public static final Supplier<EntityType<IceBall>> ICE_BALL =
            ENTITY_TYPES.register("ice_ball", () -> EntityType.Builder.<IceBall>of(IceBall::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("ice_ball")
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
