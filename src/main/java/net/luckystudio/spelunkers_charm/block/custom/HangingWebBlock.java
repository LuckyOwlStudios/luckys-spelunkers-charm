package net.luckystudio.spelunkers_charm.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HangingWebBlock extends HangingBlock {
    public static final MapCodec<HangingWebBlock> CODEC = simpleCodec(HangingWebBlock::new);

    public HangingWebBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        Vec3 vec3 = new Vec3(0.25, 0.05F, 0.25);
        if (entity instanceof LivingEntity livingentity && livingentity.hasEffect(MobEffects.WEAVING)) {
            vec3 = new Vec3(0.5, 0.25, 0.5);
        }
        entity.makeStuckInBlock(state, vec3);
    }
}
