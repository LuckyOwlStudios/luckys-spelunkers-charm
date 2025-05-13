package luckyowlstudios.mods.luckysspelunkerscharm.neoforge.client;

import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;
import luckyowlstudios.mods.luckysspelunkerscharm.client.YourModClient;

@Mod(value = LuckysSpelunkersCharm.MOD_ID, dist = Dist.CLIENT)
public class NeoForgeYourModClient {

    public NeoForgeYourModClient(IEventBus modEventBus) {
        final var context = new NeoForgeLoadContext(modEventBus);
        BalmClient.initialize(LuckysSpelunkersCharm.MOD_ID, context, YourModClient::initialize);
    }
}
