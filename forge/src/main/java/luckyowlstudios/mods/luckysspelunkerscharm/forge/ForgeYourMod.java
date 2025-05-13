package luckyowlstudios.mods.luckysspelunkerscharm.forge;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.EmptyLoadContext;
import net.blay09.mods.balm.api.client.BalmClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;
import luckyowlstudios.mods.luckysspelunkerscharm.client.YourModClient;

@Mod(LuckysSpelunkersCharm.MOD_ID)
public class ForgeYourMod {

    public ForgeYourMod(FMLJavaModLoadingContext context) {
        Balm.initializeMod(LuckysSpelunkersCharm.MOD_ID, EmptyLoadContext.INSTANCE, LuckysSpelunkersCharm::initialize);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> BalmClient.initializeMod(LuckysSpelunkersCharm.MOD_ID, EmptyLoadContext.INSTANCE, YourModClient::initialize));
    }

}
