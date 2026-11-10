package net.luckystudio.spelunkers_charm.network;

import io.netty.buffer.ByteBuf;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Tells clients about a minecart's extras: which carts it is coupled to (to draw the chain)
 * and which lantern hangs on it (to draw it and light it up with dynamic lights).
 */
public record CartSyncPayload(int cartId, List<Integer> linkedIds, int lantern) implements CustomPacketPayload {

    public static final Type<CartSyncPayload> TYPE = new Type<>(SpelunkersCharm.id("cart_sync"));

    public static final StreamCodec<ByteBuf, CartSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CartSyncPayload::cartId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CartSyncPayload::linkedIds,
            ByteBufCodecs.VAR_INT, CartSyncPayload::lantern,
            CartSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
