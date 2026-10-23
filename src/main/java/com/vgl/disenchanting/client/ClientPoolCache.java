package com.vgl.disenchanting.client;

import com.vgl.disenchanting.enchant.EnchantmentEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

public final class ClientPoolCache {

    private static volatile Map<ResourceLocation, EnchantmentEntry> POOL = Map.of();

    public static void set(Map<ResourceLocation, EnchantmentEntry> pool) {
        POOL = Collections.unmodifiableMap(pool);
    }

    public static Map<ResourceLocation, EnchantmentEntry> all() {
        return POOL;
    }

    public static boolean isEmpty() {
        return POOL.isEmpty();
    }

    private ClientPoolCache() {}
}