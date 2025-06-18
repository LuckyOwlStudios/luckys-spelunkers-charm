package net.luckystudio.spelunkers_charm.entity.custom.rock;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class AbstractThrowableRock extends ThrowableItemProjectile {

    public AbstractThrowableRock(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public AbstractThrowableRock(EntityType<? extends ThrowableItemProjectile> entityType, double x, double y, double z, Level level) {
        super(entityType, x, y, z, level);
    }

    public AbstractThrowableRock(EntityType<? extends ThrowableItemProjectile> entityType, LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
    }

    public static AbstractThrowableRock createRock(
            ServerLevel level,
            double x,
            double y,
            double z,
            AbstractThrowableRock.Type type,
            @Nullable Player player
    ) {
        AbstractThrowableRock abstractRock = (AbstractThrowableRock)(switch (type) {
            case DEEPSLATE_ROCK -> new DeepslateRock(level, x, y, z);
            case DRIPSTONE_ROCK -> new DripstoneRock(level, x, y, z);
            case BASALT_ROCK -> new BasaltRock(level, x, y, z);
            case ICE_BALL -> new IceBall(level, x, y, z);
            default -> new Rock(level, x, y, z);
        });
        return abstractRock;
    }

    public static enum Type {
        ROCK,
        DEEPSLATE_ROCK,
        DRIPSTONE_ROCK,
        BASALT_ROCK,
        ICE_BALL
    }

    private ParticleOptions getParticle() {
        ItemStack itemstack = this.getItem();
        // Change to BlockParticleOption with stone block
        return !itemstack.isEmpty() && !itemstack.is(this.getDefaultItem())
                ? new ItemParticleOption(ParticleTypes.ITEM, itemstack)
                : new BlockParticleOption(ParticleTypes.BLOCK, getCopiedBlockState());
    }

    protected abstract BlockState getCopiedBlockState();

    /**
     * Handles an entity event received from a {@link net.minecraft.network.protocol.game.ClientboundEntityEventPacket}.
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions particleoptions = this.getParticle();

            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particleoptions, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        entity.hurt(this.damageSources().thrown(this, this.getOwner()), 3.0F);
        rockHit();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        Level level = this.level();
        BlockState blockstate = this.level().getBlockState(result.getBlockPos());
        if (!this.level().isClientSide) {
            if (blockstate.getBlock() == Blocks.GLASS) {
                level.destroyBlock(result.getBlockPos(), false);
            } else {
                rockHit();
            }
        }
    }

    private void rockHit() {
        this.level().playSound(this, this.blockPosition(), getCopiedBlockState().getSoundType(this.level(), this.blockPosition(), null).getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        this.level().broadcastEntityEvent(this, (byte)3);
        this.discard();
    }
}
