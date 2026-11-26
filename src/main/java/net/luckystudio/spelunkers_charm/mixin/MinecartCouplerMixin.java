// Minecart coupler mixin.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplerPhysics;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplingVisuals;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartPassengers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractMinecart.class, remap = false)
public abstract class MinecartCouplerMixin implements MinecartCouplingVisuals {
    @Unique
    private static final EntityDataAccessor<Integer> SPELUNKERS_FIRST_COUPLING =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);

    @Unique
    private static final EntityDataAccessor<Integer> SPELUNKERS_SECOND_COUPLING =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void spelunkersCharm$defineCouplings(
            SynchedEntityData.Builder builder, CallbackInfo info) {
        builder.define(SPELUNKERS_FIRST_COUPLING, -1);
        builder.define(SPELUNKERS_SECOND_COUPLING, -1);
    }

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void spelunkersCharm$useCartBuffers(Entity entity, CallbackInfo info) {
        if (MinecartPassengers.sharesTrain((AbstractMinecart) (Object) this, entity)) {
            info.cancel();
            return;
        }
        if (entity instanceof AbstractMinecart peer
                && MinecartCouplerPhysics.replacesPush((AbstractMinecart) (Object) this, peer)) {
            info.cancel();
        }
    }

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$allowTrainFolds(
            Entity entity, CallbackInfoReturnable<Boolean> info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (entity instanceof AbstractMinecart peer
                && cart.level() instanceof ServerLevel level
                && MinecartCouplings.get(level).sameTrain(cart.getUUID(), peer.getUUID()))
            info.setReturnValue(false);
    }

    @Override
    public int spelunkersCharm$firstCouplingId() {
        return ((AbstractMinecart) (Object) this).getEntityData().get(SPELUNKERS_FIRST_COUPLING);
    }

    @Override
    public int spelunkersCharm$secondCouplingId() {
        return ((AbstractMinecart) (Object) this).getEntityData().get(SPELUNKERS_SECOND_COUPLING);
    }

    @Override
    public void spelunkersCharm$setCouplingIds(int first, int second) {
        SynchedEntityData data = ((AbstractMinecart) (Object) this).getEntityData();
        data.set(SPELUNKERS_FIRST_COUPLING, first);
        data.set(SPELUNKERS_SECOND_COUPLING, second);
    }
}
