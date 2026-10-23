package com.vgl.disenchanting.skill;

import com.vgl.disenchanting.config.DisenChantingConfig;
import com.vgl.disenchanting.enchant.EnchantmentEntry;
import com.vgl.disenchanting.network.ModNetwork;
import com.vgl.disenchanting.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class SkillManager {

    private SkillManager() {}

    public static PlayerSkillData get(Player player) {
        return player.getData(ModAttachments.PLAYER_SKILL.get());
    }

    public static int getLevel(Player player) { return get(player).getLevel(); }

    public static void setLevel(Player player, int level) {
        get(player).setLevel(level);
        sync(player);
    }

    public static void addLevels(Player player, int delta) {
        PlayerSkillData data = get(player);
        data.setLevel(data.getLevel() + delta);
        sync(player);
    }

    /** +1 уровень. Возвращает true, если уровень вырос. */
    public static boolean increaseLevel(Player player) {
        PlayerSkillData data = get(player);
        boolean ok = data.increaseLevel();
        if (ok) sync(player);
        return ok;
    }

    public static boolean canUse(Player player, int requiredSkill) {
        if (!DisenChantingConfig.isSkillRequirementsEnabled()) return true;
        if (requiredSkill <= 1) return true;
        return getLevel(player) >= requiredSkill;
    }

    /** Сколько уровней навыка даст успешное действие с этой чарой и уровнем. */
    public static int skillGainFor(Player player, EnchantmentEntry entry, int level) {
        if (!DisenChantingConfig.isSkillRequirementsEnabled()) return 1;
        int skill = getLevel(player);
        int req = entry.requiredSkillForLevel(level);
        int tier = EnchantmentEntry.colorTier(skill, req);
        return EnchantmentEntry.skillGainForTier(tier);
    }

    /** То же, но по уже готовому требованию (для распыления). */
    public static int skillGainForRequirement(Player player, int requiredSkill) {
        if (!DisenChantingConfig.isSkillRequirementsEnabled()) return 1;
        int skill = getLevel(player);
        int tier = EnchantmentEntry.colorTier(skill, requiredSkill);
        return EnchantmentEntry.skillGainForTier(tier);
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer sp) {
            ModNetwork.sendSyncSkill(sp, get(sp));
        }
    }
}