package luckyowlstudios.mods.luckysspelunkerscharm.network.protocol;

import net.blay09.mods.balm.api.network.SyncConfigMessage;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharmConfig;

import static luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm.id;

public class ClientboundConfigMessage extends SyncConfigMessage<LuckysSpelunkersCharmConfig> {
    public static final CustomPacketPayload.Type<ClientboundConfigMessage> TYPE = new CustomPacketPayload.Type<>(id("config"));

    public ClientboundConfigMessage(LuckysSpelunkersCharmConfig yourModConfig) {
        super(TYPE, yourModConfig);
    }
}
