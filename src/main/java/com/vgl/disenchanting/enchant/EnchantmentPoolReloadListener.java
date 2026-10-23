package com.vgl.disenchanting.enchant;

import com.google.gson.*;
import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentPoolReloadListener extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "enchantment_pool";
    private static final Gson GSON = new Gson();

    public EnchantmentPoolReloadListener() { super(GSON, DIRECTORY); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, EnchantmentEntry> newPool = new HashMap<>();
        int ok = 0, bad = 0;

        for (Map.Entry<ResourceLocation, JsonElement> e : entries.entrySet()) {
            ResourceLocation fileId = e.getKey();
            try {
                JsonObject json = GsonHelper.convertToJsonObject(e.getValue(), "enchantment_pool entry");

                ResourceLocation enchantId = ResourceLocation.parse(
                        GsonHelper.getAsString(json, "enchantment"));

                int maxLevel      = GsonHelper.getAsInt(json, "max_level", 1);
                String rarityStr  = GsonHelper.getAsString(json, "rarity", "common");
                int dustPerLevel  = GsonHelper.getAsInt(json, "dust_per_level", 1);
                int shardCost     = GsonHelper.getAsInt(json, "shard_cost", 0);
                String categoryStr = GsonHelper.getAsString(json, "category", "other");

                EnchantmentEntry.Rarity rarity = EnchantmentEntry.Rarity.byName(rarityStr);
                int baseRequired = GsonHelper.getAsInt(json, "required_skill",
                        EnchantmentEntry.defaultRequiredSkill(rarity));

                // ---- level_requirements (опционально) ----
                int[] levelReqs;
                if (json.has("level_requirements")) {
                    JsonArray arr = json.getAsJsonArray("level_requirements");
                    levelReqs = new int[arr.size()];
                    for (int i = 0; i < arr.size(); i++) {
                        levelReqs[i] = Math.max(1, arr.get(i).getAsInt());
                    }
                } else {
                    levelReqs = EnchantmentEntry.generateLevelRequirements(
                            Math.max(1, baseRequired), Math.max(1, maxLevel), rarity);
                }

                if (maxLevel < 1) maxLevel = 1;
                if (dustPerLevel < 0) dustPerLevel = 0;
                if (shardCost < 0) shardCost = 0;

                EnchantmentEntry entry = new EnchantmentEntry(
                        enchantId, maxLevel, rarity,
                        dustPerLevel, shardCost,
                        EnchantmentEntry.Category.byName(categoryStr),
                        levelReqs);

                if (newPool.put(enchantId, entry) != null) {
                    DisenChantingMod.LOGGER.warn(
                            "[Disen-Chanting] duplicate enchantment id {} (from {})", enchantId, fileId);
                }
                ok++;
            } catch (Exception ex) {
                bad++;
                DisenChantingMod.LOGGER.error(
                        "[Disen-Chanting] failed to parse pool entry {}", fileId, ex);
            }
        }

        EnchantmentPool.set(newPool);
        DisenChantingMod.LOGGER.info(
                "[Disen-Chanting] enchantment pool loaded: {} ok, {} bad, total {}",
                ok, bad, newPool.size());

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (var p : server.getPlayerList().getPlayers()) {
                ModNetwork.sendSyncPool(p, newPool);
            }
        }
    }
}