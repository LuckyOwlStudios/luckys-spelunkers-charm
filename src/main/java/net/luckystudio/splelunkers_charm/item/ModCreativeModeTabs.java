package net.luckystudio.splelunkers_charm.item;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpelunkersCharm.MOD_ID);

    public static final Supplier<CreativeModeTab> SPELUNKERS_CHARM = CREATIVE_MODE_TABS.register("spelunkers_charm",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.ROCK.get()))
                    .title(Component.translatable("creative_tab.spelunkers_charm"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.COLD_STONE);
                        output.accept(ModBlocks.SILT);
                        output.accept(ModBlocks.DEEPSLATE_GEYSER);
                        output.accept(ModBlocks.BASALT_GEYSER);
                        output.accept(ModBlocks.ROCK);
                        output.accept(ModBlocks.DEEPSLATE_ROCK);
                        output.accept(ModBlocks.DRIPSTONE_ROCK);
                        output.accept(ModBlocks.BASALT_ROCK);
                        output.accept(ModBlocks.CAVE_MUSHROOM);
                        output.accept(ModItems.MINING_HELMET);
                        output.accept(ModItems.SMALL_LIFT);
                        output.accept(ModItems.MEDIUM_LIFT);
                        output.accept(ModItems.LARGE_LIFT);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
