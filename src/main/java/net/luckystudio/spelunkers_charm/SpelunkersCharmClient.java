package net.luckystudio.spelunkers_charm;

import net.luckystudio.spelunkers_charm.block.custom.blaster.BlasterScreen;
import net.luckystudio.spelunkers_charm.block.custom.blaster.ModMenuTypes;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.block.custom.boulder.entity.BoulderModel;
import net.luckystudio.spelunkers_charm.block.custom.boulder.entity.BoulderRenderer;
import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLiftModel;
import net.luckystudio.spelunkers_charm.entity.custom.lift.large.LargeLiftRenderer;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLiftModel;
import net.luckystudio.spelunkers_charm.entity.custom.lift.medium.MediumLiftRenderer;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLiftModel;
import net.luckystudio.spelunkers_charm.entity.custom.lift.small.SmallLiftRenderer;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.override.OverrideMinecartRenderer;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.override.NewMinecartModel;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.luckystudio.spelunkers_charm.item.custom.MiningHelmetModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
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
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpelunkersCharmClient {

    public static final ModelLayerLocation NEW_MINECART_MODEL = new ModelLayerLocation(SpelunkersCharm.id("new_minecart_model"), "main");
    public static final ModelLayerLocation SMALL_LIFT = new ModelLayerLocation(SpelunkersCharm.id("small_lift"), "main");
    public static final ModelLayerLocation MEDIUM_LIFT = new ModelLayerLocation(SpelunkersCharm.id("medium_lift"), "main");
    public static final ModelLayerLocation LARGE_LIFT = new ModelLayerLocation(SpelunkersCharm.id("large_lift"), "main");
    public static final ModelLayerLocation BOULDER = new ModelLayerLocation(SpelunkersCharm.id("boulder"), "main");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {

    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {

        // Overrides
        event.registerEntityRenderer(EntityType.MINECART, OverrideMinecartRenderer::new);
        event.registerEntityRenderer(EntityType.CHEST_MINECART, OverrideMinecartRenderer::new);
        event.registerEntityRenderer(EntityType.FURNACE_MINECART, OverrideMinecartRenderer::new);
        event.registerEntityRenderer(EntityType.TNT_MINECART, OverrideMinecartRenderer::new);
        event.registerEntityRenderer(EntityType.HOPPER_MINECART, OverrideMinecartRenderer::new);
        event.registerEntityRenderer(EntityType.COMMAND_BLOCK_MINECART, OverrideMinecartRenderer::new);
        event.registerEntityRenderer(EntityType.SPAWNER_MINECART, OverrideMinecartRenderer::new);

        // New Entity Renderers
        event.registerEntityRenderer(ModEntityType.BLOCK_MINECART.get(), OverrideMinecartRenderer::new);
        event.registerEntityRenderer(ModEntityType.SMALL_LIFT.get(), SmallLiftRenderer::new);
        event.registerEntityRenderer(ModEntityType.MEDIUM_LIFT.get(), MediumLiftRenderer::new);
        event.registerEntityRenderer(ModEntityType.LARGE_LIFT.get(), LargeLiftRenderer::new);
        event.registerEntityRenderer(ModEntityType.BOULDER.get(), BoulderRenderer::new);
        event.registerEntityRenderer(ModEntityType.ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityType.ICE_BALL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityType.DEEPSLATE_ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityType.DRIPSTONE_ROCK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityType.BASALT_ROCK.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MiningHelmetModel.LAYER_LOCATION, MiningHelmetModel::createBodyLayer);
        event.registerLayerDefinition(BOULDER, BoulderModel::createBodyLayer);
        event.registerLayerDefinition(NEW_MINECART_MODEL, NewMinecartModel::createBodyLayer);
        event.registerLayerDefinition(SMALL_LIFT, SmallLiftModel::createBodyLayer);
        event.registerLayerDefinition(MEDIUM_LIFT, MediumLiftModel::createBodyLayer);
        event.registerLayerDefinition(LARGE_LIFT, LargeLiftModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.BLASTER_MENU.get(), BlasterScreen::new);
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
