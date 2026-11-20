package net.luckystudio.spelunkers_charm.worldgen.structure;

import com.mojang.logging.LogUtils;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.LiftSubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Lifts are Sable sub-levels, so they can't be saved inside a structure template. Instead the
 * elevator mine's start piece has an empty 7x7 deck area, and once the new chunks are loaded this
 * assembles a large lift into it.
 */
@EventBusSubscriber(modid = SpelunkersCharm.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ElevatorMineLifts {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation STRUCTURE = SpelunkersCharm.id("elevator_mine");
    /** The deck centre sits this far in from the start piece's corner (template is 11x3x11). */
    private static final int CENTER_OFFSET = 5;
    private static final int RADIUS = 3;
    /** Give up after five minutes if the chunks around the deck never finish loading. */
    private static final int MAX_WAIT_TICKS = 6000;

    private record Pending(ResourceKey<Level> dimension, BlockPos center, int[] waited) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    private ElevatorMineLifts() {}

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (!event.isNewChunk()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            if (!STRUCTURE.equals(registry.getKey(entry.getKey()))) continue;
            StructureStart start = entry.getValue();
            if (!start.isValid() || start.getPieces().isEmpty()) continue;
            StructurePiece first = start.getPieces().get(0);
            BoundingBox box = first.getBoundingBox();
            BlockPos center = new BlockPos(box.minX() + CENTER_OFFSET, box.minY(), box.minZ() + CENTER_OFFSET);
            synchronized (PENDING) {
                PENDING.add(new Pending(level.dimension(), center, new int[1]));
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending pending = it.next();
                ServerLevel level = event.getServer().getLevel(pending.dimension());
                if (level == null || ++pending.waited()[0] > MAX_WAIT_TICKS) {
                    it.remove();
                    continue;
                }
                BlockPos c = pending.center();
                // The deck and the tracks around it must all be loaded.
                if (!level.hasChunksAt(c.offset(-RADIUS - 2, 0, -RADIUS - 2), c.offset(RADIUS + 2, 3, RADIUS + 2)))
                    continue;
                it.remove();
                place(level, c);
            }
        }
    }

    private static void place(ServerLevel level, BlockPos center) {
        AbstractLift.Type type = AbstractLift.Type.LARGE;
        AbstractLift probe = null;
        Direction facing = Direction.NORTH;
        for (int turn = 0; turn < 4; ++turn) {
            AbstractLift candidate =
                    AbstractLift.createLift(
                            level, center.getX() + 0.5, center.getY(), center.getZ() + 0.5, type, ItemStack.EMPTY, null);
            candidate.setYRot(facing.toYRot());
            candidate.setYHeadRot(facing.toYRot());
            if (probe == null) probe = candidate;
            if (candidate.hasValidTracks()) {
                probe = candidate;
                break;
            }
            facing = facing.getClockWise();
        }
        if (!LiftSubLevels.assemble(level, probe, type))
            LOGGER.warn("Could not place the lift for the elevator mine at {}", center);
    }
}
