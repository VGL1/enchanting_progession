package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.cast.CastType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record S2CStartCast(CastType castType, int totalTicks, ResourceLocation enchantId, int level)
        implements CustomPacketPayload {

    public static final Type<S2CStartCast> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "start_cast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CStartCast> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT.map(
                            i -> CastType.values()[i],
                            CastType::ordinal),
                    S2CStartCast::castType,

                    ByteBufCodecs.VAR_INT,
                    S2CStartCast::totalTicks,

                    // Ключевой фикс: enchantId может быть null (для DISENCHANT),
                    // поэтому кодируем через Optional.
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC)
                            .map(opt -> opt.orElse(null), Optional::ofNullable),
                    S2CStartCast::enchantId,

                    ByteBufCodecs.VAR_INT,
                    S2CStartCast::level,

                    S2CStartCast::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}