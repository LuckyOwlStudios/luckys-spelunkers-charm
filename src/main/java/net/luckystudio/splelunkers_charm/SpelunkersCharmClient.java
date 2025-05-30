package net.luckystudio.splelunkers_charm;

import net.luckystudio.splelunkers_charm.entity.ModEntityType;
import net.luckystudio.splelunkers_charm.entity.custom.boulder.BoulderModel;
import net.luckystudio.splelunkers_charm.entity.custom.lift.large.LargeLiftModel;
import net.luckystudio.splelunkers_charm.entity.custom.lift.large.LargeLiftRenderer;
import net.luckystudio.splelunkers_charm.entity.custom.lift.medium.MediumLiftModel;
import net.luckystudio.splelunkers_charm.entity.custom.lift.medium.MediumLiftRenderer;
import net.luckystudio.splelunkers_charm.entity.custom.lift.small.SmallLiftModel;
import net.luckystudio.splelunkers_charm.entity.custom.lift.small.SmallLiftRenderer;
import net.luckystudio.splelunkers_charm.item.ModItems;
import net.luckystudio.splelunkers_charm.item.custom.MiningHelmetModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpelunkersCharmClient {

    public static final ModelLayerLocation SMALL_LIFT = new ModelLayerLocation(SpelunkersCharm.id("small_lift"), "main");
    public static final ModelLayerLocation MEDIUM_LIFT = new ModelLayerLocation(SpelunkersCharm.id("medium_lift"), "main");
    public static final ModelLayerLocation LARGE_LIFT = new ModelLayerLocation(SpelunkersCharm.id("large_lift"), "main");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {

    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityType.SMALL_LIFT.get(), SmallLiftRenderer::new);
        event.registerEntityRenderer(ModEntityType.MEDIUM_LIFT.get(), MediumLiftRenderer::new);
        event.registerEntityRenderer(ModEntityType.LARGE_LIFT.get(), LargeLiftRenderer::new);
        event.registerEntityRenderer(ModEntityType.ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityType.DEEPSLATE_ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityType.DRIPSTONE_ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityType.BASALT_ROCK.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MiningHelmetModel.LAYER_LOCATION, MiningHelmetModel::createBodyLayer);
        event.registerLayerDefinition(BoulderModel.LAYER_LOCATION, BoulderModel::createBodyLayer);
        event.registerLayerDefinition(SMALL_LIFT, SmallLiftModel::createBodyLayer);
        event.registerLayerDefinition(MEDIUM_LIFT, MediumLiftModel::createBodyLayer);
        event.registerLayerDefinition(LARGE_LIFT, LargeLiftModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        registerArmorModel(event, ModItems.MINING_HELMET.get(), MiningHelmetModel.LAYER_LOCATION);
    }

    private static void registerArmorModel(RegisterClientExtensionsEvent event, Item item, ModelLayerLocation layer) {
        event.registerItem(new IClientItemExtensions() {

            @Override
            @OnlyIn(Dist.CLIENT)
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> defaultModel) {
                ModelPart part = Minecraft.getInstance().getEntityModels().bakeLayer(layer);
                return new HumanoidModel<>(part);
            }
        }, item);
    }
}
