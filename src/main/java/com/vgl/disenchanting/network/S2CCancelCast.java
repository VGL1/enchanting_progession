package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CCancelCast(String reason, boolean failure) implements CustomPacketPayload {

    public static final Type<S2CCancelCast> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "cancel_cast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCancelCast> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, S2CCancelCast::reason,
                    ByteBufCodecs.BOOL, S2CCancelCast::failure,
                    S2CCancelCast::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}