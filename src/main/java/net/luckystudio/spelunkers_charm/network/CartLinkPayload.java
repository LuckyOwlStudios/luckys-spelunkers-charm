package net.luckystudio.spelunkers_charm.network;

import io.netty.buffer.ByteBuf;
import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Tells clients which carts a minecart is coupled to, so the chain between them can be drawn. */
public record CartLinkPayload(int cartId, List<Integer> linkedIds) implements CustomPacketPayload {

    public static final Type<CartLinkPayload> TYPE = new Type<>(SpelunkersCharm.id("cart_links"));

    public static final StreamCodec<ByteBuf, CartLinkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CartLinkPayload::cartId,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), CartLinkPayload::linkedIds,
            CartLinkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
