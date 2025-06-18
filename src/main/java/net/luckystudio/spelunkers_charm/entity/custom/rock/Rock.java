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

public class Rock extends AbstractThrowableRock {

    public Rock(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public Rock(Level level, LivingEntity shooter) {
        super(ModEntityType.ROCK.get(), shooter, level);
    }

    public Rock(Level level, double x, double y, double z) {
        super(ModEntityType.ROCK.get(), x, y, z, level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModBlocks.ROCK.asItem();
    }

    @Override
    protected BlockState getCopiedBlockState() {
        return Blocks.STONE.defaultBlockState();
    }
}
