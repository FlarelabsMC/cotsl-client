package com.flarelabsmc.cotsl.common.network.packets;

import com.flarelabsmc.cotsl.common.CotSL;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SendUserDataPacket(PermanentUser userData) implements CustomPacketPayload {
    public static final Type<SendUserDataPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CotSL.MOD_ID, "user_data_response"));

    public static final StreamCodec<ByteBuf, SendUserDataPacket> STREAM_CODEC =
            StreamCodec.composite(
                    PermanentUser.STREAM_CODEC,
                    SendUserDataPacket::userData,
                    SendUserDataPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}