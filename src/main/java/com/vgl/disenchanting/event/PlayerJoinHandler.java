package com.vgl.disenchanting.event;

import com.vgl.disenchanting.enchant.EnchantmentPool;
import com.vgl.disenchanting.network.ModNetwork;
import com.vgl.disenchanting.skill.SkillManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerJoinHandler {

    private PlayerJoinHandler() {}

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ModNetwork.sendSyncPool(sp, EnchantmentPool.all());
            ModNetwork.sendSyncSkill(sp, SkillManager.get(sp));
        }
    }
}