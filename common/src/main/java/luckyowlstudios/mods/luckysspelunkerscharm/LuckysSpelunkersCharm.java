package luckyowlstudios.mods.luckysspelunkerscharm;

import luckyowlstudios.mods.luckysspelunkerscharm.sounds.ModSoundEvents;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import luckyowlstudios.mods.luckysspelunkerscharm.block.ModBlocks;
import luckyowlstudios.mods.luckysspelunkerscharm.item.ModItems;
import luckyowlstudios.mods.luckysspelunkerscharm.network.ModNetworking;

public class LuckysSpelunkersCharm {

    public static final Logger logger = LoggerFactory.getLogger(LuckysSpelunkersCharm.class);

    public static final String MOD_ID = "luckysspelunkerscharm";

    public static void initialize() {
        LuckysSpelunkersCharmConfig.initialize();
        ModNetworking.initialize(Balm.getNetworking());
        ModBlocks.initialize(Balm.getBlocks());
        ModItems.initialize(Balm.getItems());
        ModSoundEvents.initialize(Balm.getSounds());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

}
