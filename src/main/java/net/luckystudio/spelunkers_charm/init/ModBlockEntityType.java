// Block entity type.
package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.block.custom.GeyserBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.blaster.BlasterBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.minecrate.MineCrateBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.tank.TankBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntityType {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SpelunkersCharm.MOD_ID);

    public static final Supplier<BlockEntityType<BlasterBlockEntity>> BLASTER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "blaster_block_entity",
                    () ->
                            BlockEntityType.Builder.of(
                                            BlasterBlockEntity::new, ModBlocks.BLASTER.get())
                                    .build(null));

    public static final Supplier<BlockEntityType<GeyserBlockEntity>> GEYSER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "geyser_block_entity",
                    () ->
                            BlockEntityType.Builder.of(
                                            GeyserBlockEntity::new,
                                            ModBlocks.BASALT_GEYSER.get(),
                                            ModBlocks.DEEPSLATE_GEYSER.get())
                                    .build(null));

    public static final Supplier<BlockEntityType<CartLoaderBlockEntity>> CART_LOADER =
            BLOCK_ENTITIES.register(
                    "cart_loader",
                    () ->
                            BlockEntityType.Builder.of(
                                            CartLoaderBlockEntity::new, ModBlocks.CART_LOADER.get())
                                    .build(null));
    public static final Supplier<BlockEntityType<MineCrateBlockEntity>> MINE_CRATE =
            BLOCK_ENTITIES.register(
                    "mine_crate",
                    () ->
                            BlockEntityType.Builder.of(
                                            MineCrateBlockEntity::new, ModBlocks.MINE_CRATE.get())
                                    .build(null));
    public static final Supplier<BlockEntityType<TankBlockEntity>> TANK =
            BLOCK_ENTITIES.register(
                    "tank",
                    () ->
                            BlockEntityType.Builder.of(TankBlockEntity::new, ModBlocks.TANK.get())
                                    .build(null));

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CART_LOADER.get(),
                (loader, side) -> loader.itemHandler());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, MINE_CRATE.get(), (crate, side) -> crate.storage());
        event.registerEntity(
                Capabilities.ItemHandler.ENTITY_AUTOMATION,
                ModEntityType.MINE_CRATE_MINECART.get(),
                (cart, side) -> cart.storage());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, TANK.get(), (tank, side) -> tank.storage());
        event.registerEntity(
                Capabilities.FluidHandler.ENTITY,
                ModEntityType.TANK_MINECART.get(),
                (cart, side) -> cart.storage());
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        eventBus.addListener(ModBlockEntityType::registerCapabilities);
    }
}
