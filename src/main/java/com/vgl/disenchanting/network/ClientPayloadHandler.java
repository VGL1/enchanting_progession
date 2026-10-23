package com.vgl.disenchanting.network;

import com.vgl.disenchanting.client.ClientCastState;
import com.vgl.disenchanting.client.ClientPoolCache;
import com.vgl.disenchanting.client.ClientSkillCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {

    private ClientPayloadHandler() {}

    public static void handleStartCast(S2CStartCast payload, IPayloadContext ctx) {
        ClientCastState.start(payload.castType(), payload.totalTicks(), payload.enchantId(), payload.level());
    }
    public static void handleProgress(S2CCastProgress payload, IPayloadContext ctx) {
        ClientCastState.updateProgress(payload.remainingTicks());
    }
    public static void handleEndCast(S2CEndCast payload, IPayloadContext ctx) {
        ClientCastState.stop();
        if (payload.success()) return;
        String key = payload.message();
        if (key == null || key.isEmpty()) return;
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        });
    }
    public static void handleCancel(S2CCancelCast payload, IPayloadContext ctx) {
        String key = payload.reason();
        if (payload.failure()) {
            ClientCastState.fail();
            if (key != null && !key.isEmpty()) {
                ctx.enqueueWork(() -> {
                    var player = Minecraft.getInstance().player;
                    if (player != null) player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
                });
            }
        } else {
            ClientCastState.stop();
        }
    }
    public static void handleSyncPool(S2CSyncPool payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPoolCache.set(payload.pool()));
    }
    public static void handleSyncSkill(S2CSyncSkill payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSkillCache.set(payload.level(), payload.requirementsEnabled()));
    }
}