package com.vgl.disenchanting.enchant;

import com.vgl.disenchanting.skill.PlayerSkillData;
import net.minecraft.resources.ResourceLocation;

public record EnchantmentEntry(
        ResourceLocation enchantment,
        int maxLevel,
        Rarity rarity,
        int dustPerLevel,
        int shardCost,
        Category category,
        int[] levelRequirements
) {
    // Пороги «отрыва» навыка от требования
    public static final int GRACE_ORANGE = 5;
    public static final int GRACE_YELLOW = 20;
    public static final int GRACE_GREEN  = 50;

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC;
        public static Rarity byName(String name) {
            if (name == null) return COMMON;
            for (Rarity r : values()) if (r.name().equalsIgnoreCase(name)) return r;
            return COMMON;
        }
    }

    public enum Category {
        ARMOR, WEAPON, RANGED, TOOL, SPELL, OTHER;
        public static Category byName(String name) {
            if (name == null) return OTHER;
            for (Category c : values()) if (c.name().equalsIgnoreCase(name)) return c;
            return OTHER;
        }
        public String displayName() {
            return switch (this) {
                case ARMOR  -> "Armor";
                case WEAPON -> "Melee Weapon";
                case RANGED -> "Ranged Weapon";
                case TOOL   -> "Tools";
                case SPELL  -> "Spell Power";
                case OTHER  -> "General";
            };
        }
    }

    public enum MaterialTier {
        COMMON, RARE, EPIC;
        public static MaterialTier forRarity(Rarity r) {
            return switch (r) {
                case COMMON, UNCOMMON -> COMMON;
                case RARE             -> RARE;
                case EPIC             -> EPIC;
            };
        }
    }

    public MaterialTier materialTier() { return MaterialTier.forRarity(rarity); }

    public int requiredSkill() {
        return levelRequirements.length > 0 ? levelRequirements[0] : 1;
    }

    public int requiredSkillForLevel(int level) {
        if (levelRequirements.length == 0) return 1;
        int idx = Math.max(0, Math.min(levelRequirements.length - 1, level - 1));
        return levelRequirements[idx];
    }

    public static int defaultRequiredSkill(Rarity rarity) {
        return switch (rarity) {
            case COMMON   -> 1;
            case UNCOMMON -> 20;
            case RARE     -> 60;
            case EPIC     -> 120;
        };
    }

    public static int[] generateLevelRequirements(int baseRequired, int maxLevel, Rarity rarity) {
        int[] arr = new int[Math.max(1, maxLevel)];
        arr[0] = baseRequired;
        int step = switch (rarity) {
            case COMMON   -> 15;
            case UNCOMMON -> 25;
            case RARE     -> 35;
            case EPIC     -> 45;
        };
        for (int i = 1; i < arr.length; i++) {
            arr[i] = Math.min(PlayerSkillData.MAX_LEVEL, arr[i - 1] + step);
        }
        return arr;
    }

    /**
     * Тир цвета по разнице навыка и требования.
     * 0 = locked (красный), 1 = orange (+3), 2 = yellow (+2), 3 = green (+1), 4 = gray (+0).
     */
    public static int colorTier(int skill, int req) {
        if (skill < req) return 0;
        int diff = skill - req;
        if (diff <= GRACE_ORANGE) return 1;
        if (diff <= GRACE_YELLOW) return 2;
        if (diff <= GRACE_GREEN)  return 3;
        return 4;
    }

    /** Опыт/уровни навыка за успешное действие с этим тиром. */
    public static int skillGainForTier(int tier) {
        return switch (tier) {
            case 1 -> 3;
            case 2 -> 2;
            case 3 -> 1;
            default -> 0;
        };
    }

    // ---- Стоимость ----

    private static double levelCurve(int level) {
        int L = Math.max(1, level);
        return L * (1.0 + (L - 1) * 0.05);
    }

    private static double countPenalty(int existingCount) {
        return Math.min(1.0 + Math.max(0, existingCount) * 0.05, 1.5);
    }

    public int baseDustFor(int level) {
        return (int) Math.ceil(dustPerLevel * levelCurve(level));
    }

    public int dustFor(int level, int existingEnchantCount) {
        return (int) Math.ceil(dustPerLevel * levelCurve(level) * countPenalty(existingEnchantCount));
    }

    public int baseShardsFor(int level) { return shardCost; }
    public int shardsFor(int level, int existingEnchantCount) { return shardCost; }

    public int dustFor(int level) { return baseDustFor(level); }
    public int shardsFor(int level) { return baseShardsFor(level); }
}