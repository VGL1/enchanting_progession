package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CEndCast(boolean success, String message) implements CustomPacketPayload {

    public static final Type<S2CEndCast> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "end_cast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CEndCast> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, S2CEndCast::success,
                    ByteBufCodecs.STRING_UTF8, S2CEndCast::message,
                    S2CEndCast::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}