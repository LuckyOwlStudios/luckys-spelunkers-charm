package net.luckystudio.splelunkers_charm.entity.custom.rock;

import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.luckystudio.splelunkers_charm.init.ModEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DeepslateRock extends AbstractThrowableRock {

    public DeepslateRock(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public DeepslateRock(Level level, LivingEntity shooter) {
        super(ModEntityType.DEEPSLATE_ROCK.get(), shooter, level);
    }

    public DeepslateRock(Level level, double x, double y, double z) {
        super(ModEntityType.DEEPSLATE_ROCK.get(), x, y, z, level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModBlocks.DEEPSLATE_ROCK.asItem();
    }

    @Override
    protected BlockState getCopiedBlockState() {
        return Blocks.DEEPSLATE.defaultBlockState();
    }
}
