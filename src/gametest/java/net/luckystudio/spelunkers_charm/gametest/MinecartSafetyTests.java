// Minecart safety tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class MinecartSafetyTests {
    @GameTest(template = "rail_large")
    public static void linkedCartsRejectSmallFriendlyMobs(GameTestHelper h) {
        var a = cart(h, 8.5);
        var b = cart(h, 10.05);
        MinecartCouplings.get(h.getLevel()).link(a, b);
        var pig = EntityType.PIG.create(h.getLevel());
        add(h, pig, a);
        h.assertFalse(pig.startRiding(a, true), "Linked cart accepted a friendly mob");
        MinecartCouplings.get(h.getLevel()).unlink(a.getUUID(), b.getUUID());
        h.assertTrue(pig.startRiding(a, true), "Detached cart still rejects fitting mob");
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void smallHostileMobsCanRideButOversizedMobsCannot(GameTestHelper h) {
        var cart = cart(h, 8.5);
        for (var type :
                new EntityType<?>[] {
                    EntityType.ZOMBIE,
                    EntityType.CREEPER,
                    EntityType.PIGLIN,
                    EntityType.CAVE_SPIDER,
                    EntityType.ENDERMAN
                }) {
            var mob = (Mob) type.create(h.getLevel());
            add(h, mob, cart);
            h.assertTrue(
                    mob.startRiding(cart, true), "Small hostile mob could not enter cart: " + type);
            mob.stopRiding();
            mob.discard();
        }
        for (var type :
                new EntityType<?>[] {EntityType.GHAST, EntityType.RAVAGER, EntityType.SPIDER}) {
            var mob = (Mob) type.create(h.getLevel());
            add(h, mob, cart);
            h.assertFalse(
                    mob.startRiding(cart, true), "Oversized hostile mob entered cart: " + type);
            mob.discard();
        }
        h.succeed();
    }

    @GameTest(template = "rail_large")
    public static void adultSnifferPandaAndGolemCannotEnter(GameTestHelper h) {
        var cart = cart(h, 8.5);
        for (var type :
                new EntityType<?>[] {
                    EntityType.SNIFFER, EntityType.PANDA, EntityType.IRON_GOLEM, EntityType.CAMEL
                }) {
            var mob = (Mob) type.create(h.getLevel());
            add(h, mob, cart);
            h.assertFalse(mob.startRiding(cart, true), "Oversized adult entered cart: " + type);
            mob.discard();
        }
        h.succeed();
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void babySnifferBreaksCartWhenItGrows(GameTestHelper h) {
        var cart = cart(h, 8.5);
        Sniffer mob = EntityType.SNIFFER.create(h.getLevel());
        mob.setAge(-24000);
        add(h, mob, cart);
        h.assertTrue(
                MinecartSafetyRules.fits(mob) && mob.startRiding(cart, true),
                "Small sniffer could not board");
        h.runAfterDelay(
                3,
                () -> {
                    mob.setAge(0);
                    h.runAfterDelay(
                            3,
                            () -> {
                                h.assertTrue(
                                        cart.isRemoved() && mob.isAlive() && !mob.isPassenger(),
                                        "Grown sniffer did not break and leave cart safely");
                                h.assertTrue(
                                        h.getLevel()
                                                        .getEntitiesOfClass(
                                                                ItemEntity.class,
                                                                cart.getBoundingBox().inflate(3),
                                                                item ->
                                                                        item.getItem()
                                                                                .is(Items.MINECART))
                                                        .size()
                                                == 1,
                                        "Growth did not return exactly one minecart item");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void babyPandaBreaksLinkedCartWithoutHurtingOtherCart(GameTestHelper h) {
        var cart = cart(h, 8.5);
        Panda mob = EntityType.PANDA.create(h.getLevel());
        mob.setAge(-24000);
        add(h, mob, cart);
        h.assertTrue(mob.startRiding(cart, true), "Baby panda could not board");
        var peer = cart(h, 10.05);
        var links = MinecartCouplings.get(h.getLevel());
        links.link(cart, peer);
        h.runAfterDelay(
                3,
                () -> {
                    h.assertTrue(
                            mob.getVehicle() == cart,
                            "Linking ejected an already loaded friendly mob");
                    mob.setAge(0);
                    h.runAfterDelay(
                            3,
                            () -> {
                                h.assertTrue(
                                        cart.isRemoved() && peer.isAlive() && mob.isAlive(),
                                        "Panda growth broke wrong entity");
                                h.assertTrue(
                                        links.neighbors(peer.getUUID()).isEmpty(),
                                        "Growth left a ghost coupling");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void babyPigMayGrowWhenAdultStillFits(GameTestHelper h) {
        var cart = cart(h, 8.5);
        Pig pig = EntityType.PIG.create(h.getLevel());
        pig.setAge(-24000);
        add(h, pig, cart);
        h.assertTrue(pig.startRiding(cart, true), "Baby pig could not board");
        pig.setAge(0);
        h.runAfterDelay(
                5,
                () -> {
                    h.assertTrue(
                            cart.isAlive() && pig.getVehicle() == cart, "Fitting adult broke cart");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void movingLinkedCartDoesNotAutomaticallyCollectMob(GameTestHelper h) {
        lane(h);
        var cart = cart(h, 7.5);
        var peer = cart(h, 5.95);
        MinecartCouplings.get(h.getLevel()).link(cart, peer);
        var pig = EntityType.PIG.create(h.getLevel());
        add(h, pig, cart);
        pig.setPos(cart.getX() + .3, cart.getY(), cart.getZ());
        cart.setDeltaMovement(.25, 0, 0);
        peer.setDeltaMovement(.25, 0, 0);
        h.runAfterDelay(
                6,
                () -> {
                    h.assertFalse(pig.isPassenger(), "Moving linked cart auto-mounted nearby mob");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void stationarySingleRailPileKeepsEightOldestCarts(GameTestHelper h) {
        h.setBlock(new BlockPos(8, 1, 8), Blocks.STONE);
        h.setBlock(new BlockPos(8, 2, 8), Blocks.RAIL);
        var carts = new ArrayList<Minecart>();
        for (int i = 0; i < 128; i++) carts.add(cart(h, 8.5));
        h.runAfterDelay(
                2,
                () -> {
                    for (int i = 0; i < carts.size(); i++)
                        h.assertTrue(
                                carts.get(i).isAlive() == (i < 8),
                                "Crowd cap failed to keep first eight carts: " + i);
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 50)
    public static void crowdedCargoDropsContentsAndTntDoesNotIgnite(GameTestHelper h) {
        for (int i = 0; i < 8; i++) cart(h, 8.5);
        Vec3 p = h.absoluteVec(new Vec3(8.5, 2.0625, 8.5));
        var chest = new MinecartChest(h.getLevel(), p.x, p.y, p.z);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        h.getLevel().addFreshEntity(chest);
        var tnt = new MinecartTNT(h.getLevel(), p.x, p.y, p.z);
        tnt.setDeltaMovement(.3, 0, 0);
        h.getLevel().addFreshEntity(tnt);
        h.runAfterDelay(
                2,
                () -> {
                    h.assertTrue(
                            chest.isRemoved() && tnt.isRemoved(),
                            "Excess cargo carts survived crowd limit");
                    int diamonds =
                            h
                                    .getLevel()
                                    .getEntitiesOfClass(
                                            ItemEntity.class, chest.getBoundingBox().inflate(4))
                                    .stream()
                                    .filter(item -> item.getItem().is(Items.DIAMOND))
                                    .mapToInt(item -> item.getItem().getCount())
                                    .sum();
                    h.assertTrue(diamonds == 3, "Crowd cleanup lost or duplicated cargo");
                    h.assertFalse(
                            h.getLevel()
                                            .getEntitiesOfClass(
                                                    net.minecraft.world.entity.item.PrimedTnt.class,
                                                    chest.getBoundingBox().inflate(8))
                                            .size()
                                    > 0,
                            "Crowd cleanup ignited TNT");
                    h.succeed();
                });
    }

    private static Minecart cart(GameTestHelper h, double x) {
        Vec3 p = h.absoluteVec(new Vec3(x, 2.0625, 8.5));
        var cart = new Minecart(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static void add(GameTestHelper h, Mob mob, Minecart cart) {
        mob.setNoAi(true);
        mob.setPos(cart.position());
        h.getLevel().addFreshEntity(mob);
    }

    private static void lane(GameTestHelper h) {
        for (int x = 1; x <= 40; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.REDSTONE_BLOCK);
            h.getLevel()
                    .setBlock(
                            h.absolutePos(new BlockPos(x, 2, 8)),
                            Blocks.POWERED_RAIL
                                    .defaultBlockState()
                                    .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                                    .setValue(PoweredRailBlock.POWERED, true),
                            2);
        }
    }
}
