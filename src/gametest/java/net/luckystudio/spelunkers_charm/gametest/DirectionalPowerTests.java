// Directional power tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.rail.DirectionalRailBlock;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.*;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class DirectionalPowerTests {
    @GameTest(template = "rail_large", timeoutTicks = 40)
    public static void directionalRailAcceptsTopAndAllFourSidesButNotBelow(GameTestHelper h) {
        Direction[] inputs = {
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST,
            Direction.DOWN
        };
        for (int i = 0; i < inputs.length; i++) {
            BlockPos pos = new BlockPos(5 + i * 8, 2, 8);
            place(h, pos, directional());
            h.setBlock(pos.relative(inputs[i]), Blocks.REDSTONE_BLOCK);
        }
        h.runAfterDelay(
                8,
                () -> {
                    for (int i = 0; i < inputs.length; i++)
                        h.assertTrue(
                                h.getBlockState(new BlockPos(5 + i * 8, 2, 8))
                                                .getValue(PoweredRailBlock.POWERED)
                                        == (inputs[i] != Direction.DOWN),
                                "Incorrect directional input face: " + inputs[i]);
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 40)
    public static void poweredRailsCannotSupplyDirectionalRail(GameTestHelper h) {
        for (int x = 6; x <= 9; x++) place(h, new BlockPos(x, 2, 8), powered());
        h.setBlock(new BlockPos(6, 1, 8), Blocks.REDSTONE_BLOCK);
        place(h, new BlockPos(10, 2, 8), directional());
        h.runAfterDelay(
                8,
                () -> {
                    h.assertTrue(
                            h.getBlockState(new BlockPos(9, 2, 8))
                                    .getValue(PoweredRailBlock.POWERED),
                            "Vanilla input rail failed to receive power");
                    h.assertFalse(
                            h.getBlockState(new BlockPos(10, 2, 8))
                                    .getValue(PoweredRailBlock.POWERED),
                            "Powered rail supplied directional rail");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 40)
    public static void directionalRailCannotRelayToAnyNeighborRailOrEmitSignal(GameTestHelper h) {
        BlockPos control = new BlockPos(10, 2, 8), other = new BlockPos(9, 2, 8);
        place(h, other, directional());
        place(h, control, directional());
        for (int x = 11; x <= 14; x++) place(h, new BlockPos(x, 2, 8), powered());
        h.setBlock(control.above(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(
                8,
                () -> {
                    h.assertTrue(
                            h.getBlockState(control).getValue(PoweredRailBlock.POWERED),
                            "Direct top input failed");
                    h.assertFalse(
                            h.getBlockState(other).getValue(PoweredRailBlock.POWERED),
                            "Directional rail relayed to another directional rail");
                    for (int x = 11; x <= 14; x++)
                        h.assertFalse(
                                h.getBlockState(new BlockPos(x, 2, 8))
                                        .getValue(PoweredRailBlock.POWERED),
                                "Directional rail relayed to powered rail " + x);
                    for (Direction direction : Direction.values()) {
                        BlockPos absolute = h.absolutePos(control);
                        h.assertTrue(
                                h.getLevel().getSignal(absolute, direction) == 0
                                        && h.getLevel().getDirectSignal(absolute, direction) == 0,
                                "Directional rail emitted redstone signal");
                    }
                    h.setBlock(control.above(), Blocks.AIR);
                    h.runAfterDelay(
                            4,
                            () -> {
                                h.assertFalse(
                                        h.getBlockState(control).getValue(PoweredRailBlock.POWERED),
                                        "Directional power did not turn off after source removal");
                                h.succeed();
                            });
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 40)
    public static void ascendingPoweredRailCannotSupplyDirectionalRamp(GameTestHelper h) {
        BlockPos control = new BlockPos(10, 2, 8);
        h.setBlock(control.below(), Blocks.STONE);
        h.setBlock(new BlockPos(11, 2, 8), Blocks.STONE);
        place(h, new BlockPos(11, 3, 8), powered());
        h.setBlock(new BlockPos(12, 3, 8), Blocks.REDSTONE_BLOCK);
        h.setBlock(
                control, directional().setValue(PoweredRailBlock.SHAPE, RailShape.ASCENDING_EAST));
        h.runAfterDelay(
                8,
                () -> {
                    h.assertTrue(
                            h.getBlockState(new BlockPos(11, 3, 8))
                                    .getValue(PoweredRailBlock.POWERED),
                            "Upper vanilla rail was not powered");
                    h.assertFalse(
                            h.getBlockState(control).getValue(PoweredRailBlock.POWERED),
                            "Ramp received relayed power from upper rail");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large", timeoutTicks = 40)
    public static void ordinaryPoweredRailsStillRelayNormally(GameTestHelper h) {
        for (int x = 5; x <= 12; x++) place(h, new BlockPos(x, 2, 8), powered());
        h.setBlock(new BlockPos(5, 1, 8), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(
                8,
                () -> {
                    for (int x = 5; x <= 12; x++)
                        h.assertTrue(
                                h.getBlockState(new BlockPos(x, 2, 8))
                                        .getValue(PoweredRailBlock.POWERED),
                                "Ordinary rail propagation changed");
                    h.succeed();
                });
    }

    @GameTest(template = "rail_large")
    public static void furnaceCartUsesVanillaFuelWithoutCustomScreen(GameTestHelper h) {
        Vec3 p = h.absoluteVec(new Vec3(5.5, 2.0625, 8.5));
        var cart = new MinecartFurnace(h.getLevel(), p.x, p.y, p.z);
        h.getLevel().addFreshEntity(cart);
        var player = h.makeMockServerPlayerInLevel();
        player.setPos(p.x, p.y, p.z + 1);
        player.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
        cart.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(cart.fuel > 0, "Vanilla coal interaction stopped fueling furnace cart");
        h.assertTrue(
                player.containerMenu == player.inventoryMenu,
                "Furnace cart opened a custom screen");
        h.succeed();
    }

    private static BlockState directional() {
        return ModBlocks.DIRECTIONAL_RAIL
                .get()
                .defaultBlockState()
                .setValue(DirectionalRailBlock.SHAPE, RailShape.EAST_WEST);
    }

    private static BlockState powered() {
        return Blocks.POWERED_RAIL
                .defaultBlockState()
                .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST);
    }

    private static void place(GameTestHelper h, BlockPos pos, BlockState state) {
        h.setBlock(pos.below(), Blocks.STONE);
        h.setBlock(pos, state);
    }
}
