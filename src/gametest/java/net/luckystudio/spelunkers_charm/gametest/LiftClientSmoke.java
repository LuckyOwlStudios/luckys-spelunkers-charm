// Lift client smoke.
package net.luckystudio.spelunkers_charm.gametest;

import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderBlock;
import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.cartloader.CartLoaderScreen;
import net.luckystudio.spelunkers_charm.block.custom.minecrate.MineCrateBlockEntity;
import net.luckystudio.spelunkers_charm.block.custom.tank.TankBlockEntity;
import net.luckystudio.spelunkers_charm.client.CartLoadTrailParticle;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientSupport;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.JukeboxMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.JukeboxMinecartSounds;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.LocomotiveMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MineCrateMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplingVisuals;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.MinecartCouplings;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.TankMinecart;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLantern;
import net.luckystudio.spelunkers_charm.entity.custom.minecart.link.CartLanterns;
import net.luckystudio.spelunkers_charm.entity.util.MovementType;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.luckystudio.spelunkers_charm.init.ModEntityType;
import net.luckystudio.spelunkers_charm.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = LiftTestMod.ID, value = Dist.CLIENT)
public final class LiftClientSmoke {
    private static boolean requested, setup, finished;
    private static volatile int[] lifts, riders;
    private static volatile int[] carts;
    private static final boolean COUPLER_SMOKE =
            Boolean.getBoolean("spelunkers_couplers.clientSmoke");
    private static final boolean CRATE_COUNT_ONLY =
            Boolean.getBoolean("spelunkers_crate_count.clientSmoke");
    private static final boolean CULLING_SMOKE =
            Boolean.getBoolean("spelunkers_lifts.clientCulling");
    private static final BlockPos CART_LOADER = new BlockPos(16, 85, 27);
    private static final BlockPos MINE_CRATE = new BlockPos(13, 84, 28);
    private static final BlockPos TEST_TANK = new BlockPos(25, 84, 27);
    private static final BlockPos EMPTY_TANK = TEST_TANK.west(2);
    private static final ResourceLocation CREATE_HONEY =
            ResourceLocation.fromNamespaceAndPath("create", "honey");
    private static volatile int tankCartId;
    private static volatile int crateCartId;
    private static boolean cartLoaderScreenChecked;
    private static boolean heardLoad, heardUnload;
    private static int loadTrails;
    private static int tick,
            frames,
            samples,
            upSparks,
            downSparks,
            stoppedChecks,
            loopChecks,
            silentChecks,
            pitchMask,
            creativeGroups;
    private static double maxError, maxUncorrected;
    private static final Set<Particle> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Path REPORT = Path.of("client-checks.txt");

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("spelunkers_lifts.clientSmoke") || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!requested && mc.getOverlay() == null && mc.screen != null) {
                requested = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.renderDistance().set(4);
                mc.options.framerateLimit().set(60);
                mc.options
                        .getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER)
                        .set(0.02D);
                org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
                verifyResources(mc);
                if (COUPLER_SMOKE) {
                    var loadedCart =
                            ResourceLocation.fromNamespaceAndPath(
                                    "spelunkers_charm", "loaded_cart");
                    mc.getSoundManager()
                            .addListener(
                                    (sound, eventInfo, range) -> {
                                        if (sound.getLocation().equals(loadedCart))
                                            heardLoad = true;
                                        if (sound.getLocation()
                                                .equals(
                                                        SoundEvents.BUNDLE_REMOVE_ONE
                                                                .getLocation())) heardUnload = true;
                                    });
                }
                String migration = System.getProperty("spelunkers_lifts.migrationWorld", "");
                if (!migration.isEmpty()) {
                    mc.createWorldOpenFlows().openWorld(migration, mc::stop);
                    return;
                }
                var settings =
                        new LevelSettings(
                                "Lift regression",
                                net.minecraft.world.level.GameType.CREATIVE,
                                false,
                                net.minecraft.world.Difficulty.PEACEFUL,
                                true,
                                new GameRules(),
                                WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows()
                        .createFreshLevel(
                                "lift-client-" + System.currentTimeMillis(),
                                settings,
                                new WorldOptions(42L, false, false),
                                registries ->
                                        registries
                                                .registryOrThrow(Registries.WORLD_PRESET)
                                                .getHolderOrThrow(WorldPresets.FLAT)
                                                .value()
                                                .createWorldDimensions(),
                                mc.screen);
                return;
            }
            if (LiftMountedSmoke.enabled()
                    && mc.level != null
                    && mc.player != null
                    && mc.getOverlay() == null
                    && mc.screen == null) {
                LiftMountedSmoke.tick(mc);
                return;
            }
            if (RailwayMotionSmoke.enabled()
                    && mc.level != null
                    && mc.player != null
                    && mc.getOverlay() == null) {
                RailwayMotionSmoke.tick(mc);
                return;
            }
            if (GuideBookSmoke.enabled()
                    && mc.level != null
                    && mc.player != null
                    && mc.getOverlay() == null) {
                GuideBookSmoke.tick(mc);
                return;
            }
            if (COUPLER_SMOKE && mc.screen instanceof CartLoaderScreen screen) {
                var menu = screen.getMenu();
                check(menu.slots.size() == 37, "Cart Loader needs one slot above player inventory");
                var stack = menu.getSlot(0).getItem();
                if (!stack.isEmpty()) {
                    check(
                            stack.is(Items.COBBLESTONE) && stack.getCount() == 32,
                            "Cart Loader screen did not sync its stored blocks");
                    cartLoaderScreenChecked = true;
                    mc.player.closeContainer();
                }
                return;
            }
            if (mc.level == null
                    || mc.player == null
                    || mc.getOverlay() != null
                    || mc.screen != null) return;
            if (!setup) {
                verifyCreativeOrder(mc);
                setup = true;
                if (!System.getProperty("spelunkers_lifts.migrationWorld", "").isEmpty()) {
                    mc.getSingleplayerServer()
                            .execute(() -> verifyMigration(mc.getSingleplayerServer().overworld()));
                    return;
                }
                mc.getSingleplayerServer()
                        .execute(() -> setupWorld(mc.getSingleplayerServer().overworld()));
                return;
            }
            if (lifts == null || mc.level.getEntity(lifts[2]) == null) return;
            ++tick;
            if (CULLING_SMOKE && tick == 45) {
                check(mc.player.getLookAngle().z > .95, "Camera is not facing away from the lifts");
                for (int id : lifts) {
                    var platform = (AbstractLift) mc.level.getEntity(id);
                    check(
                            platform.getY() > 76,
                            "Offscreen lift stopped interpolating: " + platform.getType());
                }
            }
            if (CRATE_COUNT_ONLY && (tick == 70 || tick == 92)) {
                var server = mc.getSingleplayerServer();
                boolean viewTanks = tick == 70;
                server.execute(
                        () -> {
                            var level = server.overworld();
                            var player = level.players().getFirst();
                            if (viewTanks) player.teleportTo(level, 25.5, 87.2, 31.5, 180, 42);
                            else player.teleportTo(level, 14.5, 85, 31, 180, 21);
                        });
            }
            if (COUPLER_SMOKE && tick == 6) {
                var server = mc.getSingleplayerServer();
                server.execute(() -> openCartLoader(server.overworld()));
            }
            if (COUPLER_SMOKE && tick == 105) {
                var server = mc.getSingleplayerServer();
                server.execute(() -> pulseLoaderAndLoadCrate(server.overworld()));
            }
            if (COUPLER_SMOKE && (tick == 120 || tick == 130)) {
                var server = mc.getSingleplayerServer();
                boolean onBlock = tick == 120;
                server.execute(() -> aimAtCrate(server.overworld(), onBlock));
            }
            if (COUPLER_SMOKE && tick == 140) {
                var crate = (MineCrateBlockEntity) mc.level.getBlockEntity(MINE_CRATE);
                check(
                        crate != null
                                && crate.storage().count() == 32
                                && crate.storage().displayItem().is(Items.DIAMOND),
                        "Mine Crate label did not sync to the client");
                var crateCart = (MineCrateMinecart) mc.level.getEntity(crateCartId);
                check(
                        crateCart != null
                                && crateCart.getName().getString().equals("33x")
                                && crateCart.displayItem().is(Items.IRON_INGOT),
                        "Crate minecart label did not sync to the client");
                var server = mc.getSingleplayerServer();
                server.execute(() -> emptyCrates(server.overworld()));
            }
            if (!CRATE_COUNT_ONLY && (tick == 20 || tick == 90 || tick == 65 || tick == 135)) {
                MovementType movement =
                        tick == 20
                                ? MovementType.UP
                                : tick == 90 ? MovementType.DOWN : MovementType.STATIONARY;
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    ServerLevel level = mc.getSingleplayerServer().overworld();
                                    for (int id : lifts)
                                        if (level.getEntity(id) instanceof AbstractLift lift) {
                                            if (movement == MovementType.DOWN)
                                                for (BlockPos guide : lift.getGuidePositions()) {
                                                    double dx = lift.getX() - guide.getX() - .5,
                                                            dz = lift.getZ() - guide.getZ() - .5;
                                                    Direction inward =
                                                            Math.abs(dx) > Math.abs(dz)
                                                                    ? (dx > 0
                                                                            ? Direction.EAST
                                                                            : Direction.WEST)
                                                                    : (dz > 0
                                                                            ? Direction.SOUTH
                                                                            : Direction.NORTH);
                                                    for (int y = 68; y <= 89; y++)
                                                        level.setBlockAndUpdate(
                                                                guide.atY(y)
                                                                        .relative(
                                                                                inward
                                                                                        .getOpposite()),
                                                                Blocks.AIR.defaultBlockState());
                                                }
                                            lift.setMovementType(movement);
                                        }
                                });
            }
            if (tick == 155) {
                if (CRATE_COUNT_ONLY) {
                    verifyClearedCrateCounts(mc);
                    verifyTankClient(mc);
                    finished = true;
                    Files.writeString(
                            REPORT, "PASS Mine Crate labels and Tank fluids on block and cart\n");
                    mc.stop();
                    return;
                }
                check(
                        frames > 30 && samples > 300,
                        "Client did not render enough moving frames: " + frames + "/" + samples);
                check(maxError < 0.035, "Remote riders visibly left deck: " + maxError);
                check(
                        upSparks >= 30 && downSparks >= 30,
                        "Missing visible sparks: up=" + upSparks + " down=" + downSparks);
                check(stoppedChecks > 5, "No stationary particle check");
                check(
                        loopChecks > 10 && silentChecks > 5,
                        "Movement loop was not verified playing/stopped");
                check(pitchMask == 7, "Pitch not verified for all lift sizes");
                if (COUPLER_SMOKE) {
                    check(cartLoaderScreenChecked, "Cart Loader screen did not open");
                    check(heardLoad && heardUnload, "Missing Mine Crate load or unload audio");
                    check(loadTrails >= 3, "Missing visible Cart Loader trails");
                    verifyClearedCrateCounts(mc);
                    var first = (MinecartCouplingVisuals) mc.level.getEntity(carts[0]);
                    var second = (MinecartCouplingVisuals) mc.level.getEntity(carts[1]);
                    check(
                            first != null
                                    && second != null
                                    && first.spelunkersCharm$firstCouplingId() == carts[1]
                                    && second.spelunkersCharm$firstCouplingId() == carts[0],
                            "Couplings did not sync to the client");
                    check(
                            Files.isRegularFile(Path.of("cart-coupling.png")),
                            "Missing coupling scene screenshot");
                    verifyRailwayClient(mc);
                }
                finished = true;
                String result =
                        "PASS actual Minecraft client; frames="
                                + frames
                                + "; samples="
                                + samples
                                + "; maximum_displayed_feet_error="
                                + maxError
                                + "; maximum_unadjusted_error="
                                + maxUncorrected
                                + "; up_sparks="
                                + upSparks
                                + "; down_sparks="
                                + downSparks
                                + "; stopped_checks="
                                + stoppedChecks
                                + "; loop_checks="
                                + loopChecks
                                + "; silent_checks="
                                + silentChecks
                                + "; textures=12; station_models=128; new_track_models=32;"
                                + " half_power_up=true; zero_power_down=true; pitch_mask="
                                + pitchMask
                                + "; creative_groups="
                                + creativeGroups
                                + "\n";
                Files.writeString(REPORT, result);
                System.out.println("SPELUNKERS_LIFT_CLIENT_PASS " + result);
                mc.stop();
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static void verifyResources(Minecraft mc) throws Exception {
        if (COUPLER_SMOKE) {
            verifyCartLoaderModels(mc);
            var crate = ModBlocks.MINE_CRATE.get().defaultBlockState();
            check(
                    mc.getBlockRenderer().getBlockModel(crate)
                            != mc.getModelManager().getMissingModel(),
                    "Missing Mine Crate block model");
            var sound =
                    ResourceLocation.fromNamespaceAndPath(
                            "spelunkers_charm", "sounds/block/cart_loader/loaded_cart.ogg");
            try (var stream = mc.getResourceManager().getResourceOrThrow(sound).open()) {
                check(
                        new String(stream.readNBytes(4), java.nio.charset.StandardCharsets.US_ASCII)
                                .equals("OggS"),
                        "Missing OGG sound data for the loader");
            }
            for (var rail :
                    new net.minecraft.world.level.block.Block[] {
                        ModBlocks.CROSSING_RAIL.get(),
                        ModBlocks.EJECTOR_RAIL.get(),
                        ModBlocks.WOODEN_RAIL.get()
                    }) {
                for (var state : rail.getStateDefinition().getPossibleStates()) {
                    check(
                            mc.getBlockRenderer().getBlockModel(state)
                                    != mc.getModelManager().getMissingModel(),
                            "Missing railway model " + state);
                }
            }
            for (String name :
                    new String[] {
                        "item/cart_coupling_hook.png", "entity/minecart/coupling_metal.png"
                    }) {
                var id =
                        ResourceLocation.fromNamespaceAndPath(
                                "spelunkers_charm", "textures/" + name);
                try (var stream = mc.getResourceManager().getResourceOrThrow(id).open();
                        var image = com.mojang.blaze3d.platform.NativeImage.read(stream)) {
                    check(
                            image.getWidth() == 16 && image.getHeight() == 16,
                            "Incorrect coupling texture " + name);
                }
            }
            var hook =
                    mc.getItemRenderer()
                            .getModel(ModItems.CART_COUPLING_HOOK.toStack(), null, null, 0);
            check(hook != mc.getModelManager().getMissingModel(), "Missing hook item model");
        }
        String[] files = {
            "up_powered_lift_track_front",
            "up_powered_lift_track_front_powered",
            "up_powered_lift_track_side",
            "down_powered_lift_track_front",
            "down_powered_lift_track_front_powered",
            "down_powered_lift_track_side",
            "directional_powered_lift_track_end",
            "directional_powered_lift_track_end_powered",
            "detector_lift_track_front",
            "powered_detector_lift_track_front",
            "up_powered_lift_track_side_powered",
            "down_powered_lift_track_side_powered"
        };
        for (String file : files) {
            var id =
                    ResourceLocation.fromNamespaceAndPath(
                            "spelunkers_charm", "textures/block/" + file + ".png");
            try (var stream = mc.getResourceManager().getResourceOrThrow(id).open();
                    var image = com.mojang.blaze3d.platform.NativeImage.read(stream)) {
                check(
                        image.getWidth() == 16 && image.getHeight() == 16,
                        "Incorrect texture " + file);
            }
        }
        for (var state :
                ModBlocks.DIRECTIONAL_LIFT_TRACK.get().getStateDefinition().getPossibleStates())
            check(
                    mc.getBlockRenderer().getBlockModel(state)
                            != mc.getModelManager().getMissingModel(),
                    "Missing station model " + state);
        for (var state :
                ModBlocks.POWERED_LIFT_TRACK.get().getStateDefinition().getPossibleStates())
            check(
                    mc.getBlockRenderer().getBlockModel(state)
                            != mc.getModelManager().getMissingModel(),
                    "Missing motor model " + state);
        for (var block :
                new net.minecraft.world.level.block.Block[] {
                    ModBlocks.BASIC_LIFT_TRACK.get(), ModBlocks.DETECTOR_LIFT_TRACK.get()
                })
            for (var state : block.getStateDefinition().getPossibleStates())
                check(
                        mc.getBlockRenderer().getBlockModel(state)
                                != mc.getModelManager().getMissingModel(),
                        "Missing new guide model " + state);
    }

    private static void verifyCartLoaderModels(Minecraft mc) {
        var block = ModBlocks.CART_LOADER.get();
        for (boolean powered : new boolean[] {false, true}) {
            var state = block.defaultBlockState().setValue(CartLoaderBlock.POWERED, powered);
            var model = mc.getBlockRenderer().getBlockModel(state);
            for (Direction face : Direction.values()) {
                var quads = model.getQuads(state, face, net.minecraft.util.RandomSource.create());
                String texture =
                        face == Direction.UP
                                ? "top"
                                : face == Direction.DOWN
                                        ? "bottom"
                                        : powered ? "side_powered" : "side";
                var expected =
                        ResourceLocation.fromNamespaceAndPath(
                                "spelunkers_charm", "block/cart_loader_" + texture);
                check(
                        quads.size() == 1
                                && quads.getFirst().getSprite().contents().name().equals(expected),
                        "Wrong Cart Loader texture for " + face + " with powered=" + powered);
            }
        }
    }

    private static void openCartLoader(ServerLevel level) {
        try {
            var player = level.players().getFirst();
            var hit =
                    new BlockHitResult(
                            Vec3.atCenterOf(CART_LOADER), Direction.SOUTH, CART_LOADER, false);
            var result =
                    player.gameMode.useItemOn(
                            player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
            check(result.consumesAction(), "Cart Loader did not accept right-click");
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> fail(error));
        }
    }

    private static void pulseLoaderAndLoadCrate(ServerLevel level) {
        try {
            level.setBlockAndUpdate(CART_LOADER.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            var player = level.players().getFirst();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT));
            check(
                    player.interactOn(level.getEntity(crateCartId), InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "Mine Crate cart did not accept the iron ingot");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> fail(error));
        }
    }

    private static void emptyCrates(ServerLevel level) {
        try {
            var player = level.players().getFirst();
            var hit =
                    new BlockHitResult(
                            Vec3.atBottomCenterOf(MINE_CRATE.above()),
                            Direction.UP,
                            MINE_CRATE,
                            false);
            check(
                    player.gameMode
                            .useItemOn(
                                    player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit)
                            .consumesAction(),
                    "Empty hand could not take the diamond stack");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            check(
                    player.interactOn(level.getEntity(crateCartId), InteractionHand.MAIN_HAND)
                            .consumesAction(),
                    "Empty hand could not take the iron stack");
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> fail(error));
        }
    }

    private static void aimAtCrate(ServerLevel level, boolean onBlock) {
        var player = level.players().getFirst();
        Vec3 target =
                onBlock
                        ? Vec3.atCenterOf(MINE_CRATE)
                        : level.getEntity(crateCartId).position().add(0, 0.35, 0);
        Vec3 offset = target.subtract(player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance()));
        player.teleportTo(level, player.getX(), player.getY(), player.getZ(), yaw, pitch);
    }

    private static void verifyClearedCrateCounts(Minecraft mc) {
        var crate = (MineCrateBlockEntity) mc.level.getBlockEntity(MINE_CRATE);
        check(
                crate != null
                        && crate.storage().count() == 0
                        && crate.storage().displayItem().isEmpty(),
                "Mine Crate label stayed visible after extraction");
        var cart = (MineCrateMinecart) mc.level.getEntity(crateCartId);
        check(
                cart != null && !cart.hasCustomName() && cart.displayItem().isEmpty(),
                "Minecart label stayed visible after extraction");
        check(
                Files.isRegularFile(Path.of("mine-crate-block-count.png"))
                        && Files.isRegularFile(Path.of("mine-crate-cart-count.png")),
                "Client did not target both crate count labels");
    }

    private static void setupTanks(ServerLevel level) {
        level.setBlockAndUpdate(EMPTY_TANK.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(EMPTY_TANK, ModBlocks.TANK.get().defaultBlockState());
        level.setBlockAndUpdate(TEST_TANK.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(TEST_TANK, ModBlocks.TANK.get().defaultBlockState());
        var tank = (TankBlockEntity) level.getBlockEntity(TEST_TANK);
        tank.storage().fill(new FluidStack(Fluids.WATER, 3500), FluidAction.EXECUTE);
        BlockPos rail = TEST_TANK.east(2);
        level.setBlockAndUpdate(rail.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(rail, Blocks.RAIL.defaultBlockState());
        var cart = ModEntityType.TANK_MINECART.get().create(level);
        cart.setPos(Vec3.atBottomCenterOf(rail).add(0, 0.08, 0));
        var fluid = BuiltInRegistries.FLUID.getOptional(CREATE_HONEY).orElse(Fluids.LAVA);
        cart.storage().fill(new FluidStack(fluid, 6500), FluidAction.EXECUTE);
        level.addFreshEntity(cart);
        tankCartId = cart.getId();
    }

    private static void verifyTankClient(Minecraft mc) {
        var tank = (TankBlockEntity) mc.level.getBlockEntity(TEST_TANK);
        check(
                tank != null
                        && tank.storage().getFluidAmount() == 3500
                        && tank.storage().getFluid().getFluid() == Fluids.WATER,
                "Stationary tank did not synchronize water and level");
        var cart = (TankMinecart) mc.level.getEntity(tankCartId);
        check(
                cart != null && cart.storage().getFluidAmount() == 6500,
                "Tank cart did not synchronize its fluid level");
        var fluid = cart.storage().getFluid();
        var expected =
                ModList.get().isLoaded("create")
                        ? CREATE_HONEY
                        : ResourceLocation.withDefaultNamespace("lava");
        check(
                expected.equals(BuiltInRegistries.FLUID.getKey(fluid.getFluid())),
                "Wrong fluid in the client tank cart");
        var texture = IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid);
        check(
                texture != null
                        && mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                                .apply(texture)
                                .contents()
                                .name()
                                .equals(texture),
                "Tank cart did not resolve the fluid texture from the atlas");
        var state = ModBlocks.TANK.get().defaultBlockState();
        var model = mc.getBlockRenderer().getBlockModel(state);
        check(model != mc.getModelManager().getMissingModel(), "Missing tank block model");
        var bottom = ResourceLocation.fromNamespaceAndPath("spelunkers_charm", "block/tank_bottom");
        check(
                model.getQuads(state, null, net.minecraft.util.RandomSource.create()).stream()
                        .anyMatch(
                                quad ->
                                        quad.getDirection() == Direction.UP
                                                && quad.getSprite()
                                                        .contents()
                                                        .name()
                                                        .equals(bottom)),
                "Tank model is missing its inward-facing bottom texture");
        check(
                Files.isRegularFile(Path.of("tank-fluids.png")),
                "Missing screenshot of the tanks and empty floor");
    }

    private static void verifyCreativeOrder(Minecraft mc) {
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
                mc.level.enabledFeatures(), true, mc.level.registryAccess());
        net.minecraft.world.item.Item[] group = {
            ModBlocks.BASIC_LIFT_TRACK.get().asItem(),
            ModBlocks.POWERED_LIFT_TRACK.get().asItem(),
            ModBlocks.DIRECTIONAL_LIFT_TRACK.get().asItem(),
            ModBlocks.DETECTOR_LIFT_TRACK.get().asItem(),
            net.luckystudio.spelunkers_charm.init.ModItems.SMALL_LIFT.get(),
            net.luckystudio.spelunkers_charm.init.ModItems.MEDIUM_LIFT.get(),
            net.luckystudio.spelunkers_charm.init.ModItems.LARGE_LIFT.get()
        };
        for (var key :
                java.util.List.of(
                        net.minecraft.world.item.CreativeModeTabs.REDSTONE_BLOCKS,
                        net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES,
                        net.minecraft.world.item.CreativeModeTabs.SEARCH)) {
            var tab =
                    net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(
                            key);
            var entries = new java.util.ArrayList<>(tab.getDisplayItems());
            int start = -1;
            for (int i = 0; i < entries.size(); i++)
                if (entries.get(i).is(group[0])) {
                    start = i;
                    break;
                }
            check(
                    start >= 0 && start + group.length <= entries.size(),
                    "Missing lift group in creative " + key.location());
            for (int i = 0; i < group.length; i++)
                check(
                        entries.get(start + i).is(group[i]),
                        "Separated lift items in creative " + key.location() + " at " + i);
            creativeGroups++;
        }
    }

    private static void verifyMigration(ServerLevel level) {
        try {
            BlockPos oldBasic = new BlockPos(2, 75, 0), oldDetector = new BlockPos(2, 76, 0);
            level.getChunkAt(oldBasic);
            check(
                    level.getBlockState(oldBasic).is(ModBlocks.BASIC_LIFT_TRACK.get()),
                    "Legacy world lost basic rack at "
                            + oldBasic
                            + ": "
                            + level.getBlockState(oldBasic));
            check(
                    level.getBlockState(oldDetector).is(ModBlocks.DETECTOR_LIFT_TRACK.get()),
                    "Legacy world lost detector rack");
            check(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK
                            .getKey(level.getBlockState(oldBasic).getBlock())
                            .getPath()
                            .equals("basic_lift_track"),
                    "Old world kept wrong canonical block ID");
            Minecraft.getInstance()
                    .execute(
                            () -> {
                                try {
                                    Files.writeString(
                                            Path.of("migration-checks.txt"),
                                            "PASS loaded lifts.4 world with lifts.5; old basic"
                                                    + " palette remapped, detector preserved, registry"
                                                    + " snapshot accepted\n");
                                    finished = true;
                                    Minecraft.getInstance().stop();
                                } catch (Throwable error) {
                                    fail(error);
                                }
                            });
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> fail(error));
        }
    }

    private static void setupWorld(ServerLevel level) {
        try {
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            var player = level.players().getFirst();
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 14.5, 85, 31, CULLING_SMOKE ? 0 : 180, CULLING_SMOKE ? 0 : 21);
            if (COUPLER_SMOKE) {
                for (int x = 12; x <= 16; x++) {
                    level.setBlockAndUpdate(
                            new BlockPos(x, 83, 25), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(
                            new BlockPos(x, 84, 25),
                            Blocks.RAIL
                                    .defaultBlockState()
                                    .setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
                }
                var first = new LocomotiveMinecart(level, 13.7, 84.08, 25.5);
                var second = new JukeboxMinecart(level, 15.3, 84.08, 25.5);
                level.addFreshEntity(first);
                level.addFreshEntity(second);
                CartLanterns.hang(second, new ItemStack(Items.LANTERN), player);
                ItemStack saved = player.getMainHandItem();
                player.setItemInHand(
                        InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_CAT));
                second.interact(player, InteractionHand.MAIN_HAND);
                player.setItemInHand(InteractionHand.MAIN_HAND, saved);
                MinecartCouplings.get(level).link(first.getUUID(), second.getUUID());
                carts = new int[] {first.getId(), second.getId()};
                level.setBlockAndUpdate(
                        CART_LOADER, ModBlocks.CART_LOADER.get().defaultBlockState());
                level.setBlockAndUpdate(
                        CART_LOADER.west(3), ModBlocks.CART_LOADER.get().defaultBlockState());
                level.setBlockAndUpdate(
                        CART_LOADER.west(3).north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                var loader = (CartLoaderBlockEntity) level.getBlockEntity(CART_LOADER);
                loader.itemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 32), false);
                level.setBlockAndUpdate(MINE_CRATE, ModBlocks.MINE_CRATE.get().defaultBlockState());
                var crate = (MineCrateBlockEntity) level.getBlockEntity(MINE_CRATE);
                crate.storage().insertItem(0, new ItemStack(Items.DIAMOND, 32), false);
                BlockPos crateRail = MINE_CRATE.east(2);
                level.setBlockAndUpdate(crateRail.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(crateRail, Blocks.RAIL.defaultBlockState());
                var crateCart = ModEntityType.MINE_CRATE_MINECART.get().create(level);
                crateCart.setPos(Vec3.atBottomCenterOf(crateRail).add(0, .08, 0));
                crateCart.storage().insertItem(0, new ItemStack(Items.IRON_INGOT, 32), false);
                level.addFreshEntity(crateCart);
                crateCartId = crateCart.getId();
                if (CRATE_COUNT_ONLY) setupTanks(level);
            }
            int[] liftIds = new int[3], riderIds = new int[12];
            int index = 0;
            for (AbstractLift.Type type : AbstractLift.Type.values()) {
                double x = index * 14 + 0.5;
                AbstractLift lift =
                        AbstractLift.createLift(level, x, 74, 0.5, type, ItemStack.EMPTY, null);
                lift.setYRot(180);
                int column = 0;
                for (BlockPos guide : lift.getGuidePositions()) {
                    boolean motor = column++ < lift.getGuidePositions().size() / 2;
                    double dx = x - guide.getX() - 0.5, dz = 0.5 - guide.getZ() - 0.5;
                    Direction inward =
                            Math.abs(dx) > Math.abs(dz)
                                    ? (dx > 0 ? Direction.EAST : Direction.WEST)
                                    : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
                    for (int y = 68; y <= 89; ++y) {
                        BlockPos rail = guide.atY(y);
                        var block =
                                motor
                                        ? ModBlocks.POWERED_LIFT_TRACK.get()
                                        : (y == 76
                                                ? ModBlocks.DETECTOR_LIFT_TRACK.get()
                                                : ModBlocks.BASIC_LIFT_TRACK.get());
                        level.setBlockAndUpdate(
                                rail,
                                block.defaultBlockState()
                                        .setValue(HorizontalDirectionalBlock.FACING, inward));
                        level.setBlockAndUpdate(
                                rail.relative(inward.getOpposite()),
                                (motor ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState());
                    }
                }
                level.addFreshEntity(lift);
                liftIds[index] = lift.getId();
                Entity[] entities = {
                    EntityType.ARMOR_STAND.create(level),
                    EntityType.MINECART.create(level),
                    EntityType.SHEEP.create(level),
                    new net.minecraft.world.entity.item.ItemEntity(
                            level, x + 0.65, 75, -0.15, new ItemStack(Items.DIAMOND))
                };
                double[][] positions = {{-.65, -.65}, {.65, .65}, {-.65, .65}, {.65, -.65}};
                for (int n = 0; n < entities.length; ++n) {
                    Entity entity = entities[n];
                    entity.setPos(x + positions[n][0], 75, 0.5 + positions[n][1]);
                    if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.setOnGround(true);
                    level.addFreshEntity(entity);
                    riderIds[index * 4 + n] = entity.getId();
                }
                ++index;
            }
            riders = riderIds;
            lifts = liftIds;
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> fail(error));
        }
    }

    @SubscribeEvent
    public static void rendered(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("spelunkers_lifts.clientSmoke")
                || finished
                || lifts == null
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!((tick >= 32 && tick <= 60) || (tick >= 102 && tick <= 130))) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            ++frames;
            for (int i = 0; i < 3; ++i) {
                Entity lift = mc.level.getEntity(lifts[i]);
                if (lift == null) continue;
                double deck = Mth.lerp(partial, lift.yOld, lift.getY()) + lift.getBbHeight();
                for (int n = 0; n < 4; ++n) {
                    Entity rider = mc.level.getEntity(riders[i * 4 + n]);
                    if (rider == null) continue;
                    double feet =
                            Mth.lerp(partial, rider.yOld, rider.getY())
                                    + rider.getBoundingBox().minY
                                    - rider.getY();
                    double offset = LiftClientSupport.getRenderOffset(rider, partial);
                    maxUncorrected = Math.max(maxUncorrected, Math.abs(feet - deck));
                    maxError = Math.max(maxError, Math.abs(feet + offset - deck));
                    ++samples;
                }
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    @SubscribeEvent
    public static void pixels(RenderGuiEvent.Post event) {
        if (!Boolean.getBoolean("spelunkers_lifts.clientSmoke") || finished || lifts == null)
            return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (CRATE_COUNT_ONLY && tick >= 74 && tick <= 90 && mc.player.getX() > 24) {
                Path screenshot = Path.of("tank-fluids.png");
                if (!Files.exists(screenshot)) {
                    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                        image.writeToFile(screenshot);
                    }
                }
            }
            if (COUPLER_SMOKE && tick >= 123 && tick <= 139) {
                boolean block =
                        tick < 130
                                && mc.hitResult instanceof BlockHitResult hit
                                && hit.getType() == HitResult.Type.BLOCK
                                && hit.getBlockPos().equals(MINE_CRATE);
                boolean cart = tick >= 133 && mc.crosshairPickEntity instanceof MineCrateMinecart;
                if (block || cart) {
                    Path screenshot =
                            Path.of(
                                    block
                                            ? "mine-crate-block-count.png"
                                            : "mine-crate-cart-count.png");
                    if (!Files.exists(screenshot)) {
                        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                            image.writeToFile(screenshot);
                        }
                    }
                }
            }
            if (CRATE_COUNT_ONLY) return;
            if (COUPLER_SMOKE && tick >= 12 && tick <= 18) {
                Path screenshot = Path.of("cart-coupling.png");
                if (!Files.exists(screenshot))
                    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                        image.writeToFile(screenshot);
                    }
            }
            if (tick == 45 || tick == 115) {
                Path screenshot = Path.of(tick == 45 ? "lift-up.png" : "lift-down.png");
                if (!Files.exists(screenshot))
                    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                        image.writeToFile(screenshot);
                    }
            }
            if (tick < 30) return;
            Field loopsField =
                    net.luckystudio.spelunkers_charm.entity.custom.lift.LiftClientSounds.class
                            .getDeclaredField("SOUNDS");
            loopsField.setAccessible(true);
            Map<?, ?> loops = (Map<?, ?>) loopsField.get(null);
            int activeLoops = 0;
            for (Object sound : loops.values()) {
                var instance = (net.minecraft.client.resources.sounds.SoundInstance) sound;
                if (mc.getSoundManager().isActive(instance)) activeLoops++;
                Field owner = sound.getClass().getDeclaredField("lift");
                owner.setAccessible(true);
                var lift = (AbstractLift) owner.get(sound);
                int flag = lift.getBbWidth() < 4 ? 1 : lift.getBbWidth() < 6 ? 2 : 4;
                float expected = flag == 1 ? 1.2F : flag == 2 ? 1.0F : .8F;
                check(
                        Math.abs(instance.getPitch() - expected) < .00001,
                        "Wrong engine pitch for " + lift.getBbWidth());
                pitchMask |= flag;
            }
            if ((tick >= 32 && tick <= 60) || (tick >= 102 && tick <= 130)) {
                check(activeLoops > 0, "No active running sound while travelling");
                check(loops.size() <= 3, "Duplicate loops for one lift");
                loopChecks++;
            }
            if ((tick >= 80 && tick <= 85) || tick >= 150) {
                check(activeLoops == 0, "Loop kept playing after stop");
                silentChecks++;
            }
            int warm = 0;
            Object engine = mc.particleEngine;
            Field field = engine.getClass().getDeclaredField("particles");
            field.setAccessible(true);
            Map<?, ?> particles = (Map<?, ?>) field.get(engine);
            for (Object value : particles.values())
                for (Object entry : (Iterable<?>) value) {
                    if (entry instanceof CartLoadTrailParticle trail
                            && tick >= 105
                            && tick < 125
                            && seen.add(trail)) {
                        check(
                                number(trail, "yd") < 0 && number(trail, "y") < CART_LOADER.getY(),
                                "Cart Loader trail is not falling from the chute");
                        ++loadTrails;
                    }
                    if (!(entry instanceof GlowParticle particle)
                            || number(particle, "rCol") < .99
                            || Math.abs(number(particle, "bCol") - .15) > .001) continue;
                    ++warm;
                    if (seen.add(particle)) {
                        double vy = number(particle, "yd");
                        if (tick >= 30 && tick <= 64) {
                            check(vy < 0, "Ascending lift spark did not travel down: " + vy);
                            ++upSparks;
                        }
                        if (tick >= 100 && tick <= 134) {
                            check(vy > 0, "Descending lift spark did not travel up: " + vy);
                            ++downSparks;
                        }
                    }
                }
            if ((tick >= 80 && tick <= 85) || tick >= 150) {
                check(warm == 0, "Stopped lift kept emitting sparks: " + warm);
                ++stoppedChecks;
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static double number(Particle particle, String name) throws Exception {
        Field field = Particle.class.getDeclaredField(name);
        field.setAccessible(true);
        return ((Number) field.get(particle)).doubleValue();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void verifyRailwayClient(Minecraft mc) throws Exception {
        check(
                mc.level.getEntity(carts[0]) instanceof LocomotiveMinecart,
                "Locomotive was not tracked");
        JukeboxMinecart cart = (JukeboxMinecart) mc.level.getEntity(carts[1]);
        check(cart.isPlaying() && cart.hasRecord(), "Jukebox disc state was not synchronized");
        check(
                CartLanterns.getLantern(cart) == CartLantern.LANTERN,
                "Cart lantern was not synchronized");
        var soundManager = mc.getSoundManager();
        Field engineField = soundManager.getClass().getDeclaredField("soundEngine");
        engineField.setAccessible(true);
        Object engine = engineField.get(soundManager);
        Field ticksField = engine.getClass().getDeclaredField("tickingSounds");
        ticksField.setAccessible(true);
        var sounds = (java.util.List<?>) ticksField.get(engine);
        long songs =
                sounds.stream()
                        .filter(
                                sound ->
                                        sound.getClass()
                                                .getName()
                                                .startsWith(
                                                        JukeboxMinecartSounds.class.getName()
                                                                + "$"))
                        .map(sound -> (net.minecraft.client.resources.sounds.SoundInstance) sound)
                        .filter(soundManager::isActive)
                        .filter(
                                sound ->
                                        Math.abs(sound.getX() - cart.getX()) < 0.3
                                                && Math.abs(sound.getZ() - cart.getZ()) < 0.3)
                        .count();
        check(songs == 1, "Expected one active sound at the jukebox cart, got " + songs);
    }

    private static void fail(Throwable error) {
        if (finished) return;
        finished = true;
        error.printStackTrace();
        try {
            Files.writeString(REPORT, "FAIL " + error + "\n");
        } catch (Exception ignored) {
        }
        Minecraft.getInstance().stop();
    }
}
