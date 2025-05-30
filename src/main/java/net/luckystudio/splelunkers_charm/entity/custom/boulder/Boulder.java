package net.luckystudio.splelunkers_charm.entity.custom.boulder;

import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Mainly copied from the armor stand entity, but without the armor slots and other humanoid features.
public class Boulder extends LivingEntity {

    public Boulder(EntityType<Boulder> boulderEntityType, Level level) {
        super(boulderEntityType, level);
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return null;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return null;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {

    }

    @Override
    public HumanoidArm getMainArm() {
        return null;
    }
}
