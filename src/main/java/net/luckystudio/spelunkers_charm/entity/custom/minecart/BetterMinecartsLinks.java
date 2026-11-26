// Better minecarts links.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class BetterMinecartsLinks {
    private static final boolean INSTALLED = ModList.get().isLoaded("betterminecarts");
    private static final Access ACCESS = INSTALLED ? discover() : null;

    private BetterMinecartsLinks() {}

    public static boolean hasLink(AbstractMinecart cart) {
        if (!INSTALLED) return false;
        if (ACCESS == null) return true;
        if (!ACCESS.type.isInstance(cart)) return false;
        try {
            return ACCESS.parent.invoke(cart) != null || ACCESS.child.invoke(cart) != null;
        } catch (ReflectiveOperationException error) {
            return true;
        }
    }

    private static Access discover() {
        try {
            Class<?> type = Class.forName("hu.xannosz.betterminecarts.utils.Linkable");
            return new Access(
                    type, type.getMethod("getLinkedParent"), type.getMethod("getLinkedChild"));
        } catch (ReflectiveOperationException | LinkageError error) {
            return null;
        }
    }

    /** Caches access to Better Minecarts link data. */
    private record Access(Class<?> type, Method parent, Method child) {}
}
