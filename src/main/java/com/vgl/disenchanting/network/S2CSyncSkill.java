package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CSyncSkill(int level, boolean requirementsEnabled) implements CustomPacketPayload {

    public static final Type<S2CSyncSkill> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "sync_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncSkill> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, S2CSyncSkill::level,
                    ByteBufCodecs.BOOL, S2CSyncSkill::requirementsEnabled,
                    S2CSyncSkill::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}