package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.List;

/**
 * The client's copy of each minecart's couplings and lantern, by entity id.
 * Kept free of client-only classes so it is safe to reference from common code.
 */
public final class ClientCartData {

    private static final Int2ObjectMap<int[]> LINKS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<CartLantern> LANTERNS = new Int2ObjectOpenHashMap<>();
    private static final int[] NONE = new int[0];

    private ClientCartData() {
    }

    public static void set(int cartId, List<Integer> linkedIds, int lantern) {
        if (linkedIds.isEmpty()) {
            LINKS.remove(cartId);
        } else {
            LINKS.put(cartId, linkedIds.stream().mapToInt(Integer::intValue).toArray());
        }
        CartLantern type = CartLantern.byId(lantern);
        if (type == CartLantern.NONE) {
            LANTERNS.remove(cartId);
        } else {
            LANTERNS.put(cartId, type);
        }
    }

    public static int[] get(int cartId) {
        return LINKS.getOrDefault(cartId, NONE);
    }

    public static CartLantern getLantern(int cartId) {
        return LANTERNS.getOrDefault(cartId, CartLantern.NONE);
    }

    public static boolean isLinked(int cartId, int otherId) {
        for (int id : get(cartId)) {
            if (id == otherId) return true;
        }
        return false;
    }

    public static void remove(int cartId) {
        LINKS.remove(cartId);
        LANTERNS.remove(cartId);
    }

    public static void clear() {
        LINKS.clear();
        LANTERNS.clear();
    }
}
