package luckyowlstudios.mods.luckysspelunkerscharm.fabric;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.fabricmc.api.ModInitializer;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;

public class FabricYourMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Balm.initializeMod(LuckysSpelunkersCharm.MOD_ID, EmptyLoadContext.INSTANCE, LuckysSpelunkersCharm::initialize);
    }
}
