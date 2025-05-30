package net.luckystudio.splelunkers_charm.sound;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, SpelunkersCharm.MOD_ID);

    public static final Supplier<SoundEvent> RUMBLE_GENERIC = registerSoundEvent("rumble_generic");
    public static final Supplier<SoundEvent> RUMBLE_ICY = registerSoundEvent("rumble_icy");
    public static final Supplier<SoundEvent> TREMOR_GENERIC = registerSoundEvent("tremor_generic");
    public static final Supplier<SoundEvent> TREMOR_ICY = registerSoundEvent("tremor_icy");

    public static Supplier<SoundEvent> GEYSER_CHARGE = registerSoundEvent("geyser_charge");
    public static Supplier<SoundEvent> GEYSER_ERUPT_WATER = registerSoundEvent("geyser_erupt_water");

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
