package luckyowlstudios.mods.luckysspelunkerscharm.network;

import net.blay09.mods.balm.api.network.BalmNetworking;
import net.blay09.mods.balm.api.network.SyncConfigMessage;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharmConfig;
import luckyowlstudios.mods.luckysspelunkerscharm.network.protocol.ClientboundConfigMessage;

public class ModNetworking {

    public static void initialize(BalmNetworking networking) {
        SyncConfigMessage.register(ClientboundConfigMessage.TYPE,
                ClientboundConfigMessage.class,
                ClientboundConfigMessage::new,
                LuckysSpelunkersCharmConfig.class,
                LuckysSpelunkersCharmConfig::new);
    }
}
