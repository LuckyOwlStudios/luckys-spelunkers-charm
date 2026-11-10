package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Passive guide, using the wooden rack appearance from the original release. */
public class BasicLiftTrackBlock extends LiftTrackBlock {
    public static final MapCodec<BasicLiftTrackBlock> CODEC = simpleCodec(BasicLiftTrackBlock::new);

    public BasicLiftTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BasicLiftTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean hasSignal(Level level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spelunkers_charm.lift_basic"));
    }
}
