package com.vgl.disenchanting.client;

public final class ClientSkillCache {

    private static int level = 1;
    private static boolean requirementsEnabled = true;

    private ClientSkillCache() {}

    public static void set(int lvl, boolean reqEnabled) {
        level = lvl;
        requirementsEnabled = reqEnabled;
    }

    public static int getLevel() { return level; }
    public static boolean isRequirementsEnabled() { return requirementsEnabled; }

    public static final int MAX_LEVEL = 300;
}