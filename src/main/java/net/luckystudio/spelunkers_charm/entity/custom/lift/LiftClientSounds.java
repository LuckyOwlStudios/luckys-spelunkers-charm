package net.luckystudio.spelunkers_charm.entity.custom.lift;

import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Map;

/** One positional OpenAL loop per moving lift, with short attack/release ramps. */
@EventBusSubscriber(modid = "spelunkers_charm", value = Dist.CLIENT)
public final class LiftClientSounds {
    private static final Map<Integer, Loop> SOUNDS = new HashMap<>();

    private LiftClientSounds() {}

    public static void update(AbstractLift lift, double actualY) {
        Minecraft mc = Minecraft.getInstance();
        Loop sound = SOUNDS.get(lift.getId());
        if (sound != null && (sound.isStopped() || sound.lift != lift)) {
            mc.getSoundManager().stop(sound);
            SOUNDS.remove(lift.getId());
            sound = null;
        }
        if (sound == null
                && Math.abs(actualY) > 1.0E-6
                && travelling(lift)
                && mc.getCameraEntity() != null
                && mc.getCameraEntity().distanceToSqr(lift) <= 48 * 48) {
            sound = new Loop(lift);
            SOUNDS.put(lift.getId(), sound);
            mc.getSoundManager().play(sound);
        }
        if (sound != null && Math.abs(actualY) > 1.0E-6) sound.lastMovement = lift.tickCount;
    }

    private static boolean travelling(AbstractLift lift) {
        return lift.getMovementType() == MovementType.UP
                || lift.getMovementType() == MovementType.DOWN;
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        for (Loop sound : SOUNDS.values()) Minecraft.getInstance().getSoundManager().stop(sound);
        SOUNDS.clear();
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        final AbstractLift lift;
        int lastMovement;

        Loop(AbstractLift lift) {
            super(
                    ModSoundEvents.LIFT_LOOP.get(),
                    SoundSource.BLOCKS,
                    SoundInstance.createUnseededRandom());
            this.lift = lift;
            this.lastMovement = lift.tickCount;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.pitch = lift.getBbWidth() < 4.0F ? 1.2F : lift.getBbWidth() < 6.0F ? 1.0F : 0.8F;
            this.x = lift.getX();
            this.y = lift.getY() + .5;
            this.z = lift.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public boolean canPlaySound() {
            return !lift.isSilent();
        }

        @Override
        public void tick() {
            if (lift.isRemoved() || Minecraft.getInstance().level != lift.level()) {
                stop();
                return;
            }
            this.x = lift.getX();
            this.y = lift.getY() + .5;
            this.z = lift.getZ();
            boolean running = travelling(lift) && lift.tickCount - lastMovement <= 3;
            float target = running ? 0.75F : 0.0F;
            this.volume = Mth.approach(this.volume, target, 0.25F);
            if (!running && this.volume <= 0.0F) stop();
        }
    }
}
