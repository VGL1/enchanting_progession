package com.vgl.disenchanting.cast;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.enchant.DisenchantHelper;
import com.vgl.disenchanting.enchant.EnchantmentHelper;
import com.vgl.disenchanting.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CastManager {

    private static final Map<UUID, CastInstance> ACTIVE = new ConcurrentHashMap<>();
    private static final double MOVE_EPSILON_SQR = 0.001;
    private static final double JUMP_EPSILON = 0.001;

    private CastManager() {}

    public static boolean startEnchant(ServerPlayer player, ResourceLocation enchantId,
                                       int level, int ticks, int targetSlot, boolean confirmed) {
        if (ACTIVE.containsKey(player.getUUID())) return false;

        EnchantmentHelper.Result pre = EnchantmentHelper.validate(
                player, enchantId, level, targetSlot, confirmed);
        if (pre != EnchantmentHelper.Result.OK) {
            ModNetwork.sendEndCast(player, false, EnchantmentHelper.describe(pre));
            return false;
        }

        CastInstance cast = new CastInstance(CastType.ENCHANT, ticks, enchantId, level,
                targetSlot, confirmed, player.getX(), player.getY(), player.getZ());
        ACTIVE.put(player.getUUID(), cast);
        ModNetwork.sendStartCast(player, cast);
        return true;
    }

    public static boolean startDisenchant(ServerPlayer player, int ticks, int targetSlot) {
        if (ACTIVE.containsKey(player.getUUID())) return false;

        DisenchantHelper.Result pre = DisenchantHelper.validate(player, targetSlot);
        if (pre != DisenchantHelper.Result.OK) {
            ModNetwork.sendEndCast(player, false, DisenchantHelper.describe(pre));
            return false;
        }

        CastInstance cast = new CastInstance(CastType.DISENCHANT, ticks, null, 0,
                targetSlot, true, player.getX(), player.getY(), player.getZ());
        ACTIVE.put(player.getUUID(), cast);
        ModNetwork.sendStartCast(player, cast);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CastInstance cast = ACTIVE.get(player.getUUID());
        if (cast == null) return;

        double dx = player.getX() - cast.startX;
        double dy = player.getY() - cast.startY;
        double dz = player.getZ() - cast.startZ;

        boolean movedH = dx * dx + dz * dz > MOVE_EPSILON_SQR;
        boolean movedV = Math.abs(dy) > JUMP_EPSILON;

        if (movedH || movedV) {
            ACTIVE.remove(player.getUUID());
            ModNetwork.sendCancelCast(player,
                    "message.disen_chanting.result.cast_interrupted", true);
            return;
        }

        String failKey = validateCurrent(player, cast);
        if (failKey != null) {
            ACTIVE.remove(player.getUUID());
            ModNetwork.sendCancelCast(player, failKey, true);
            return;
        }

        cast.remainingTicks--;
        if (cast.remainingTicks <= 0) {
            ACTIVE.remove(player.getUUID());
            finish(player, cast);
        } else {
            ModNetwork.sendProgress(player, cast.remainingTicks);
        }
    }

    private static void finish(ServerPlayer player, CastInstance cast) {
        switch (cast.type) {
            case ENCHANT -> {
                EnchantmentHelper.Result r = EnchantmentHelper.applyEnchant(
                        player, cast.enchantId, cast.level, cast.targetSlot, cast.confirmed);
                ModNetwork.sendEndCast(player, r == EnchantmentHelper.Result.OK,
                        EnchantmentHelper.describe(r));
            }
            case DISENCHANT -> {
                DisenchantHelper.Result r = DisenchantHelper.disenchant(player, cast.targetSlot);
                ModNetwork.sendEndCast(player, r == DisenchantHelper.Result.OK,
                        DisenchantHelper.describe(r));
            }
        }
        DisenChantingMod.LOGGER.info("[Disen-Chanting] cast finished for {}",
                player.getName().getString());
    }

    public static void cancel(ServerPlayer player, String reasonKey) {
        if (ACTIVE.remove(player.getUUID()) != null) {
            ModNetwork.sendCancelCast(player, reasonKey, false);
        }
    }

    public static boolean isCasting(Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    private static String validateCurrent(ServerPlayer player, CastInstance cast) {
        switch (cast.type) {
            case ENCHANT -> {
                var r = EnchantmentHelper.validate(player, cast.enchantId, cast.level,
                        cast.targetSlot, true); // уже подтверждено на старте
                if (r != EnchantmentHelper.Result.OK) return EnchantmentHelper.describe(r);
            }
            case DISENCHANT -> {
                var r = DisenchantHelper.validate(player, cast.targetSlot);
                if (r != DisenchantHelper.Result.OK) return DisenchantHelper.describe(r);
            }
        }
        return null;
    }
}