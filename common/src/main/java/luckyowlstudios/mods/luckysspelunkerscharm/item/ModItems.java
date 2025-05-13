package luckyowlstudios.mods.luckysspelunkerscharm.item;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.item.BalmItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm;

import static net.blay09.mods.balm.api.item.BalmItems.itemProperties;
import static luckyowlstudios.mods.luckysspelunkerscharm.LuckysSpelunkersCharm.id;

public class ModItems {
    public static DeferredObject<CreativeModeTab> creativeModeTab;

    public static Item yourItem;

    public static void initialize(BalmItems items) {
        items.registerItem((identifier) -> yourItem = new Item(itemProperties(identifier)), id("your_item"));

        creativeModeTab = items.registerCreativeModeTab(() -> new ItemStack(yourItem), id(LuckysSpelunkersCharm.MOD_ID));
    }

}
