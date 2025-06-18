package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class CandleHelmet extends ArmorItem {

    private final ResourceLocation TEXTURE = SpelunkersCharm.id("textures/armor/candle_helmet.png");

    public CandleHelmet(Properties properties) {
        super(ArmorMaterials.CHAIN, Type.HELMET, properties);
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof LivingEntity living)) return;
        if (!level.isClientSide) return;
        Random random = new Random();
        if (random.nextFloat() >= 0.1f) return; // 10% chance to spawn particles

        Vec3 position = living.trackingPosition();
        float yOffset = living.getBbHeight() + 0.25f;

        // Spawn particles
        level.addParticle(ParticleTypes.SMALL_FLAME, position.x, position.y + yOffset, position.z, 0, 0.01, 0);
        level.addParticle(ParticleTypes.SMOKE, position.x, position.y + yOffset, position.z, 0, 0.01, 0);
    }
}