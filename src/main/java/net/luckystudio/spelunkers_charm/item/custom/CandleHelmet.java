package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CandleHelmet extends ArmorItem {

    private final ResourceLocation TEXTURE = SpelunkersCharm.id("textures/armor/mining_helmet.png");

    public CandleHelmet(Properties properties) {
        super(ArmorMaterials.CHAIN, Type.HELMET, properties);
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }
}
