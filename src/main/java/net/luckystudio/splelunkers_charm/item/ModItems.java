package net.luckystudio.splelunkers_charm.item;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpelunkersCharm.MODID);

    public static final DeferredItem<Item> ROCK = ITEMS.register("rock", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
