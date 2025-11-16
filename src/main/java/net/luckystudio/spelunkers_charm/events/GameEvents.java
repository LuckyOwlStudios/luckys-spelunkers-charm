package net.luckystudio.spelunkers_charm.events;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.events.tremor.Tremor;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.block.custom.RockBlock;
import net.luckystudio.spelunkers_charm.block.util.ModBlockStateProperties;
import net.luckystudio.spelunkers_charm.events.tremor.TremorManager;
import net.luckystudio.spelunkers_charm.item.potion.ModPotions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

// Look at Mcreator for help on this file

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class GameEvents {

    // Look at Commands.java for help on this
    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal(Component.translatable("command.spelunkers_charm.tremor").getString()).requires(s -> s.hasPermission(4))
                        .then(Commands.literal(Component.translatable("command.spelunkers_charm.tremor.start").getString())
                                .then(Commands.argument(Component.translatable("command.spelunkers_charm.tremor.lengthInSeconds").getString(), IntegerArgumentType.integer(5, 100))
                                        .then(Commands.argument(Component.translatable("command.spelunkers_charm.tremor.intensity").getString(), IntegerArgumentType.integer(1, 5))
                                                .executes(ctx -> {
                                                    runTremorStart(ctx);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal(Component.translatable("command.spelunkers_charm.tremor.stop").getString())
                                .executes(ctx -> {
                                    runTremorStop(ctx);
                                    return 1;
                                })
                        )
                        .then(Commands.literal(Component.translatable("command.spelunkers_charm.tremor.help").getString())
                                .executes(ctx -> {
                                    runTremorHelp(ctx);
                                    return 1;
                                })
                        )
        );
    }

    private static void runTremorStart(CommandContext<CommandSourceStack> ctx) {
        Level level = ctx.getSource().getUnsidedLevel();
        Entity entity = ctx.getSource().getEntity();
        assert entity != null;
        BlockPos blockPos = new BlockPos((int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
        int length = ctx.getArgument(Component.translatable("command.spelunkers_charm.tremor.lengthInSeconds").getString(), Integer.class);
        int intensity = ctx.getArgument(Component.translatable("command.spelunkers_charm.tremor.intensity").getString(), Integer.class);
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("command.spelunkers_charm.tremor.start.message"), false);
        }
        TremorManager.spawnTremor(level, blockPos, length * 20, intensity, 1.0F, true);
    }

    private static void runTremorStop(CommandContext<CommandSourceStack> ctx) {
        Level level = ctx.getSource().getUnsidedLevel();
        Entity entity = ctx.getSource().getEntity();
        assert entity != null;
        BlockPos blockPos = entity.blockPosition();
        // Radius within which to look for tremors
        int radius = 64;

        // Replace TremorEntity.class with your actual class
        List<Tremor> tremors = level.getEntitiesOfClass(
                Tremor.class,
                new AABB(blockPos).inflate(radius)
        );

        for (Tremor tremor : tremors) {
            tremor.discard();
        }

        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("command.spelunkers_charm.tremor.stop.message", tremors.size()), false);
        }
    }

    private static void runTremorHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("command.spelunkers_charm.tremor.help.message"), false);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() == Items.MINECART) {
            event.getToolTip().add(Component.translatable("tooltip.spelunkers_charm.minecart").withStyle(ChatFormatting.GRAY));
        }
    }


    @SubscribeEvent
    public static void onBrewingRecipeRegister(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        builder.addMix(Potions.AWKWARD, ModBlocks.CAVE_MUSHROOM.asItem(), ModPotions.HASTE_POTION);
        builder.addMix(ModPotions.HASTE_POTION, Items.REDSTONE, ModPotions.LONG_HASTE_POTION);
        builder.addMix(ModPotions.HASTE_POTION, Items.GLOWSTONE_DUST, ModPotions.STRONG_HASTE_POTION);
    }

    @SubscribeEvent
    public static ItemInteractionResult onUseClayBallOnBlock(UseItemOnBlockEvent event) {
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        BlockPos offset = event.getPos().relative(Objects.requireNonNull(event.getFace()));
        Player player = event.getPlayer();
        if (stack.getItem() == Items.CLAY_BALL) {
            if (state.getBlock() == ModBlocks.CLAY_PILE.get() && state.getValue(ModBlockStateProperties.ROCKS) < 3) {
                level.setBlock(pos, state.cycle(RockBlock.ROCKS), 3);
                useClayBall(player, level, pos, stack);
                player.swing(event.getHand());
                return ItemInteractionResult.SUCCESS;
            } else if (event.getFace() == Direction.UP && (!state.isAir() && !state.canBeReplaced() && player.isCrouching())) {
                level.setBlock(offset, ModBlocks.CLAY_PILE.get().defaultBlockState().setValue(RockBlock.FACING, Direction.fromYRot(player.getYRot()).getOpposite()), 3);
                useClayBall(player, level, pos, stack);
                player.swing(event.getHand());
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
