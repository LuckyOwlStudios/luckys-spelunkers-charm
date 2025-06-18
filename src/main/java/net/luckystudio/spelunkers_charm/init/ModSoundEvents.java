package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, SpelunkersCharm.MOD_ID);

    // Ambient Sounds
    public static final Supplier<SoundEvent> RUMBLE_GENERIC = registerSoundEvent("rumble_generic");
    public static final Supplier<SoundEvent> RUMBLE_ICY = registerSoundEvent("rumble_icy");
    public static final Supplier<SoundEvent> TREMOR_GENERIC = registerSoundEvent("tremor_generic");
    public static final Supplier<SoundEvent> TREMOR_ICY = registerSoundEvent("tremor_icy");
    public static final Supplier<SoundEvent> SPIDER_CAVE_ADDITIONS = registerSoundEvent("spider_cave_additions");

    // Block Sounds
    public static final Supplier<SoundEvent> GEYSER_CHARGE = registerSoundEvent("geyser_charge");
    public static final Supplier<SoundEvent> GEYSER_ERUPT_WATER = registerSoundEvent("geyser_erupt_water");
    public static final Supplier<SoundEvent> SPIDER_EGG_CRACK = registerSoundEvent("spider_egg_crack");


    // Entity Sounds
    public static final Supplier<SoundEvent> LIFT_START = registerSoundEvent("lift_start");
    public static final Supplier<SoundEvent> LIFT_STOP = registerSoundEvent("lift_stop");
    public static final Supplier<SoundEvent> BOULDER_LAND = registerSoundEvent("boulder_land");

    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SpelunkersCharm.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
