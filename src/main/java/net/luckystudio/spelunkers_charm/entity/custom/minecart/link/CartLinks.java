package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The carts a minecart is coupled to, saved on the cart as a data attachment.
 * A cart can be coupled to at most {@link CartLinking#MAX_LINKS} others (one in front, one behind).
 */
public record CartLinks(List<UUID> links) {

    public static final CartLinks EMPTY = new CartLinks(List.of());
    public static final Codec<CartLinks> CODEC = UUIDUtil.CODEC.listOf().xmap(CartLinks::new, CartLinks::links);

    public CartLinks {
        links = List.copyOf(links);
    }

    public boolean isEmpty() {
        return this.links.isEmpty();
    }

    public boolean contains(UUID id) {
        return this.links.contains(id);
    }

    public CartLinks with(UUID id) {
        if (contains(id)) return this;
        List<UUID> list = new ArrayList<>(this.links);
        list.add(id);
        return new CartLinks(list);
    }

    public CartLinks without(UUID id) {
        List<UUID> list = new ArrayList<>(this.links);
        list.remove(id);
        return new CartLinks(list);
    }
}
