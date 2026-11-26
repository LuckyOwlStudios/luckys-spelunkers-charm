// Mine crate tests.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.minecrate.MineCrateBlockEntity;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MineCrateMinecart;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModDataComponents;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(LiftTestMod.ID)
@PrefixGameTestTemplate(false)
public final class MineCrateTests {
    private static final BlockPos CARGO = new BlockPos(8, 2, 8);
    private static final BlockPos LOADER = CARGO.above(2);
    private static final BlockPos POWER = LOADER.east();

    @GameTest(template = "empty")
    public static void mineCrateKeeps2048ItemsThroughBreakingAndPlacement(GameTestHelper h) {
        h.setBlock(CARGO.below(), Blocks.STONE);
        h.setBlock(CARGO, ModBlocks.MINE_CRATE.get());
        var crate = (MineCrateBlockEntity) h.getBlockEntity(CARGO);
        IItemHandler storage = crate.storage();
        for (int i = 0; i < 32; i++) {
            h.assertTrue(
                    storage.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false).isEmpty(),
                    "All 32 stacks must fit");
        }
        h.assertTrue(
                storage.getStackInSlot(0).getCount() == 2048
                        && !storage.insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty(),
                "Crate did not enforce the 2048-item limit");
        var position = h.absolutePos(CARGO);
        h.getLevel().destroyBlock(position, true);
        var drops =
                h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(1));
        ItemStack packed =
                drops.stream()
                        .map(ItemEntity::getItem)
                        .filter(stack -> stack.is(ModBlocks.MINE_CRATE.asItem()))
                        .findFirst()
                        .orElse(ItemStack.EMPTY)
                        .copy();
        h.assertTrue(
                packed.has(ModDataComponents.CRATE_CONTENTS.get()),
                "Crate dropped without its cargo");
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(position.getX() + .5, position.getY(), position.getZ() + 2, 180, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, packed);
        var hit =
                new BlockHitResult(
                        Vec3.atBottomCenterOf(position), Direction.UP, position.below(), false);
        h.assertTrue(
                player.gameMode
                        .useItemOn(player, h.getLevel(), packed, InteractionHand.MAIN_HAND, hit)
                        .consumesAction(),
                "Could not place the packed crate");
        var restored = (MineCrateBlockEntity) h.getBlockEntity(CARGO);
        h.assertTrue(
                restored.storage().getStackInSlot(0).getCount() == 2048
                        && restored.storage().displayItem().is(Items.IRON_INGOT),
                "Crate contents did not survive placement");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void loaderTwoBlocksUpHandlesCrateAndOnlyMatchingCargo(GameTestHelper h) {
        IItemHandler slot = setupLoader(h);
        Minecart empty = spawnCart(h);
        slot.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false);
        h.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(
                3,
                () -> {
                    h.assertTrue(
                            !empty.isRemoved() && slot.getStackInSlot(0).getCount() == 8,
                            "Ordinary carts must reject non-block items");
                    slot.extractItem(0, 64, false);
                    slot.insertItem(0, ModBlocks.MINE_CRATE.toStack(), false);
                    pulse(h);
                    h.runAfterDelay(
                            3,
                            () -> {
                                MineCrateMinecart cart = crateCart(h);
                                h.assertTrue(
                                        empty.isRemoved() && slot.getStackInSlot(0).isEmpty(),
                                        "Loader two blocks above did not mount the crate");
                                for (int i = 0; i < 31; i++) {
                                    cart.storage()
                                            .insertItem(
                                                    0, new ItemStack(Items.IRON_INGOT, 64), false);
                                }
                                cart.storage()
                                        .insertItem(0, new ItemStack(Items.IRON_INGOT, 16), false);
                                slot.insertItem(0, new ItemStack(Items.GOLD_INGOT, 64), false);
                                pulse(h);
                                h.runAfterDelay(
                                        3,
                                        () -> {
                                            h.assertTrue(
                                                    slot.getStackInSlot(0).getCount() == 64
                                                            && cart.storage()
                                                                            .getStackInSlot(0)
                                                                            .getCount()
                                                                    == 2000,
                                                    "Loader mixed two cargo types");
                                            slot.extractItem(0, 64, false);
                                            slot.insertItem(
                                                    0, new ItemStack(Items.IRON_INGOT, 64), false);
                                            pulse(h);
                                            h.runAfterDelay(
                                                    3,
                                                    () -> {
                                                        h.assertTrue(
                                                                cart.storage()
                                                                                        .getStackInSlot(
                                                                                                0)
                                                                                        .getCount()
                                                                                == 2048
                                                                        && slot.getStackInSlot(0)
                                                                                        .getCount()
                                                                                == 16,
                                                                "Load should move only the items"
                                                                        + " that fit");
                                                        h.succeed();
                                                    });
                                        });
                            });
                });
    }

    @GameTest(template = "empty")
    public static void loadedCrateCanBeMountedSavedAndRemoved(GameTestHelper h) {
        h.setBlock(CARGO.below(), Blocks.STONE);
        h.setBlock(CARGO, Blocks.RAIL);
        Minecart empty = spawnCart(h);
        h.setBlock(CARGO.east(), ModBlocks.MINE_CRATE.get());
        var stationary = (MineCrateBlockEntity) h.getBlockEntity(CARGO.east());
        stationary.storage().insertItem(0, new ItemStack(Items.REDSTONE, 64), false);
        stationary.storage().insertItem(0, new ItemStack(Items.REDSTONE, 32), false);
        ItemStack packed = ModBlocks.MINE_CRATE.toStack();
        packed.set(
                ModDataComponents.CRATE_CONTENTS.get(),
                stationary.storage().pack(h.getLevel().registryAccess()));
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPose(Pose.CROUCHING);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, packed);
        h.assertTrue(
                player.interactOn(empty, InteractionHand.MAIN_HAND).consumesAction()
                        && packed.isEmpty(),
                "Sneak-right-click should attach the packed crate");
        MineCrateMinecart cart = crateCart(h);
        CompoundTag data = cart.saveWithoutId(new CompoundTag());
        var restored = ModEntityType.MINE_CRATE_MINECART.get().create(h.getLevel());
        restored.load(data);
        h.assertTrue(
                restored.storage().getStackInSlot(0).getCount() == 96
                        && restored.displayItem().is(Items.REDSTONE),
                "Cargo or label was lost on cart save and load");
        var name = restored.getCustomName();
        h.assertTrue(
                name != null
                        && name.getString().equals("96x")
                        && name.getStyle().isBold()
                        && name.getStyle().getColor().getValue() == ChatFormatting.GOLD.getColor(),
                "Restored crate should display its gold cargo count");
        restored.storage().extractItem(0, 64, false);
        h.assertTrue(
                restored.getName().getString().equals("32x"),
                "Partial extraction did not update the count");
        restored.storage().extractItem(0, 64, false);
        h.assertFalse(restored.hasCustomName(), "Empty minecart should hide the count");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(
                player.interactOn(cart, InteractionHand.MAIN_HAND).consumesAction()
                        && cart.isRemoved(),
                "Sneak-right-click with an empty hand should detach the crate");
        ItemStack returned =
                player.getInventory().items.stream()
                        .filter(stack -> stack.is(ModBlocks.MINE_CRATE.asItem()))
                        .findFirst()
                        .orElse(ItemStack.EMPTY);
        var component = returned.get(ModDataComponents.CRATE_CONTENTS.get());
        h.assertTrue(
                component != null && component.copyTag().getInt("Count") == 96,
                "Detaching should preserve the full crate");
        h.succeed();
    }

    private static IItemHandler setupLoader(GameTestHelper h) {
        h.setBlock(CARGO.below(), Blocks.STONE);
        h.setBlock(CARGO, Blocks.RAIL);
        h.setBlock(LOADER, ModBlocks.CART_LOADER.get());
        return ((CartLoaderBlockEntity) h.getBlockEntity(LOADER)).itemHandler();
    }

    private static Minecart spawnCart(GameTestHelper h) {
        Vec3 pos = Vec3.atBottomCenterOf(h.absolutePos(CARGO)).add(0, .08, 0);
        Minecart cart = new Minecart(h.getLevel(), pos.x, pos.y, pos.z);
        h.getLevel().addFreshEntity(cart);
        return cart;
    }

    private static void pulse(GameTestHelper h) {
        h.setBlock(POWER, Blocks.AIR);
        h.setBlock(POWER, Blocks.REDSTONE_BLOCK);
    }

    private static MineCrateMinecart crateCart(GameTestHelper h) {
        var carts =
                h.getLevel()
                        .getEntitiesOfClass(
                                MineCrateMinecart.class, new AABB(h.absolutePos(CARGO)));
        h.assertTrue(carts.size() == 1, "Expected a single mine crate cart");
        return carts.getFirst();
    }
}
