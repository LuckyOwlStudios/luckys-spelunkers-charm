package net.luckystudio.splelunkers_charm;

import net.luckystudio.splelunkers_charm.entity.ModEntityType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@EventBusSubscriber(modid = SpelunkersCharm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpelunkersCharmClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        // How did we find this? We searched all projects in Minecraft to see where the SnowBall projectile was registered in client and copied this.
        EntityRenderers.register(ModEntityType.ROCK.get(), ThrownItemRenderer::new);
        EntityRenderers.register(ModEntityType.DEEPSLATE_ROCK.get(), ThrownItemRenderer::new);
        EntityRenderers.register(ModEntityType.DRIPSTONE_ROCK.get(), ThrownItemRenderer::new);
        EntityRenderers.register(ModEntityType.BASALT_ROCK.get(), ThrownItemRenderer::new);
    }
}
