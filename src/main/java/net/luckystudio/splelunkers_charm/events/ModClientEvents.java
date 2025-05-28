package net.luckystudio.splelunkers_charm.events;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.item.ModItems;
import net.luckystudio.splelunkers_charm.item.custom.MiningHelmetModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = SpelunkersCharm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MiningHelmetModel.LAYER_LOCATION, MiningHelmetModel::createBodyLayer);
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
