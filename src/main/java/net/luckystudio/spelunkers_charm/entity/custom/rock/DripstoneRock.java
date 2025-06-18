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

public class DripstoneRock extends AbstractThrowableRock {

    public DripstoneRock(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public DripstoneRock(Level level, LivingEntity shooter) {
        super(ModEntityType.DRIPSTONE_ROCK.get(), shooter, level);
    }

    public DripstoneRock(Level level, double x, double y, double z) {
        super(ModEntityType.DRIPSTONE_ROCK.get(), x, y, z, level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModBlocks.DRIPSTONE_ROCK.asItem();
    }

    @Override
    protected BlockState getCopiedBlockState() {
        return Blocks.DRIPSTONE_BLOCK.defaultBlockState();
    }
}
