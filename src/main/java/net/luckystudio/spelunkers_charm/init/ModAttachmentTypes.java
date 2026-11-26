// Attachment types.
package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SpelunkersCharm.MOD_ID);

    /** The lantern hanging on a minecart, if any. Saved with the cart. */
    public static final Supplier<AttachmentType<CartLantern>> CART_LANTERN =
            ATTACHMENT_TYPES.register(
                    "cart_lantern",
                    () ->
                            AttachmentType.builder(() -> CartLantern.NONE)
                                    .serialize(CartLantern.CODEC)
                                    .build());

    public static final Supplier<AttachmentType<GlobalPos>> CART_LANTERN_LIGHT =
            ATTACHMENT_TYPES.register(
                    "cart_lantern_light",
                    () ->
                            AttachmentType.builder(
                                            () -> GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO))
                                    .serialize(GlobalPos.CODEC)
                                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
