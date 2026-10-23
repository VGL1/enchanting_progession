package com.vgl.disenchanting.client;

import com.vgl.disenchanting.enchant.EnchantmentEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public final class ClientPanelState {

    private static final Map<EnchantmentEntry.Category, Boolean> EXPANDED
            = new EnumMap<>(EnchantmentEntry.Category.class);

    private static String searchQuery = "";
    private static ResourceLocation selectedId = null;
    private static int selectedLevel = 1;

    static {
        for (var cat : EnchantmentEntry.Category.values()) EXPANDED.put(cat, true);
    }

    private ClientPanelState() {}

    public static boolean isExpanded(EnchantmentEntry.Category cat) {
        return !Boolean.FALSE.equals(EXPANDED.get(cat));
    }
    public static void toggle(EnchantmentEntry.Category cat) {
        EXPANDED.put(cat, !isExpanded(cat));
    }

    public static String getSearchQuery() { return searchQuery; }
    public static void setSearchQuery(String q) { searchQuery = q == null ? "" : q; }

    public static ResourceLocation getSelectedId() { return selectedId; }
    public static int getSelectedLevel() { return selectedLevel; }

    public static void setSelected(ResourceLocation id, int level) {
        selectedId = id;
        selectedLevel = Math.max(1, level);
    }

    public static void setSelectedLevel(int level) {
        selectedLevel = Math.max(1, level);
    }

    public static void clearSelected() {
        selectedId = null;
        selectedLevel = 1;
    }
}