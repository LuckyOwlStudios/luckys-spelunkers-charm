package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The carts a minecart is coupled to, saved on the cart as a data attachment, and which item (chain, rope...)
 * each coupling was made with, so that item is given back when it comes apart.
 * A cart can be coupled to at most {@link CartLinking#MAX_LINKS} others (one in front, one behind).
 */
public record CartLinks(List<UUID> links, Map<UUID, Item> items) {

    public static final CartLinks EMPTY = new CartLinks(List.of(), Map.of());

    private static final Codec<CartLinks> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.listOf().fieldOf("links").forGetter(CartLinks::links),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, BuiltInRegistries.ITEM.byNameCodec()).optionalFieldOf("items", Map.of()).forGetter(CartLinks::items)
    ).apply(instance, CartLinks::new));

    /** Also reads the old format, a plain list of carts, where every coupling was a chain. */
    public static final Codec<CartLinks> CODEC = Codec.withAlternative(FULL_CODEC,
            UUIDUtil.CODEC.listOf().xmap(list -> new CartLinks(list, Map.of()), CartLinks::links));

    public CartLinks {
        links = List.copyOf(links);
        items = Map.copyOf(items);
    }

    public boolean isEmpty() {
        return this.links.isEmpty();
    }

    public boolean contains(UUID id) {
        return this.links.contains(id);
    }

    /** What the coupling to this cart was made with. */
    public Item itemFor(UUID id) {
        return this.items.getOrDefault(id, Items.CHAIN);
    }

    public CartLinks with(UUID id, Item item) {
        if (contains(id)) return this;
        List<UUID> list = new ArrayList<>(this.links);
        list.add(id);
        Map<UUID, Item> map = new HashMap<>(this.items);
        map.put(id, item);
        return new CartLinks(list, map);
    }

    public CartLinks without(UUID id) {
        List<UUID> list = new ArrayList<>(this.links);
        list.remove(id);
        Map<UUID, Item> map = new HashMap<>(this.items);
        map.remove(id);
        return new CartLinks(list, map);
    }
}
