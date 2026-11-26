// Minecart motion hooks.
package net.luckystudio.spelunkers_charm.mixin;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractMinecart.class, priority = 1100, remap = false)
public abstract class MinecartMotionMixin extends net.minecraft.world.entity.Entity
        implements MinecartMotionVisuals {
    @Shadow private int lerpSteps;
    @Shadow private double lerpX;
    @Shadow private double lerpY;
    @Shadow private double lerpZ;
    @Shadow private double lerpYRot;
    @Shadow private double lerpXRot;
    @Shadow private net.minecraft.world.phys.Vec3 targetDeltaMovement;
    @Shadow private boolean onRails;
    @Unique private boolean spelunkersCharm$positionSeen;

    protected MinecartMotionMixin(
            net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Unique
    private static final EntityDataAccessor<Float> SPELUNKERS_BRAKING =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Integer> SPELUNKERS_REPLACED_CART =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);

    @Unique
    private static final EntityDataAccessor<Boolean> SPELUNKERS_AIRBORNE =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private static final EntityDataAccessor<Float> SPELUNKERS_FLIGHT_YAW =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Float> SPELUNKERS_FLIGHT_PITCH =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Integer> SPELUNKERS_HOOK_SIDES =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);

    @Unique
    private static final EntityDataAccessor<Float> SPELUNKERS_BODY_YAW =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.FLOAT);

    @Unique
    private static final EntityDataAccessor<Boolean> SPELUNKERS_FLIGHT_TRAIN =
            SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BOOLEAN);

    @Unique private final MinecartMotionState spelunkersCharm$motion = new MinecartMotionState();

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void spelunkersCharm$defineMotion(
            SynchedEntityData.Builder builder, CallbackInfo info) {
        builder.define(SPELUNKERS_BRAKING, 0.0F);
        builder.define(SPELUNKERS_REPLACED_CART, -1);
        builder.define(SPELUNKERS_AIRBORNE, false);
        builder.define(SPELUNKERS_FLIGHT_YAW, 0F);
        builder.define(SPELUNKERS_FLIGHT_PITCH, 0F);
        builder.define(SPELUNKERS_HOOK_SIDES, 0);
        builder.define(SPELUNKERS_BODY_YAW, Float.NaN);
        builder.define(SPELUNKERS_FLIGHT_TRAIN, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide()
                && key == SPELUNKERS_BODY_YAW
                && !spelunkersCharm$motion.clientBodyFacingSeen) {
            float facing = getEntityData().get(SPELUNKERS_BODY_YAW);
            if (Float.isFinite(facing))
                MinecartBodyOrientation.acceptServerFacing(
                        (AbstractMinecart) (Object) this, facing);
        }
        if (level().isClientSide()
                && (key == SPELUNKERS_FLIGHT_YAW || key == SPELUNKERS_FLIGHT_PITCH)) {
            spelunkersCharm$motion.physicalYaw =
                    Math.toRadians(getEntityData().get(SPELUNKERS_FLIGHT_YAW));
            spelunkersCharm$motion.physicalPitch =
                    Math.toRadians(getEntityData().get(SPELUNKERS_FLIGHT_PITCH));
            spelunkersCharm$motion.flightRotationReady = true;
        }
        if (key == SPELUNKERS_AIRBORNE) {
            boolean airborne = getEntityData().get(SPELUNKERS_AIRBORNE);
            if (level().isClientSide() && spelunkersCharm$motion.airborne && !airborne) {
                var track = MinecartTrack.current((AbstractMinecart) (Object) this);
                if (track != null)
                    MinecartBodyOrientation.land((AbstractMinecart) (Object) this, track);
            }
            spelunkersCharm$motion.airborne = airborne;
        }
        if (key == SPELUNKERS_BRAKING && level().isClientSide()) {
            float strength = getEntityData().get(SPELUNKERS_BRAKING);
            if (strength > 0) {
                spelunkersCharm$motion.clientBrakeStrength = strength;
                spelunkersCharm$motion.clientBrakeUntil = level().getGameTime() + 4;
            }
        }
        if (key == SPELUNKERS_REPLACED_CART && level().isClientSide())
            MinecartReplacementVisuals.accept((AbstractMinecart) (Object) this);
    }

    @Inject(method = "lerpTo", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$positionPacket(
            double x, double y, double z, float yaw, float pitch, int steps, CallbackInfo info) {
        spelunkersCharm$positionSeen = true;
        if (level().isClientSide()) {
            var motion = spelunkersCharm$motion;
            var target = new net.minecraft.world.phys.Vec3(x, y, z);
            long now = level().getGameTime();
            if (motion.clientPacketTarget != null) {
                motion.clientPacketVelocity =
                        target.subtract(motion.clientPacketTarget)
                                .scale(1.0 / Math.max(1, now - motion.clientPacketTime));
                motion.clientPacketRailVelocity =
                        MinecartInterpolation.railVelocity(
                                (AbstractMinecart) (Object) this,
                                motion.clientPacketTarget,
                                target,
                                Math.max(1, now - motion.clientPacketTime));
                double vertical = Math.abs(target.y - motion.clientPacketTarget.y);
                if (vertical > .001 && vertical < 4) {
                    double speed = vertical / Math.max(1, now - motion.clientPacketTime);
                    motion.clientPacketVerticalSpeed =
                            now <= motion.clientVerticalUntil
                                    ? Math.max(speed, motion.clientPacketVerticalSpeed)
                                    : speed;
                    motion.clientVerticalUntil = now + 4;
                }
                double distance = target.subtract(motion.clientPacketTarget).horizontalDistance();
                if (distance > 0.001 && distance < 1.2) {
                    double speed =
                            Math.min(.6, distance / Math.max(1, now - motion.clientPacketTime));
                    motion.clientPacketSpeed =
                            now <= motion.clientPacketUntil
                                    ? Math.max(speed, motion.clientPacketSpeed)
                                    : speed;
                    motion.clientPacketUntil = now + 4;
                }
            }
            motion.clientPacketTarget = target;
            motion.clientPacketTime = now;
            if (MinecartSpeedRules.managed((AbstractMinecart) (Object) this)) {
                var predicted =
                        MinecartInterpolation.predicted((AbstractMinecart) (Object) this, target);
                lerpX = predicted.x;
                lerpY = predicted.y;
                lerpZ = predicted.z;
                lerpSteps = 3;
                lerpYRot = yaw;
                lerpXRot = pitch;
                setDeltaMovement(targetDeltaMovement);
                info.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void spelunkersCharm$finishClientInterpolation(CallbackInfo info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        var motion = spelunkersCharm$motion;
        if (level().isClientSide()
                && motion.clientPacketTarget != null
                && MinecartSpeedRules.managed(cart)) {
            long age = level().getGameTime() - motion.clientPacketTime;
            if (age > 0 && age <= 2) {
                var target =
                        MinecartInterpolation.predicted(
                                cart,
                                motion.clientPacketTarget,
                                MinecartInterpolation.LEAD_TICKS + age);
                lerpX = target.x;
                lerpY = target.y;
                lerpZ = target.z;
                lerpSteps = 3;
            }
        }
        if (level().isClientSide()
                && spelunkersCharm$positionSeen
                && lerpSteps <= 0
                && MinecartSpeedRules.managed(cart)) {
            double distance =
                    position()
                            .distanceToSqr(new net.minecraft.world.phys.Vec3(lerpX, lerpY, lerpZ));
            if (distance > 0.0001 && distance < 36) lerpSteps = 3;
        }
    }

    @Inject(method = "moveAlongTrack", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$move(BlockPos pos, BlockState state, CallbackInfo info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (MinecartSpeedRules.managed(cart)) {
            if (!cart.level().isClientSide()) MinecartRailMotion.move(cart, pos, state);
            info.cancel();
        }
    }

    @Inject(method = "comeOffTrack", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$coastOffRails(CallbackInfo info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (MinecartSpeedRules.managed(cart)) {
            if (!cart.level().isClientSide()) MinecartRailMotion.moveOffTrack(cart);
            info.cancel();
        }
    }

    @Override
    public void push(double x, double y, double z) {
        if (!MinecartHandbrake.active((AbstractMinecart) (Object) this)
                || !MinecartTrainBrakes.onRail((AbstractMinecart) (Object) this))
            super.push(x, y, z);
    }

    @Override
    public void move(
            net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 movement) {
        if (!MinecartHandbrake.anchored((AbstractMinecart) (Object) this))
            super.move(type, movement);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void spelunkersCharm$saveBrake(net.minecraft.nbt.CompoundTag tag, CallbackInfo info) {
        if (spelunkersCharm$motion.visualYawReady)
            tag.putFloat("SpelunkersBodyYaw", spelunkersCharm$motion.visualYaw);
        tag.putBoolean("SpelunkersAirborne", spelunkersCharm$airborne());
        tag.putBoolean("SpelunkersFlightLaunched", spelunkersCharm$motion.flightLaunched);
        if (Double.isFinite(spelunkersCharm$motion.flightReferenceYaw))
            tag.putDouble("SpelunkersLaunchYaw", spelunkersCharm$motion.flightReferenceYaw);
        if (spelunkersCharm$motion.groundIdleTicks > 0)
            tag.putInt("SpelunkersGroundIdle", spelunkersCharm$motion.groundIdleTicks);
        if (spelunkersCharm$motion.flightRotationReady) {
            tag.putDouble("SpelunkersHingeYaw", spelunkersCharm$motion.physicalYaw);
            tag.putDouble("SpelunkersHingePitch", spelunkersCharm$motion.physicalPitch);
            tag.putDouble("SpelunkersPitchRate", spelunkersCharm$motion.pitchRate);
            tag.putDouble("SpelunkersYawRate", spelunkersCharm$motion.yawRate);
            tag.putInt("SpelunkersFlightSign", spelunkersCharm$motion.flightForwardSign);
        }
        var hooks = new net.minecraft.nbt.ListTag();
        spelunkersCharm$motion.hingeSides.forEach(
                (id, side) -> {
                    var hook = new net.minecraft.nbt.CompoundTag();
                    hook.putUUID("Peer", id);
                    hook.putInt("Side", side);
                    hooks.add(hook);
                });
        tag.put("SpelunkersHingeSides", hooks);
        if (Double.isFinite(spelunkersCharm$motion.fallPeak))
            tag.putDouble("SpelunkersFallPeak", spelunkersCharm$motion.fallPeak);
        if ((Object) this instanceof net.minecraft.world.entity.vehicle.MinecartFurnace) {
            tag.putBoolean("SpelunkersHandbrake", spelunkersCharm$motion.handbrake);
            tag.putDouble("SpelunkersBrakeSpeed", spelunkersCharm$motion.handbrakeSpeed);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void spelunkersCharm$loadBrake(net.minecraft.nbt.CompoundTag tag, CallbackInfo info) {
        spelunkersCharm$motion.recoverBodyFacing =
                !tag.contains("SpelunkersBodyYaw")
                        && !tag.getBoolean("SpelunkersAirborne")
                        && !((Object) this instanceof LocomotiveMinecart);
        if (tag.contains("SpelunkersBodyYaw")) {
            spelunkersCharm$motion.visualYaw =
                    spelunkersCharm$motion.visualYawOld = tag.getFloat("SpelunkersBodyYaw");
            spelunkersCharm$motion.visualYawReady = true;
            spelunkersCharm$setBodyYaw(spelunkersCharm$motion.visualYaw);
        }
        spelunkersCharm$setAirborne(tag.getBoolean("SpelunkersAirborne"));
        spelunkersCharm$motion.flightLaunched = tag.getBoolean("SpelunkersFlightLaunched");
        spelunkersCharm$motion.flightReferenceYaw =
                tag.contains("SpelunkersLaunchYaw")
                        ? tag.getDouble("SpelunkersLaunchYaw")
                        : Double.NaN;
        spelunkersCharm$motion.groundIdleTicks =
                Math.clamp(tag.getInt("SpelunkersGroundIdle"), 0, 1200);
        spelunkersCharm$motion.flightRotationReady = tag.contains("SpelunkersHingeYaw");
        spelunkersCharm$motion.physicalYaw = tag.getDouble("SpelunkersHingeYaw");
        spelunkersCharm$motion.physicalPitch =
                Math.clamp(tag.getDouble("SpelunkersHingePitch"), -Math.PI / 2, Math.PI / 2);
        spelunkersCharm$motion.pitchRate = tag.getDouble("SpelunkersPitchRate");
        spelunkersCharm$motion.yawRate = tag.getDouble("SpelunkersYawRate");
        spelunkersCharm$motion.flightForwardSign =
                tag.getInt("SpelunkersFlightSign") == -1 ? -1 : 1;
        spelunkersCharm$motion.hingeSides.clear();
        for (var entry : tag.getList("SpelunkersHingeSides", net.minecraft.nbt.Tag.TAG_COMPOUND))
            if (entry instanceof net.minecraft.nbt.CompoundTag hook && hook.hasUUID("Peer"))
                spelunkersCharm$motion.hingeSides.put(
                        hook.getUUID("Peer"), hook.getInt("Side") == -1 ? -1 : 1);
        spelunkersCharm$motion.fallPeak =
                tag.contains("SpelunkersFallPeak")
                        ? tag.getDouble("SpelunkersFallPeak")
                        : Double.NaN;
        spelunkersCharm$motion.flightPitch = getXRot();
        spelunkersCharm$motion.handbrake = tag.getBoolean("SpelunkersHandbrake");
        spelunkersCharm$motion.handbrakeSpeed = tag.getDouble("SpelunkersBrakeSpeed");
        spelunkersCharm$motion.handbrakeAnchor =
                spelunkersCharm$motion.handbrake && spelunkersCharm$motion.handbrakeSpeed < 0.003
                        ? position()
                        : null;
    }

    @Inject(method = "getMaxSpeedWithRail", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$speed(CallbackInfoReturnable<Double> info) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (MinecartSpeedRules.managed(cart)) info.setReturnValue(MinecartSpeedRules.limit(cart));
    }

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;lerpPositionAndRotationStep(IDDDDD)V"))
    private void spelunkersCharm$followRails(
            AbstractMinecart cart,
            int steps,
            double x,
            double y,
            double z,
            double yaw,
            double pitch) {
        var target = new net.minecraft.world.phys.Vec3(x, y, z);
        var next = MinecartInterpolation.next(cart, target, steps);
        if (next == null && MinecartSpeedRules.managed(cart))
            next = MinecartInterpolation.fallback(cart, target, steps);
        if (next != null)
            spelunkersCharm$motion.clientVerticalStep = Math.abs(next.y - cart.getY());
        lerpPositionAndRotationStep(steps, x, y, z, yaw, pitch);
        if (next != null) {
            cart.setPos(next.x, next.y, next.z);
            if (steps <= 1 && next.distanceToSqr(target) > 0.0001) lerpSteps = 2;
        }
    }

    @Inject(method = "lerpMotion", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$rememberServerVelocity(
            double x, double y, double z, CallbackInfo info) {
        spelunkersCharm$motion.clientVelocity = new net.minecraft.world.phys.Vec3(x, y, z);
        spelunkersCharm$motion.clientVelocitySeen = true;
        if (level().isClientSide()
                && MinecartSpeedRules.managed((AbstractMinecart) (Object) this)) {
            targetDeltaMovement = spelunkersCharm$motion.clientVelocity;
            setDeltaMovement(targetDeltaMovement);
            info.cancel();
            if (targetDeltaMovement.lengthSqr() < 1e-8
                    && spelunkersCharm$motion.clientPacketTarget != null) {
                var target = spelunkersCharm$motion.clientPacketTarget;
                lerpX = target.x;
                lerpY = target.y;
                lerpZ = target.z;
                lerpSteps = 3;
            }
        }
    }

    @Inject(method = "getPos", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$freeRenderPosition(
            double x,
            double y,
            double z,
            CallbackInfoReturnable<net.minecraft.world.phys.Vec3> info) {
        var cart = (AbstractMinecart) (Object) this;
        if (level().isClientSide() && MinecartSpeedRules.managed(cart)) {
            if (MinecartInterpolation.freeRender(cart)) {
                info.setReturnValue(null);
                return;
            }
            var position =
                    MinecartInterpolation.renderPosition(
                            cart, new net.minecraft.world.phys.Vec3(x, y, z), 0);
            if (position != null) info.setReturnValue(position);
        }
    }

    @Inject(method = "getPosOffs", at = @At("HEAD"), cancellable = true)
    private void spelunkersCharm$freeRenderOffset(
            double x,
            double y,
            double z,
            double offset,
            CallbackInfoReturnable<net.minecraft.world.phys.Vec3> info) {
        var cart = (AbstractMinecart) (Object) this;
        if (level().isClientSide() && MinecartSpeedRules.managed(cart)) {
            if (MinecartInterpolation.freeRender(cart)) {
                info.setReturnValue(null);
                return;
            }
            var position =
                    MinecartInterpolation.renderPosition(
                            cart, new net.minecraft.world.phys.Vec3(x, y, z), offset);
            if (position != null) info.setReturnValue(position);
        }
    }

    @Inject(method = "setCurrentCartSpeedCapOnRail", at = @At("HEAD"))
    private void spelunkersCharm$externalCap(float cap, CallbackInfo info) {
        if (Float.isFinite(cap) && cap >= 0) spelunkersCharm$motion.externalSpeedCap = cap;
    }

    @Override
    public MinecartMotionState spelunkersCharm$motion() {
        return spelunkersCharm$motion;
    }

    @Override
    public float spelunkersCharm$braking() {
        float strength = getEntityData().get(SPELUNKERS_BRAKING);
        return level().isClientSide()
                        && level().getGameTime() <= spelunkersCharm$motion.clientBrakeUntil
                ? Math.max(strength, spelunkersCharm$motion.clientBrakeStrength)
                : strength;
    }

    @Override
    public void spelunkersCharm$setBraking(float strength) {
        ((AbstractMinecart) (Object) this).getEntityData().set(SPELUNKERS_BRAKING, strength);
    }

    @Override
    public int spelunkersCharm$replacedCartId() {
        return getEntityData().get(SPELUNKERS_REPLACED_CART);
    }

    @Override
    public void spelunkersCharm$setReplacedCartId(int id) {
        getEntityData().set(SPELUNKERS_REPLACED_CART, id);
    }

    @Override
    public void spelunkersCharm$setOnRails(boolean rails) {
        onRails = rails;
    }

    @Override
    public boolean spelunkersCharm$airborne() {
        return getEntityData().get(SPELUNKERS_AIRBORNE);
    }

    @Override
    public float spelunkersCharm$flightYaw() {
        return getEntityData().get(SPELUNKERS_FLIGHT_YAW);
    }

    @Override
    public float spelunkersCharm$flightPitch() {
        return getEntityData().get(SPELUNKERS_FLIGHT_PITCH);
    }

    @Override
    public void spelunkersCharm$setFlightPose(float yaw, float pitch) {
        getEntityData().set(SPELUNKERS_FLIGHT_YAW, yaw);
        getEntityData().set(SPELUNKERS_FLIGHT_PITCH, pitch);
    }

    @Override
    public int spelunkersCharm$hookSides() {
        return getEntityData().get(SPELUNKERS_HOOK_SIDES);
    }

    @Override
    public float spelunkersCharm$bodyYaw() {
        return getEntityData().get(SPELUNKERS_BODY_YAW);
    }

    @Override
    public boolean spelunkersCharm$flightTrain() {
        return getEntityData().get(SPELUNKERS_FLIGHT_TRAIN);
    }

    @Override
    public void spelunkersCharm$setFlightTrain(boolean active) {
        getEntityData().set(SPELUNKERS_FLIGHT_TRAIN, active);
    }

    @Override
    public void spelunkersCharm$setBodyYaw(float yaw) {
        getEntityData().set(SPELUNKERS_BODY_YAW, net.minecraft.util.Mth.wrapDegrees(yaw));
    }

    @Override
    public void spelunkersCharm$setHookSides(int sides) {
        getEntityData().set(SPELUNKERS_HOOK_SIDES, sides);
    }

    @Override
    public void spelunkersCharm$setAirborne(boolean airborne) {
        spelunkersCharm$motion.airborne = airborne;
        getEntityData().set(SPELUNKERS_AIRBORNE, airborne);
    }
}
