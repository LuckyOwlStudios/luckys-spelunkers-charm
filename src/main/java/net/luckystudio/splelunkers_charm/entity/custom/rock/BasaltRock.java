package net.luckystudio.splelunkers_charm.entity.custom.rock;

import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.entity.ModEntityType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class BasaltRock extends ThrowableItemProjectile {

    public BasaltRock(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public BasaltRock(Level level, LivingEntity shooter) {
        super(ModEntityType.BASALT_ROCK.get(), shooter, level);
    }

    public BasaltRock(Level level, double x, double y, double z) {
        super(ModEntityType.BASALT_ROCK.get(), x, y, z, level);
    }
    @Override
    protected @NotNull Item getDefaultItem() {
        return ModBlocks.BASALT_ROCK.asItem();
    }

    private ParticleOptions getParticle() {
        ItemStack itemstack = this.getItem();
        // Change to BlockParticleOption with stone block
        return !itemstack.isEmpty() && !itemstack.is(this.getDefaultItem())
                ? new ItemParticleOption(ParticleTypes.ITEM, itemstack)
                : new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BASALT.defaultBlockState());
    }

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
        this.level().playSound(this, this.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 1.5F);
        this.level().broadcastEntityEvent(this, (byte)3);
        this.discard();
    }
}
