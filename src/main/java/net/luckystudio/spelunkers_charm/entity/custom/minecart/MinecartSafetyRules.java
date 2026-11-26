// Minecart safety rules.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.mixin.VehicleDropAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;

@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID)
public final class MinecartSafetyRules {
    private static final String APPROVED = "SpelunkersSmallCartPassenger";
    public static final int MAX_CROWD = 8;

    private MinecartSafetyRules() {}

    public static boolean fits(Mob mob) {
        return mob.getBbWidth() <= 1.00001
                && mob.getBbHeight() <= (mob instanceof EnderMan ? 3.00001 : 2.00001);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mount(EntityMountEvent event) {
        if (!event.isMounting()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntityMounting() instanceof Mob mob)
                || event.getEntityBeingMounted() == null) return;
        var cart = MinecartPassengers.cart(event.getEntityBeingMounted());
        if (cart == null) return;
        if (!fits(mob) || !MinecartCouplings.get(level).neighbors(cart.getUUID()).isEmpty())
            event.setCanceled(true);
        else mob.getPersistentData().putUUID(APPROVED, cart.getUUID());
    }

    @SubscribeEvent
    public static void grow(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel))
            return;
        var cart = MinecartPassengers.cart(mob);
        if (cart == null || !cart.isAlive()) return;
        if (mob instanceof EnderMan enderman
                && (enderman.getTarget() instanceof Player
                        || enderman.isCreepy() && enderman.getPersistentAngerTarget() != null)) {
            mob.stopRiding();
            mob.getPersistentData().remove(APPROVED);
            return;
        }
        if (fits(mob)) {
            if (!mob.getPersistentData().hasUUID(APPROVED)
                    || !cart.getUUID().equals(mob.getPersistentData().getUUID(APPROVED)))
                mob.getPersistentData().putUUID(APPROVED, cart.getUUID());
        } else {
            boolean grew =
                    mob.getPersistentData().hasUUID(APPROVED)
                            && cart.getUUID().equals(mob.getPersistentData().getUUID(APPROVED));
            mob.stopRiding();
            mob.getPersistentData().remove(APPROVED);
            if (grew) breakCart(cart);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void crowded(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractMinecart cart)
                || !(cart.level() instanceof ServerLevel level)) return;
        if (!cart.isAlive()) {
            event.setCanceled(true);
            return;
        }
        var pile =
                level.getEntitiesOfClass(
                        AbstractMinecart.class,
                        cart.getBoundingBox().inflate(.2, .4, .2),
                        peer ->
                                peer.isAlive()
                                        && peer.position()
                                                        .subtract(cart.position())
                                                        .horizontalDistanceSqr()
                                                < .65 * .65
                                        && Math.abs(peer.getY() - cart.getY()) < .8);
        if (pile.size() > MAX_CROWD) {
            pile.sort(Comparator.comparingInt(AbstractMinecart::getId));
            for (int i = MAX_CROWD; i < pile.size(); i++) breakCart(pile.get(i));
        }
        if (!cart.isAlive()) event.setCanceled(true);
    }

    public static void breakCart(AbstractMinecart cart) {
        if (cart.level().isClientSide() || !cart.isAlive()) return;
        cart.ejectPassengers();
        cart.destroy(((VehicleDropAccess) cart).spelunkersCharm$dropItem());
    }
}
