// Train sounds and particles.
package net.luckystudio.spelunkers_charm.client;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplerAnchors;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartMotionVisuals;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, value = Dist.CLIENT)
public final class RailwayClientEffects {
    private static final Map<AbstractMinecart, Playback> PLAYBACK = new WeakHashMap<>();

    private RailwayClientEffects() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        Iterator<Map.Entry<AbstractMinecart, Playback>> iterator = PLAYBACK.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (mc.level != entry.getKey().level() || entry.getKey().isRemoved()) {
                if (entry.getValue().brake != null)
                    mc.getSoundManager().stop(entry.getValue().brake);
                iterator.remove();
            }
        }
        if (mc.level == null || mc.player == null) return;
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractMinecart cart)) continue;
            if (net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartSpeedRules.managed(
                    cart))
                net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartBodyOrientation.yaw(
                        cart, 1);
            if (cart.distanceToSqr(mc.player) > 48 * 48) continue;
            Playback playback = PLAYBACK.computeIfAbsent(cart, ignored -> new Playback());
            float braking = ((MinecartMotionVisuals) cart).spelunkersCharm$braking();
            if (braking > 0.01F) {
                if (playback.brake == null || playback.brake.isStopped()) {
                    playback.brake = new BrakeSound(cart);
                    mc.getSoundManager().play(playback.brake);
                }
                sparks(mc, cart, braking);
            }
            Vec3 position = cart.position();
            double speed =
                    playback.previous == null
                            ? 0
                            : position.subtract(playback.previous).horizontalDistance();
            playback.previous = position;
            if (cart instanceof LocomotiveMinecart engine && engine.isBurning())
                steam(mc, engine, playback, speed);
            else playback.steamPhase = 0;
            if (cart instanceof LocomotiveMinecart locomotive
                    && (locomotive.isBurning() || locomotive.emergencyBrake())
                    && speed > 0.008) {
                double fraction = Math.clamp(speed / 0.5, 0, 1);
                playback.pistonPhase += LocomotiveMinecart.pistonFrequency(speed) / 20.0;
                if (playback.pistonPhase >= 1) {
                    playback.pistonPhase -= 1;
                    mc.getSoundManager()
                            .play(
                                    new SimpleSoundInstance(
                                            ModSoundEvents.LOCOMOTIVE_PISTON.get(),
                                            SoundSource.NEUTRAL,
                                            (float) (0.30 + 0.30 * fraction),
                                            (float) (0.69 + 0.21 * fraction),
                                            SoundInstance.createUnseededRandom(),
                                            cart.getX(),
                                            cart.getY(),
                                            cart.getZ()));
                }
            } else playback.pistonPhase = 0;
        }
    }

    private static void steam(
            Minecraft mc, LocomotiveMinecart cart, Playback playback, double speed) {
        double fraction = Math.clamp(speed / .45, 0, 1);
        playback.steamPhase += .12 + fraction * 1.25;
        Vec3 up = MinecartCouplerAnchors.up(cart, 1);
        Vec3 outlet = cart.position().add(0, .375, 0).add(up.scale(.74));
        while (playback.steamPhase >= 1) {
            playback.steamPhase--;
            double scatter = (cart.getRandom().nextDouble() - .5) * .06;
            Particle particle =
                    mc.particleEngine.createParticle(
                            ParticleTypes.CLOUD,
                            outlet.x + scatter,
                            outlet.y,
                            outlet.z - scatter,
                            0,
                            0,
                            0);
            if (particle != null) {
                particle.setColor(.96F, .97F, 1.0F);
                particle.scale((float) (.65 + .40 * fraction));
                particle.setLifetime(30 + cart.getRandom().nextInt(16));
                Vec3 drift =
                        up.scale(.035 + .06 * fraction).add(cart.getDeltaMovement().scale(-.12));
                particle.setParticleSpeed(drift.x, drift.y, drift.z);
            }
        }
    }

    private static void sparks(Minecraft mc, AbstractMinecart cart, float strength) {
        Vec3 velocity = cart.getDeltaMovement();
        Vec3 along =
                velocity.horizontalDistanceSqr() > 0.00001
                        ? new Vec3(velocity.x, 0, velocity.z).normalize()
                        : Vec3.directionFromRotation(0, cart.getYRot());
        Vec3 side = new Vec3(-along.z, 0, along.x);
        for (int wheel = 0; wheel < 2; wheel++)
            for (int edge : new int[] {-1, 1}) {
                Vec3 point =
                        cart.position()
                                .add(along.scale(wheel == 0 ? -0.27 : 0.27))
                                .add(side.scale(edge * 0.34));
                Particle spark =
                        mc.particleEngine.createParticle(
                                ParticleTypes.ELECTRIC_SPARK,
                                point.x,
                                point.y + 0.06,
                                point.z,
                                0,
                                0,
                                0);
                if (spark != null) {
                    spark.setLifetime(7 + cart.getRandom().nextInt(5));
                    spark.setColor(1, 0.65F + cart.getRandom().nextFloat() * 0.15F, 0.15F);
                    double scatter = (cart.getRandom().nextDouble() - 0.5) * 0.06;
                    spark.setParticleSpeed(
                            -along.x * (0.04 + strength * 0.06) + side.x * edge * 0.045 + scatter,
                            0.07 + strength * 0.07,
                            -along.z * (0.04 + strength * 0.06) + side.z * edge * 0.045 - scatter);
                }
            }
    }

    /** Tracks one cart's active sounds and particle timing. */
    private static final class Playback {
        private Vec3 previous;
        private double pistonPhase;
        private double steamPhase;
        private BrakeSound brake;
    }

    /** Plays a brake loop that fades with braking strength. */
    private static final class BrakeSound extends AbstractTickableSoundInstance {
        private final AbstractMinecart cart;
        private float fade;

        private BrakeSound(AbstractMinecart cart) {
            super(
                    cart instanceof LocomotiveMinecart
                            ? ModSoundEvents.LOCOMOTIVE_BRAKE.get()
                            : ModSoundEvents.CART_BRAKE.get(),
                    SoundSource.NEUTRAL,
                    SoundInstance.createUnseededRandom());
            this.cart = cart;
            looping = true;
            delay = 0;
            volume = 0.01F;
            pitch = cart instanceof LocomotiveMinecart ? 0.78F : 1.12F;
            x = cart.getX();
            y = cart.getY();
            z = cart.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (cart.isRemoved() || mc.level != cart.level()) {
                stop();
                return;
            }
            float strength = ((MinecartMotionVisuals) cart).spelunkersCharm$braking();
            fade = strength > 0 ? Math.min(1, fade + 0.5F) : Math.max(0, fade - 0.3F);
            if (fade == 0) {
                stop();
                return;
            }
            volume = fade * (0.22F + strength * 0.45F);
            pitch = (cart instanceof LocomotiveMinecart ? 0.72F : 1.05F) + strength * 0.16F;
            x = cart.getX();
            y = cart.getY();
            z = cart.getZ();
        }
    }
}
