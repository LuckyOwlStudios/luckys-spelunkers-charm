// Minecart lantern mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.MinecartLanternVisuals;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractMinecart.class, remap = false)
public abstract class MinecartLanternMixin implements MinecartLanternVisuals {
    @Unique
    private static final EntityDataAccessor<Integer> SPELUNKERS_LANTERN =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void spelunkersCharm$defineLantern(
            SynchedEntityData.Builder builder, CallbackInfo info) {
        builder.define(SPELUNKERS_LANTERN, CartLantern.NONE.ordinal());
    }

    @Override
    public CartLantern spelunkersCharm$getLantern() {
        return CartLantern.byId(
                ((AbstractMinecart) (Object) this).getEntityData().get(SPELUNKERS_LANTERN));
    }

    @Override
    public void spelunkersCharm$setLantern(CartLantern lantern) {
        ((AbstractMinecart) (Object) this)
                .getEntityData()
                .set(SPELUNKERS_LANTERN, lantern.ordinal());
    }
}
