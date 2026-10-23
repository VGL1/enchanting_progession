package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record C2SDisenchantSlot(int slotIndex) implements CustomPacketPayload {

    public static final Type<C2SDisenchantSlot> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "disenchant_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SDisenchantSlot> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, C2SDisenchantSlot::slotIndex,
                    C2SDisenchantSlot::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}