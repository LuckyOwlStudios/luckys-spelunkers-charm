// Minecart train payload.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

public record MinecartTrainPayload(long tick, List<Cart> carts) implements CustomPacketPayload {
    public static final Type<MinecartTrainPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("spelunkers_charm", "train_frame"));
    public static final StreamCodec<FriendlyByteBuf, MinecartTrainPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, frame) -> {
                        buffer.writeLong(frame.tick());
                        buffer.writeVarInt(frame.carts().size());
                        for (var cart : frame.carts()) {
                            buffer.writeVarInt(cart.id());
                            buffer.writeVec3(cart.position());
                            buffer.writeFloat((float) cart.axis().x);
                            buffer.writeFloat((float) cart.axis().y);
                            buffer.writeFloat((float) cart.axis().z);
                        }
                    },
                    buffer -> {
                        long tick = buffer.readLong();
                        int count = buffer.readVarInt();
                        if (count < 0 || count > 512)
                            throw new IllegalArgumentException("Invalid train frame size");
                        var carts = new ArrayList<Cart>(count);
                        for (int i = 0; i < count; i++)
                            carts.add(
                                    new Cart(
                                            buffer.readVarInt(),
                                            buffer.readVec3(),
                                            new Vec3(
                                                    buffer.readFloat(),
                                                    buffer.readFloat(),
                                                    buffer.readFloat())));
                        return new MinecartTrainPayload(tick, List.copyOf(carts));
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Stores a tracked cart's position and physical front axis. */
    public record Cart(int id, Vec3 position, Vec3 axis) {}

    @EventBusSubscriber(modid = "spelunkers_charm", bus = EventBusSubscriber.Bus.MOD)
    /** Registers synchronized train frames for modded clients. */
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("trains1")
                    .playToClient(
                            TYPE,
                            STREAM_CODEC,
                            (payload, context) ->
                                    context.enqueueWork(
                                            () ->
                                                    MinecartTrainInterpolation.accept(
                                                            context.player().level(), payload)));
        }
    }
}
