package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.Nullable;

public class MiningHelmet extends ArmorItem {

    private final ResourceLocation TEXTURE = SpelunkersCharm.id("textures/armor/mining_helmet.png");

    public MiningHelmet(Properties properties) {
        super(ArmorMaterials.GOLD, Type.HELMET, properties.attributes(
                ItemAttributeModifiers.builder()
                        .add(Attributes.ARMOR, new AttributeModifier(SpelunkersCharm.id("armor"), 3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                        .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, new AttributeModifier(SpelunkersCharm.id("explosion_resistance"), 2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                        .build()
        ));
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }
}