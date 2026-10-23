package com.vgl.disenchanting.client;

import com.vgl.disenchanting.cast.CastType;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public final class ClientCastState {

    public static final int FAIL_DURATION = 12;

    private static boolean active = false;
    private static CastType type = CastType.ENCHANT;
    private static int totalTicks = 0;
    private static int remainingTicks = 0;
    private static ResourceLocation enchantId = null;
    private static int level = 0;

    private static boolean failing = false;
    private static int failTicks = 0;

    /** Системное время последнего апдейта — для локальной экстраполяции. */
    private static long lastUpdateMs = 0L;
    /** Сколько тиков было в remainingTicks на момент lastUpdateMs. */
    private static int lastSyncedRemaining = 0;

    private ClientCastState() {}

    public static void start(CastType t, int total, ResourceLocation id, int lvl) {
        active = true;
        type = t;
        totalTicks = total;
        remainingTicks = total;
        enchantId = id;
        level = lvl;
        failing = false;
        failTicks = 0;
        lastUpdateMs = Util.getMillis();
        lastSyncedRemaining = total;
    }

    public static void updateProgress(int remaining) {
        remainingTicks = remaining;
        lastSyncedRemaining = remaining;
        lastUpdateMs = Util.getMillis();
    }

    public static void stop() {
        active = false;
        totalTicks = 0;
        remainingTicks = 0;
        failing = false;
        failTicks = 0;
        lastUpdateMs = 0L;
        lastSyncedRemaining = 0;
    }

    public static void fail() {
        if (!active) return;
        failing = true;
        failTicks = FAIL_DURATION;
    }

    public static void tick() {
        if (failing && failTicks > 0) {
            failTicks--;
            if (failTicks <= 0) stop();
        }
    }

    /**
     * Текущее оставшееся время (в тиках, дробное) с учётом partial-tick
     * и локальной экстраполяции от последнего пакета.
     */
    public static float getSmoothedRemaining(float partialTick) {
        if (!active) return 0f;

        long now = Util.getMillis();
        float elapsedMs = now - lastUpdateMs;
        float elapsedTicks = elapsedMs / 50f; // 1 tick = 50ms

        float extrapolated = lastSyncedRemaining - elapsedTicks - partialTick;
        // Не даём уйти ниже нуля
        if (extrapolated < 0f) extrapolated = 0f;
        return extrapolated;
    }

    public static boolean isActive()      { return active; }
    public static boolean isFailing()     { return failing; }
    public static int getFailTicks()      { return failTicks; }
    public static CastType getType()      { return type; }
    public static int getTotalTicks()     { return totalTicks; }
    public static int getRemainingTicks() { return remainingTicks; }
    public static ResourceLocation getEnchantId() { return enchantId; }
    public static int getLevel()          { return level; }
}