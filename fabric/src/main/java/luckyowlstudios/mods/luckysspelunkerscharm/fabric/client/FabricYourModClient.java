package luckyowlstudios.mods.luckysspelunkerscharm.fabric.client;

import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;
import luckyowlstudios.mods.luckysspelunkerscharm.client.YourModClient;

public class FabricYourModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(LuckysSpelunkersCharm.MOD_ID, EmptyLoadContext.INSTANCE, YourModClient::initialize);
    }
}
