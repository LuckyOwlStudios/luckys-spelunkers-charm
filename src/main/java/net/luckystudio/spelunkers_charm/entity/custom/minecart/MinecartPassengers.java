// Minecart passengers.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import org.jetbrains.annotations.Nullable;

public final class MinecartPassengers {
    private MinecartPassengers() {}

    public static boolean sharesTrain(Entity first, Entity second) {
        if (!(first.level() instanceof ServerLevel level) || first.level() != second.level())
            return false;
        AbstractMinecart a = cart(first);
        if (a == null) return false;
        AbstractMinecart b = cart(second);
        return b != null && MinecartCouplings.get(level).sameTrain(a.getUUID(), b.getUUID());
    }

    @Nullable
    static AbstractMinecart cart(Entity entity) {
        for (Entity current = entity; current != null; current = current.getVehicle())
            if (current instanceof AbstractMinecart cart) return cart;
        return null;
    }
}
