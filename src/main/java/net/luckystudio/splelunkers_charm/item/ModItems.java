package net.luckystudio.splelunkers_charm.item;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.item.custom.MiningHelmet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpelunkersCharm.MODID);

    // Items
    public static final DeferredItem<Item> MINING_HELMET = ITEMS.register("mining_helmet", () -> new MiningHelmet(new Item.Properties().stacksTo(1).durability(250).rarity(Rarity.UNCOMMON)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
