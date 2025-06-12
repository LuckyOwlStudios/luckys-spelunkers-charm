package net.luckystudio.splelunkers_charm.init;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.custom.blaster.BlasterBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntityType {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SpelunkersCharm.MOD_ID);

    public static final Supplier<BlockEntityType<BlasterBlockEntity>> BLASTER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("blaster_block_entity", () -> BlockEntityType.Builder.of(
                    BlasterBlockEntity::new, ModBlocks.BLASTER.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
