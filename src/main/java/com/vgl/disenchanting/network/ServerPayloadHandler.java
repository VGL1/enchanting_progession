package com.vgl.disenchanting.network;

import com.vgl.disenchanting.cast.CastManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {

    private static final int CAST_TICKS = 60;

    private ServerPayloadHandler() {}

    public static void handleRequestEnchant(C2SRequestEnchant payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            CastManager.startEnchant(player, payload.enchantId(), payload.level(),
                    CAST_TICKS, payload.targetSlot(), payload.confirmed());
        });
    }

    public static void handleDisenchantSlot(C2SDisenchantSlot payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            CastManager.startDisenchant(player, CAST_TICKS, payload.slotIndex());
        });
    }
}