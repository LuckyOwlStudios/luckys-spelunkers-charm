// Spelunkers guide item.
package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.client.guide.GuideBookScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SpelunkersGuideItem extends Item {
    public SpelunkersGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) GuideBookScreen.open();
        return InteractionResultHolder.sidedSuccess(
                player.getItemInHand(hand), level.isClientSide());
    }
}
