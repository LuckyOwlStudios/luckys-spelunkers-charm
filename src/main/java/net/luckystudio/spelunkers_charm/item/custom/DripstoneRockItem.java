package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.entity.custom.rock.DripstoneRock;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class DripstoneRockItem extends BlockItem implements ProjectileItem {
    public DripstoneRockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITCH_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            DripstoneRock dripstoneRock = new DripstoneRock(level, player);
            dripstoneRock.setItem(itemstack);
            dripstoneRock.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.0F, 1.0F);
            level.addFreshEntity(dripstoneRock);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        itemstack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    public @NotNull Projectile asProjectile(@NotNull Level level, Position pos, @NotNull ItemStack stack, @NotNull Direction direction) {
        DripstoneRock dripstoneRock = new DripstoneRock(level, pos.x(), pos.y(), pos.z());
        dripstoneRock.setItem(stack);
        return dripstoneRock;
    }
}
