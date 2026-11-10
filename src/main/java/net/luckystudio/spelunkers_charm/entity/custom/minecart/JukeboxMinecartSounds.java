package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.WeakHashMap;

/** Client only. Plays a jukebox minecart's song so that it follows the cart. */
public final class JukeboxMinecartSounds {

    /** The last play id started for each cart, so one disc never starts two songs. */
    private static final Map<JukeboxMinecart, Integer> STARTED = new WeakHashMap<>();

    private JukeboxMinecartSounds() {
    }

    public static void play(JukeboxMinecart cart) {
        int playId = cart.getPlayId();
        Integer last = STARTED.get(cart);
        if (last != null && last == playId) return;

        cart.getSong().ifPresent(song -> {
            STARTED.put(cart, playId);
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getSoundManager().play(new CartSongInstance(cart, song.value().soundEvent().value(), playId));
            if (minecraft.player != null && minecraft.player.distanceToSqr(cart) < 64.0 * 64.0) {
                minecraft.gui.setNowPlaying(song.value().description());
            }
        });
    }

    private static class CartSongInstance extends AbstractTickableSoundInstance {
        private final JukeboxMinecart cart;
        private final int playId;

        CartSongInstance(JukeboxMinecart cart, SoundEvent sound, int playId) {
            super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.cart = cart;
            this.playId = playId;
            this.looping = false;
            this.delay = 0;
            // Same range as a jukebox: heard up to 64 blocks away.
            this.volume = 4.0F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.x = cart.getX();
            this.y = cart.getY();
            this.z = cart.getZ();
        }

        @Override
        public void tick() {
            if (this.cart.isRemoved() || !this.cart.isPlaying() || this.cart.getPlayId() != this.playId) {
                this.stop();
                return;
            }
            this.x = this.cart.getX();
            this.y = this.cart.getY();
            this.z = this.cart.getZ();
        }
    }
}
