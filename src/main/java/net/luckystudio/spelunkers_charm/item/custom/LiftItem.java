package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftPlacement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/** Placement at a floor center or directly onto a complete ring of racks. */
public class LiftItem extends Item {
    final AbstractLift.Type type;

    public LiftItem(AbstractLift.Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.PASS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        LiftPlacement.Result result = LiftPlacement.find(level, context, type);
        AbstractLift lift = result.lift();
        if (lift == null) {
            if (player != null)
                player.displayClientMessage(
                        Component.translatable(
                                result.occupiedShaft()
                                        ? "message.spelunkers_charm.lift_shaft_occupied"
                                        : "message.spelunkers_charm.lift_tracks"),
                        true);
            return InteractionResult.FAIL;
        }
        if (player != null
                && !player.mayUseItemAt(lift.blockPosition(), context.getClickedFace(), stack))
            return InteractionResult.FAIL;
        if (!level.addFreshEntity(lift)) return InteractionResult.FAIL;
        level.gameEvent(GameEvent.ENTITY_PLACE, lift.position(), GameEvent.Context.of(player));
        if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_item"));
        tooltip.add(
                Component.translatable(
                        "tooltip.spelunkers_charm.lift_"
                                + type.name().toLowerCase(java.util.Locale.ROOT)));
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_tracks"));
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_redstone"));
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_controls"));
    }
}
