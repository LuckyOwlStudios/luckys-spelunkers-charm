// Item tags.
package net.luckystudio.spelunkers_charm.datagen.itemTags;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModItemTags {
    public static final TagKey<Item> MINERALS = createTag("minerals");
    public static final TagKey<Item> GEYSERS = createTag("geysers");

    /** Items that can couple minecarts: any chain, and ropes from other mods. */
    public static final TagKey<Item> CART_COUPLERS = createTag("cart_couplers");

    private static TagKey<Item> createTag(String name) {
        return ItemTags.create(SpelunkersCharm.id(name));
    }
}
