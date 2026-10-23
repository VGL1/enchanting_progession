package com.vgl.disenchanting.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DisenChantingConfig {

    public static final ModConfigSpec SPEC;

    // gameplay — читается сервером, синкается клиенту
    public static final ModConfigSpec.BooleanValue SKILL_REQUIREMENTS_ENABLED;

    // UI — читается клиентом локально
    public static final ModConfigSpec.BooleanValue LEVEL_SELECTION_ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_LOCKED_ENCHANTMENTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Enchanting skill system").push("skill");
        SKILL_REQUIREMENTS_ENABLED = b
                .comment("If false, all enchantments are available regardless of skill level.")
                .define("requirementsEnabled", true);
        b.pop();

        b.comment("Client-side UI").push("ui");
        LEVEL_SELECTION_ENABLED = b
                .comment("Show +/- buttons to pick enchantment level manually.",
                        "If false, level is auto-selected to the highest available.")
                .define("levelSelectionEnabled", false);

        SHOW_LOCKED_ENCHANTMENTS = b
                .comment("Show enchantments that the player cannot yet use (in red).",
                        "If false, they are hidden.")
                .define("showLockedEnchantments", false);
        b.pop();

        SPEC = b.build();
    }

    private DisenChantingConfig() {}

    public static boolean isSkillRequirementsEnabled() {
        try { return SKILL_REQUIREMENTS_ENABLED.get(); } catch (Throwable t) { return true; }
    }

    public static boolean isLevelSelectionEnabled() {
        try { return LEVEL_SELECTION_ENABLED.get(); } catch (Throwable t) { return false; }
    }

    public static boolean isShowLockedEnchantments() {
        try { return SHOW_LOCKED_ENCHANTMENTS.get(); } catch (Throwable t) { return false; }
    }
}