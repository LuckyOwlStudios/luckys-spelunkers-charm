package luckyowlstudios.mods.luckysspelunkerscharm.neoforge;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;

@Mod(LuckysSpelunkersCharm.MOD_ID)
public class NeoForgeLuckysSpelunkersCharm {

    public NeoForgeLuckysSpelunkersCharm(IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modEventBus);
        Balm.initializeMod(LuckysSpelunkersCharm.MOD_ID, context, LuckysSpelunkersCharm::initialize);
    }
}
