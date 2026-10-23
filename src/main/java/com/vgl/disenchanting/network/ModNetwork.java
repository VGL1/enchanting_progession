package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.cast.CastInstance;
import com.vgl.disenchanting.config.DisenChantingConfig;
import com.vgl.disenchanting.enchant.EnchantmentEntry;
import com.vgl.disenchanting.skill.PlayerSkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Map;

public final class ModNetwork {

    private ModNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(S2CStartCast.TYPE,    S2CStartCast.STREAM_CODEC,    ClientPayloadHandler::handleStartCast);
        registrar.playToClient(S2CCastProgress.TYPE, S2CCastProgress.STREAM_CODEC, ClientPayloadHandler::handleProgress);
        registrar.playToClient(S2CEndCast.TYPE,      S2CEndCast.STREAM_CODEC,      ClientPayloadHandler::handleEndCast);
        registrar.playToClient(S2CCancelCast.TYPE,   S2CCancelCast.STREAM_CODEC,   ClientPayloadHandler::handleCancel);
        registrar.playToClient(S2CSyncPool.TYPE,     S2CSyncPool.STREAM_CODEC,     ClientPayloadHandler::handleSyncPool);
        registrar.playToClient(S2CSyncSkill.TYPE,    S2CSyncSkill.STREAM_CODEC,    ClientPayloadHandler::handleSyncSkill);

        registrar.playToServer(C2SRequestEnchant.TYPE, C2SRequestEnchant.STREAM_CODEC, ServerPayloadHandler::handleRequestEnchant);
        registrar.playToServer(C2SDisenchantSlot.TYPE, C2SDisenchantSlot.STREAM_CODEC, ServerPayloadHandler::handleDisenchantSlot);
    }

    public static void sendStartCast(ServerPlayer player, CastInstance cast) {
        PacketDistributor.sendToPlayer(player, new S2CStartCast(cast.type, cast.totalTicks, cast.enchantId, cast.level));
    }
    public static void sendProgress(ServerPlayer player, int remaining) {
        PacketDistributor.sendToPlayer(player, new S2CCastProgress(remaining));
    }
    public static void sendEndCast(ServerPlayer player, boolean success, String message) {
        PacketDistributor.sendToPlayer(player, new S2CEndCast(success, message));
    }
    public static void sendCancelCast(ServerPlayer player, String reason, boolean failure) {
        PacketDistributor.sendToPlayer(player, new S2CCancelCast(reason, failure));
    }
    public static void sendSyncPool(ServerPlayer player, Map<ResourceLocation, EnchantmentEntry> pool) {
        PacketDistributor.sendToPlayer(player, new S2CSyncPool(pool));
    }
    public static void sendSyncSkill(ServerPlayer player, PlayerSkillData data) {
        PacketDistributor.sendToPlayer(player, new S2CSyncSkill(
                data.getLevel(),
                DisenChantingConfig.isSkillRequirementsEnabled()));
    }
}