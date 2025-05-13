package luckyowlstudios.mods.luckysspelunkerscharm.sounds;

import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.sound.BalmSounds;
import net.minecraft.sounds.SoundEvent;

public class ModSoundEvents {

    public static DeferredObject<SoundEvent> GEYSER_CHARGE;
    public static DeferredObject<SoundEvent> GEYSER_ERUPT_WATER;

    public static void initialize(BalmSounds sounds) {
        GEYSER_CHARGE = sounds.register(LuckysSpelunkersCharm.id("geyser_charge"));
        GEYSER_ERUPT_WATER = sounds.register(LuckysSpelunkersCharm.id("geyser_erupt_water"));
    }
}
