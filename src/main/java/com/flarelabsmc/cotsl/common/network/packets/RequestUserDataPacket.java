package com.flarelabsmc.cotsl.common.network.packets;

import com.flarelabsmc.cotsl.common.CotSL;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RequestUserDataPacket(UUID uuid) implements CustomPacketPayload {
    public static final Type<RequestUserDataPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CotSL.MOD_ID, "request_user_data"));

    public static final StreamCodec<ByteBuf, RequestUserDataPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    RequestUserDataPacket::uuid,
                    RequestUserDataPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}