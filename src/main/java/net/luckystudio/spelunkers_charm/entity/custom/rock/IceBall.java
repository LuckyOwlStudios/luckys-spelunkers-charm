package net.luckystudio.spelunkers_charm.entity.custom.rock;

import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class IceBall extends AbstractThrowableRock {

    public IceBall(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public IceBall(Level level, LivingEntity shooter) {
        super(ModEntityType.ICE_BALL.get(), shooter, level);
    }

    public IceBall(Level level, double x, double y, double z) {
        super(ModEntityType.ICE_BALL.get(), x, y, z, level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModBlocks.ICE_BALL.asItem();
    }

    @Override
    protected BlockState getCopiedBlockState() {
        return Blocks.PACKED_ICE.defaultBlockState();
    }
}
