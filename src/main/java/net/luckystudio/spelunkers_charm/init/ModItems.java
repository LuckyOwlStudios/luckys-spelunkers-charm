package net.luckystudio.spelunkers_charm.init;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.item.custom.MiningHelmet;
import net.luckystudio.spelunkers_charm.item.custom.LiftItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpelunkersCharm.MOD_ID);

    // Items
    public static final DeferredItem<Item> MINING_HELMET = ITEMS.register("mining_helmet", () -> new MiningHelmet(new Item.Properties().stacksTo(1).durability(250).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SMALL_LIFT = ITEMS.register("small_lift", () -> new LiftItem(AbstractLift.Type.SMALL, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> MEDIUM_LIFT = ITEMS.register("medium_lift", () -> new LiftItem(AbstractLift.Type.MEDIUM, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> LARGE_LIFT = ITEMS.register("large_lift", () -> new LiftItem(AbstractLift.Type.LARGE, new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
