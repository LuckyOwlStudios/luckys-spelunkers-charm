// Railway tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.rail.CrossingRailBlock;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.JukeboxMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartRailPath;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLanterns;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class RailwayTests {
    @GameTest(template = "empty")
    public static void formsCrossingFromNormalRailInteraction(GameTestHelper h) {
        rail(h, 8, 8, RailShape.NORTH_SOUTH);
        BlockPos pos = h.absolutePos(new BlockPos(8, 2, 8));
        ServerPlayer player = player(h);
        player.moveTo(pos.getX() - 1, pos.getY(), pos.getZ() + 0.5, -90, 0);
        ItemStack rails = new ItemStack(Items.RAIL, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, rails);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var result =
                player.gameMode.useItemOn(
                        player, h.getLevel(), rails, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(result.consumesAction(), "Rail click did not create a crossing");
        h.assertTrue(
                h.getLevel().getBlockState(pos).is(ModBlocks.CROSSING_RAIL.get()),
                "Crossing block missing");
        h.assertTrue(rails.getCount() == 1, "Crossing should consume one rail in survival");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void crossingKeepsBothAxesSeparate(GameTestHelper h) {
        crossing(h);
        Minecart east = cart(h, 8.5, 8.5);
        Minecart south = cart(h, 8.5, 8.5);
        east.setDeltaMovement(0.3, 0, 0);
        south.setDeltaMovement(0, 0, 0.3);
        h.assertTrue(
                MinecartRailPath.between(east, south) == null,
                "Crossing incorrectly joins two axes");
        south.discard();
        double startX = east.getX();
        double startZ = east.getZ();
        h.runAfterDelay(
                6,
                () -> {
                    h.assertTrue(
                            east.getX() - startX > 1 && Math.abs(east.getZ() - startZ) < 0.01,
                            "Cart turned on east-west crossing");
                    east.discard();
                    Minecart north = cart(h, 8.5, 8.5);
                    north.setDeltaMovement(0, 0, -0.3);
                    h.runAfterDelay(
                            6,
                            () -> {
                                h.assertTrue(
                                        startZ - north.getZ() > 1
                                                && Math.abs(north.getX() - startX) < 0.01,
                                        "Cart turned on north-south crossing");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void locomotivePullsJukeboxThroughCrossing(GameTestHelper h) {
        crossing(h);
        for (int x = 12; x < 27; x++) rail(h, x, 8, RailShape.EAST_WEST);
        Minecart engineBase = cart(h, 7.8, 8.5);
        Minecart musicBase = cart(h, 6.25, 8.5);
        ServerPlayer player = player(h);
        var engineBox = engineBase.getBoundingBox();
        var musicBox = musicBase.getBoundingBox();
        CartLanterns.hang(musicBase, new ItemStack(Items.LANTERN), player);
        use(h, player, engineBase, ModItems.CART_COUPLING_HOOK.toStack());
        use(h, player, musicBase, player.getMainHandItem());
        player.setPose(Pose.CROUCHING);
        player.setShiftKeyDown(true);
        ItemStack furnace = new ItemStack(Items.BLAST_FURNACE, 2);
        use(h, player, engineBase, furnace);
        h.assertTrue(
                furnace.getCount() == 1, "Locomotive conversion should consume one blast furnace");
        use(h, player, musicBase, new ItemStack(Items.JUKEBOX));
        var engine =
                h.getLevel().getEntitiesOfClass(LocomotiveMinecart.class, engineBox).getFirst();
        var music = h.getLevel().getEntitiesOfClass(JukeboxMinecart.class, musicBox).getFirst();
        MinecartCouplings links = MinecartCouplings.get(h.getLevel());
        h.assertTrue(
                links.linked(engine.getUUID(), music.getUUID()),
                "Cart conversions lost the coupling");
        h.assertTrue(
                CartLanterns.getLantern(music) == CartLantern.LANTERN,
                "Conversion lost the lantern");
        use(h, player, music, new ItemStack(Items.MUSIC_DISC_CAT));
        h.assertTrue(
                music.isPlaying() && music.getComparatorLevel() > 0,
                "Jukebox cart did not play a disc");
        engine.setItem(0, new ItemStack(Items.COAL));
        h.assertTrue(
                engine.driveVector().lengthSqr() < .001,
                "Stationary block conversion chose a drive command before player input");
        player.moveTo(engine.getX() - 1, engine.getY(), engine.getZ(), 0, 0);
        use(h, player, engine, ItemStack.EMPTY);
        player.moveTo(engine.getX(), engine.getY() + 5, engine.getZ(), 0, 0);
        double initial = music.getX();
        double lane = music.getZ();
        h.runAfterDelay(
                30,
                () -> {
                    h.assertTrue(engine.isBurning(), "Fueled locomotive failed to start");
                    h.assertTrue(
                            music.getX() - initial > 3,
                            "Locomotive did not pull the jukebox across the crossing");
                    h.assertTrue(
                            Math.abs(music.getZ() - lane) < 0.02,
                            "Train changed lanes at the crossing");
                    h.assertTrue(
                            links.linked(engine.getUUID(), music.getUUID()),
                            "Train uncoupled at the crossing");
                    use(h, player, music, new ItemStack(Items.SHEARS));
                    h.assertTrue(
                            CartLanterns.getLantern(music) == CartLantern.NONE && music.isPlaying(),
                            "Removing the lantern affected the jukebox disc");
                    use(h, player, music, ItemStack.EMPTY);
                    h.assertFalse(music.hasRecord(), "Right-click did not eject the disc");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void woodenRailSlowsCartsAndBreaksUnderLocomotive(GameTestHelper h) {
        BlockPos pos = new BlockPos(8, 2, 8);
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(
                pos,
                ModBlocks.WOODEN_RAIL
                        .get()
                        .defaultBlockState()
                        .setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        Minecart cart = cart(h, 8.5, 8.5);
        h.assertTrue(
                Math.abs(
                                cart.getMaxSpeedWithRail()
                                        - net.luckystudio.spelunkers_charm.entity.custom.minecart
                                                        .MinecartSpeedRules.SPRINT_BLOCKS_PER_SECOND
                                                * net.luckystudio.spelunkers_charm.entity.custom
                                                        .minecart.MinecartSpeedRules.SPEED_SCALE
                                                / 20)
                        < 0.001,
                "Wooden rail did not limit cart speed");
        cart.discard();
        LocomotiveMinecart locomotive =
                new LocomotiveMinecart(h.getLevel(), cart.getX(), cart.getY(), cart.getZ());
        h.getLevel().addFreshEntity(locomotive);
        h.succeedWhen(() -> h.assertBlockPresent(Blocks.AIR, pos));
    }

    @GameTest(template = "empty")
    public static void poweredEjectorLaunchesPassengersAboveSlime(GameTestHelper h) {
        BlockPos pos = new BlockPos(8, 2, 8);
        h.setBlock(pos.below(), Blocks.SLIME_BLOCK);
        h.setBlock(pos.north(), Blocks.REDSTONE_BLOCK);
        h.setBlock(
                pos,
                ModBlocks.EJECTOR_RAIL
                        .get()
                        .defaultBlockState()
                        .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST)
                        .setValue(PoweredRailBlock.POWERED, true));
        Minecart cart = cart(h, 8.5, 8.5);
        Sheep passenger = EntityType.SHEEP.create(h.getLevel());
        passenger.setPos(cart.position());
        h.getLevel().addFreshEntity(passenger);
        h.assertTrue(passenger.startRiding(cart, true), "Test passenger could not board");
        h.runAfterDelay(
                4,
                () -> {
                    h.assertFalse(
                            passenger.isPassenger(), "Ejector rail did not dismount passenger");
                    h.assertTrue(
                            passenger.getY() > cart.getY() + 2,
                            "Slime ejector did not launch passenger");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void lanternLightFollowsCartAndLeavesWhenRemoved(GameTestHelper h) {
        for (int x = 5; x <= 11; x++) rail(h, x, 8, RailShape.EAST_WEST);
        Minecart cart = cart(h, 7.5, 8.5);
        ServerPlayer player = player(h);
        use(h, player, cart, new ItemStack(Items.SOUL_LANTERN));
        BlockPos start = cart.blockPosition().above();
        h.runAfterDelay(
                2,
                () -> {
                    h.assertTrue(
                            h.getLevel().getBlockState(start).is(Blocks.LIGHT)
                                    && h.getLevel().getBlockState(start).getValue(LightBlock.LEVEL)
                                            == 10,
                            "Soul lantern did not place a level-10 light");
                    CompoundTag saved = cart.saveWithoutId(new CompoundTag());
                    cart.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                    Minecart reloaded =
                            new Minecart(h.getLevel(), cart.getX(), cart.getY(), cart.getZ());
                    reloaded.load(saved);
                    h.assertTrue(
                            h.getLevel().addFreshEntity(reloaded), "Could not reload lantern cart");
                    h.assertTrue(
                            CartLanterns.getLantern(reloaded) == CartLantern.SOUL_LANTERN,
                            "Saved cart lost its lantern");
                    reloaded.setDeltaMovement(0.3, 0, 0);
                    h.runAfterDelay(
                            6,
                            () -> {
                                BlockPos moved = reloaded.blockPosition().above();
                                h.assertFalse(
                                        start.equals(moved), "Cart did not move to the next block");
                                h.assertTrue(
                                        h.getLevel().getBlockState(start).isAir(),
                                        "Reloaded lantern left light behind");
                                h.assertTrue(
                                        h.getLevel().getBlockState(moved).is(Blocks.LIGHT),
                                        "Light did not move with cart");
                                reloaded.discard();
                                h.assertTrue(
                                        h.getLevel().getBlockState(moved).isAir(),
                                        "Removed cart left its light behind");
                                h.succeed();
                            });
                });
    }

    private static void crossing(GameTestHelper h) {
        for (int x = 2; x <= 12; x++) rail(h, x, 8, RailShape.EAST_WEST);
        for (int z = 3; z <= 12; z++) {
            if (z != 8) rail(h, 8, z, RailShape.NORTH_SOUTH);
        }
        h.setBlock(
                new BlockPos(8, 2, 8),
                ModBlocks.CROSSING_RAIL
                        .get()
                        .defaultBlockState()
                        .setValue(CrossingRailBlock.SHAPE, RailShape.EAST_WEST));
        for (int x : new int[] {7, 9}) forceRail(h, x, 8, RailShape.EAST_WEST);
        for (int z : new int[] {7, 9}) forceRail(h, 8, z, RailShape.NORTH_SOUTH);
    }

    private static void rail(GameTestHelper h, int x, int z, RailShape shape) {
        h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        h.setBlock(
                new BlockPos(x, 2, z),
                Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape));
    }

    private static void forceRail(GameTestHelper h, int x, int z, RailShape shape) {
        h.getLevel()
                .setBlock(
                        h.absolutePos(new BlockPos(x, 2, z)),
                        Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape),
                        Block.UPDATE_CLIENTS);
    }

    private static Minecart cart(GameTestHelper h, double x, double z) {
        Vec3 position = h.absoluteVec(new Vec3(x, 2.08, z));
        Minecart cart = new Minecart(h.getLevel(), position.x, position.y, position.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void use(
            GameTestHelper h, ServerPlayer player, AbstractMinecart cart, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        h.assertTrue(
                player.interactOn(cart, InteractionHand.MAIN_HAND).consumesAction(),
                "Cart did not accept interaction");
    }
}
