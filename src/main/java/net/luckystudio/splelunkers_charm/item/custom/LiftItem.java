package net.luckystudio.splelunkers_charm.item.custom;

import net.luckystudio.splelunkers_charm.entity.custom.lift.AbstractLift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Objects;

public class LiftItem extends Item {
    final AbstractLift.Type type;

    public LiftItem(AbstractLift.Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        ItemStack itemstack = context.getItemInHand();

        if (level instanceof ServerLevel serverlevel) {
            AbstractLift abstractLift = AbstractLift.createLift(
                    serverlevel,
                    blockpos.getX() + 0.5,
                    blockpos.getY() + 1.0,
                    blockpos.getZ() + 0.5,
                    this.type,
                    itemstack,
                    context.getPlayer()
            );
            Direction facing = Objects.requireNonNull(context.getPlayer()).getDirection();
            abstractLift.setYRot(facing.toYRot());

            // ✅ Perform space check before adding to the world
            if (!serverlevel.noCollision(abstractLift, abstractLift.getBoundingBox())) {
                return InteractionResult.FAIL;  // Not enough space
            }

            serverlevel.addFreshEntity(abstractLift);
            serverlevel.gameEvent(GameEvent.ENTITY_PLACE, blockpos, GameEvent.Context.of(context.getPlayer(), serverlevel.getBlockState(blockpos.below())));
            itemstack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
