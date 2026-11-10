package net.luckystudio.spelunkers_charm.entity.custom.lift;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** A server-observed support relation, independent of entity movement snapshots. */
public record LiftSupportPayload(int entityId, int liftId, float relativeY)
        implements CustomPacketPayload {
    public static final Type<LiftSupportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("spelunkers_charm", "lift_support"));
    public static final StreamCodec<ByteBuf, LiftSupportPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    LiftSupportPayload::entityId,
                    ByteBufCodecs.VAR_INT,
                    LiftSupportPayload::liftId,
                    ByteBufCodecs.FLOAT,
                    LiftSupportPayload::relativeY,
                    LiftSupportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = "spelunkers_charm", bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("lifts2")
                    .playToClient(
                            TYPE,
                            STREAM_CODEC,
                            (payload, context) ->
                                    context.enqueueWork(
                                            () ->
                                                    LiftClientSupport.accept(
                                                            context.player().level(), payload)));
        }
    }
}
