package net.luckystudio.spelunkers_charm.item.custom;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

/** Places a locomotive on a rail, like a minecart. It will drive the way the player was facing. */
public class LocomotiveItem extends Item {

    public LocomotiveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel serverLevel) {
            RailShape shape = state.getBlock() instanceof BaseRailBlock rail
                    ? rail.getRailDirection(state, level, pos, null)
                    : RailShape.NORTH_SOUTH;
            double yOffset = shape.isAscending() ? 0.5 : 0.0;
            LocomotiveMinecart locomotive = new LocomotiveMinecart(serverLevel,
                    pos.getX() + 0.5, pos.getY() + 0.0625 + yOffset, pos.getZ() + 0.5);
            EntityType.<LocomotiveMinecart>createDefaultStackConfig(serverLevel, stack, context.getPlayer()).accept(locomotive);
            locomotive.setDriveDirection(context.getHorizontalDirection());
            serverLevel.addFreshEntity(locomotive);
            serverLevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), serverLevel.getBlockState(pos.below())));
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
