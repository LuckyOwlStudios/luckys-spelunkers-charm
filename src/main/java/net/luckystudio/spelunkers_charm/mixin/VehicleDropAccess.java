// Vehicle drop access.
package net.luckystudio.spelunkers_charm.mixin;

import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = VehicleEntity.class, remap = false)
public interface VehicleDropAccess {
    @Invoker("getDropItem")
    Item spelunkersCharm$dropItem();
}
