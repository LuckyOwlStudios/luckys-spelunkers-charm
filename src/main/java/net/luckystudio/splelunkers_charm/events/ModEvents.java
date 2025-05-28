package net.luckystudio.splelunkers_charm.events;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.luckystudio.splelunkers_charm.block.custom.RockBlock;
import net.luckystudio.splelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.splelunkers_charm.entity.custom.tremor.TremorManager;
import net.luckystudio.splelunkers_charm.item.ModItems;
import net.luckystudio.splelunkers_charm.item.potion.ModPotions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@EventBusSubscriber(modid = SpelunkersCharm.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {

    @SubscribeEvent
    public static void onBrewingRecipeRegister(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        builder.addMix(Potions.AWKWARD, ModBlocks.CAVE_MUSHROOM.asItem(), ModPotions.HASTE_POTION);
        builder.addMix(ModPotions.HASTE_POTION, Items.REDSTONE, ModPotions.LONG_HASTE_POTION);
        builder.addMix(ModPotions.HASTE_POTION, Items.GLOWSTONE_DUST, ModPotions.STRONG_HASTE_POTION);
    }

    /**
     *     This event is fired when an explosion occurs in the game.
     *     A Tremor is spawned at the center of the explosion if the explosion is in the Overworld or Nether and below y=32.
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        Vec3 center = event.getExplosion().center();
        if (center.y < 32 && (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER)) {
            float force = event.getExplosion().radius();
            BlockPos pos = new BlockPos((int) center.x, (int) center.y, (int) center.z);
            ResourceKey<Biome> biomeKey = level.getBiome(pos).getKey();
            System.out.println(biomeKey);
            TremorManager.spawnTremor(level, pos, (int) (force * 60F), (int) force);
        }
    }

    @SubscribeEvent
    public static ItemInteractionResult onUseClayBallOnBlock(UseItemOnBlockEvent event) {
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();
        if (stack.getItem() == Items.CLAY_BALL) {
            event.cancelWithResult(ItemInteractionResult.SUCCESS);
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            BlockPos offset = event.getPos().relative(Objects.requireNonNull(event.getFace()));
            Player player = event.getPlayer();
            assert player != null;
            if (state.getBlock() == ModBlocks.CLAY_BALL.get() && state.getValue(ModBlockStateProperties.ROCKS) < 3) {
                level.setBlock(pos, state.cycle(RockBlock.ROCKS), 3);
                useClayBall(player, level, pos, stack);
                return ItemInteractionResult.SUCCESS;
            } else if (event.getFace() == Direction.UP && (!state.isAir() && !state.canBeReplaced())) {
                level.setBlock(offset, ModBlocks.CLAY_BALL.get().defaultBlockState().setValue(RockBlock.FACING, Direction.fromYRot(player.getYRot()).getOpposite()), 3);
                useClayBall(player, level, pos, stack);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void useClayBall(@NotNull Player player, Level level, BlockPos pos, ItemStack stack) {
        level.playSound(null, pos, SoundEvents.WET_GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.awardStat(Stats.ITEM_USED.get(Items.CLAY_BALL));
        stack.consume(1, player);
    }
}
