package net.luckystudio.spelunkers_charm.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Reconcile saved input latches, including stations loaded from the old level-triggered version.
 */
@EventBusSubscriber(modid = "spelunkers_charm")
public final class LiftTrackLoadEvents {
    private LiftTrackLoadEvents() {}

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        // Queue explicitly: execute() runs inline on the server thread, before
        // the chunk-load callback has necessarily finished publishing the chunk.
        level.getServer()
                .tell(
                        new net.minecraft.server.TickTask(
                                level.getServer().getTickCount(),
                                () -> {
                                    if (!level.hasChunk(chunk.getPos().x, chunk.getPos().z)) return;
                                    int originX = chunk.getPos().getMinBlockX(),
                                            originZ = chunk.getPos().getMinBlockZ();
                                    var sections = chunk.getSections();
                                    for (int section = 0; section < sections.length; section++) {
                                        var states = sections[section];
                                        if (!states.maybeHas(
                                                state ->
                                                        state.hasProperty(
                                                                DirectionalLiftTrackBlock
                                                                        .SIGNAL_HELD))) continue;
                                        int originY = chunk.getMinBuildHeight() + section * 16;
                                        for (int x = 0; x < 16; x++)
                                            for (int y = 0; y < 16; y++)
                                                for (int z = 0; z < 16; z++) {
                                                    var state = states.getBlockState(x, y, z);
                                                    if (!state.hasProperty(
                                                            DirectionalLiftTrackBlock.SIGNAL_HELD))
                                                        continue;
                                                    BlockPos pos =
                                                            new BlockPos(
                                                                    originX + x, originY + y,
                                                                    originZ + z);
                                                    if (!level.getBlockTicks()
                                                            .hasScheduledTick(
                                                                    pos, state.getBlock()))
                                                        level.scheduleTick(
                                                                pos, state.getBlock(), 1);
                                                }
                                    }
                                }));
    }
}
