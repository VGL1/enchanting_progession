package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SRequestEnchant(ResourceLocation enchantId, int level, int targetSlot, boolean confirmed)
        implements CustomPacketPayload {

    public static final Type<C2SRequestEnchant> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "request_enchant"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestEnchant> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, C2SRequestEnchant::enchantId,
                    ByteBufCodecs.VAR_INT, C2SRequestEnchant::level,
                    ByteBufCodecs.VAR_INT, C2SRequestEnchant::targetSlot,
                    ByteBufCodecs.BOOL, C2SRequestEnchant::confirmed,
                    C2SRequestEnchant::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}