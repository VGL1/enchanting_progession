package com.vgl.disenchanting.enchant;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EnchantmentPool {

    private static volatile Map<ResourceLocation, EnchantmentEntry> POOL = Map.of();

    private EnchantmentPool() {}

    public static void set(Map<ResourceLocation, EnchantmentEntry> newPool) {
        POOL = Collections.unmodifiableMap(new HashMap<>(newPool));
    }

    public static EnchantmentEntry get(ResourceLocation id) {
        return POOL.get(id);
    }

    public static boolean contains(ResourceLocation id) {
        return POOL.containsKey(id);
    }

    public static Map<ResourceLocation, EnchantmentEntry> all() {
        return POOL;
    }

    public static int size() {
        return POOL.size();
    }

    public static synchronized void putIfAbsent(ResourceLocation id, EnchantmentEntry entry) {
        if (POOL.containsKey(id)) return;
        Map<ResourceLocation, EnchantmentEntry> copy = new HashMap<>(POOL);
        copy.put(id, entry);
        POOL = Collections.unmodifiableMap(copy);
    }

    /** Замена существующей записи (используется автоапгрейдом категории). */
    public static synchronized void replace(ResourceLocation id, EnchantmentEntry entry) {
        if (!POOL.containsKey(id)) return;
        Map<ResourceLocation, EnchantmentEntry> copy = new HashMap<>(POOL);
        copy.put(id, entry);
        POOL = Collections.unmodifiableMap(copy);
    }
}