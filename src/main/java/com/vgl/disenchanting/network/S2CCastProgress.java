package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CCastProgress(int remainingTicks) implements CustomPacketPayload {

    public static final Type<S2CCastProgress> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "cast_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CCastProgress> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, S2CCastProgress::remainingTicks,
                    S2CCastProgress::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}