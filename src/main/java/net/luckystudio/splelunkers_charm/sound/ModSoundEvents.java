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
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, SpelunkersCharm.MODID);

    public static final Supplier<SoundEvent> RUMBLE = registerSoundEvent("rumble");
    public static final Supplier<SoundEvent> TREMOR = registerSoundEvent("tremor");
//    public static final Holder.Reference<SoundEvent> TREMOR = registerSoundHolder("tremor");

//    private static Holder.Reference<SoundEvent> registerSoundHolder(String name) {
//        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MODID, name);
//        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
//    }

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
