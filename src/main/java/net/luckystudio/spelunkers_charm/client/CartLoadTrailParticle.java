// Cart load trail particle.
package net.luckystudio.spelunkers_charm.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public final class CartLoadTrailParticle extends TextureSheetParticle {
    private CartLoadTrailParticle(
            ClientLevel level, double x, double y, double z, double speed, SpriteSet sprites) {
        super(level, x, y, z);
        yd = speed;
        friction = 1.0F;
        lifetime = 5;
        quadSize = 0.18F + random.nextFloat() * 0.12F;
        setSize(0.04F, 0.04F);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (onGround) remove();
        setAlpha(1.0F - age / (float) lifetime);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Creates particles for cart loading trails. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double vx,
                double vy,
                double vz) {
            return new CartLoadTrailParticle(level, x, y, z, vy, sprites);
        }
    }
}
