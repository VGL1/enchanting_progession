package com.vgl.disenchanting.enchant;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;

public final class EnchantmentAutoLoader {

    private static final Logger LOGGER = DisenChantingMod.LOGGER;

    private EnchantmentAutoLoader() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var registry = event.getServer().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        int added = 0, upgraded = 0, skippedCurse = 0;
        Map<EnchantmentEntry.Category, Integer> counters = new EnumMap<>(EnchantmentEntry.Category.class);

        for (Holder.Reference<Enchantment> holder : registry.holders().toList()) {
            ResourceLocation id = holder.key().location();
            if (holder.is(EnchantmentTags.CURSE)) { skippedCurse++; continue; }

            Enchantment enchantment = holder.value();
            Enchantment.EnchantmentDefinition def = enchantment.definition();
            EnchantmentEntry.Category autoCat = resolveCategory(def.supportedItems());

            EnchantmentEntry existing = EnchantmentPool.get(id);
            if (existing != null) {
                if (existing.category() == EnchantmentEntry.Category.OTHER
                        && autoCat != EnchantmentEntry.Category.OTHER) {
                    EnchantmentEntry upgradedEntry = new EnchantmentEntry(
                            existing.enchantment(), existing.maxLevel(), existing.rarity(),
                            existing.dustPerLevel(), existing.shardCost(),
                            autoCat, existing.levelRequirements());
                    EnchantmentPool.replace(id, upgradedEntry);
                    upgraded++;
                }
                counters.merge(existing.category(), 1, Integer::sum);
                continue;
            }

            int weight = def.weight();
            int anvilCost = def.anvilCost();
            int maxLevel = def.maxLevel();
            int dustPerLevel = (int) Math.ceil(8.0 / Math.max(1, weight)) + Math.max(1, anvilCost / 2);
            int shardCost = Math.max(0, anvilCost / 4);
            EnchantmentEntry.Rarity rarity = rarityFromWeight(weight);
            int baseRequired = EnchantmentEntry.defaultRequiredSkill(rarity);

            int[] levelReqs = EnchantmentEntry.generateLevelRequirements(
                    baseRequired, maxLevel, rarity);

            EnchantmentEntry entry = new EnchantmentEntry(
                    id, maxLevel, rarity,
                    dustPerLevel, shardCost,
                    autoCat, levelReqs);

            EnchantmentPool.putIfAbsent(id, entry);
            added++;
            counters.merge(autoCat, 1, Integer::sum);
        }

        LOGGER.info("[Disen-Chanting] auto-loaded {} new, upgraded {} categories ({} curses skipped). Categories: {}",
                added, upgraded, skippedCurse, counters);
    }

    public static EnchantmentEntry.Category resolveCategory(HolderSet<Item> supported) {
        if (supported instanceof HolderSet.Named<Item> named) {
            EnchantmentEntry.Category exact = categoryForTag(named.key().location());
            if (exact != null) return exact;
        }
        if (intersectsTag(supported, "minecraft", "enchantable/bow")
                || intersectsTag(supported, "minecraft", "enchantable/crossbow")
                || intersectsTag(supported, "minecraft", "enchantable/trident"))
            return EnchantmentEntry.Category.RANGED;
        if (intersectsTag(supported, "spell_power", "enchantable/spell_power_generic")
                || intersectsTag(supported, "spell_power", "enchantable/critical_damage")
                || intersectsTag(supported, "spell_power", "enchantable/critical_chance")
                || intersectsTag(supported, "spell_power", "enchantable/haste")
                || intersectsTag(supported, "spell_power", "enchantable/spell_power_energize")
                || intersectsTag(supported, "spell_power", "enchantable/spell_power_soulfrost")
                || intersectsTag(supported, "spell_power", "enchantable/spell_power_sunfire"))
            return EnchantmentEntry.Category.SPELL;
        if (intersectsTag(supported, "minecraft", "enchantable/armor")
                || intersectsTag(supported, "minecraft", "enchantable/head_armor")
                || intersectsTag(supported, "minecraft", "enchantable/chest_armor")
                || intersectsTag(supported, "minecraft", "enchantable/leg_armor")
                || intersectsTag(supported, "minecraft", "enchantable/foot_armor")
                || intersectsTag(supported, "minecraft", "enchantable/equippable"))
            return EnchantmentEntry.Category.ARMOR;
        if (intersectsTag(supported, "minecraft", "enchantable/sword")
                || intersectsTag(supported, "minecraft", "enchantable/sharp_weapon")
                || intersectsTag(supported, "minecraft", "enchantable/fire_aspect")
                || intersectsTag(supported, "minecraft", "enchantable/mace")
                || intersectsTag(supported, "minecraft", "enchantable/weapon"))
            return EnchantmentEntry.Category.WEAPON;
        if (intersectsTag(supported, "minecraft", "enchantable/mining")
                || intersectsTag(supported, "minecraft", "enchantable/mining_loot")
                || intersectsTag(supported, "minecraft", "enchantable/fishing"))
            return EnchantmentEntry.Category.TOOL;
        return EnchantmentEntry.Category.OTHER;
    }

    private static EnchantmentEntry.Category categoryForTag(ResourceLocation key) {
        String ns = key.getNamespace();
        String path = key.getPath();
        if (ns.equals("minecraft")) {
            return switch (path) {
                case "enchantable/weapon", "enchantable/sword", "enchantable/sharp_weapon",
                     "enchantable/fire_aspect", "enchantable/mace", "enchantable/sweeping"
                        -> EnchantmentEntry.Category.WEAPON;
                case "enchantable/bow", "enchantable/crossbow", "enchantable/trident"
                        -> EnchantmentEntry.Category.RANGED;
                case "enchantable/mining", "enchantable/mining_loot", "enchantable/fishing"
                        -> EnchantmentEntry.Category.TOOL;
                case "enchantable/armor", "enchantable/head_armor", "enchantable/chest_armor",
                     "enchantable/leg_armor", "enchantable/foot_armor", "enchantable/equippable"
                        -> EnchantmentEntry.Category.ARMOR;
                case "enchantable/durability", "enchantable/vanishing"
                        -> EnchantmentEntry.Category.OTHER;
                default -> null;
            };
        }
        if (ns.equals("c")) {
            if (path.equals("tools/melee_weapon"))   return EnchantmentEntry.Category.WEAPON;
            if (path.equals("tools/ranged_weapon"))  return EnchantmentEntry.Category.RANGED;
            if (path.equals("tools/mining_tool"))    return EnchantmentEntry.Category.TOOL;
            if (path.equals("armors"))               return EnchantmentEntry.Category.ARMOR;
            if (path.equals("tools"))                return null;
        }
        if (ns.equals("rpg_series")) {
            if (path.startsWith("weapon_type/")) {
                String wt = path.substring("weapon_type/".length());
                return switch (wt) {
                    case "bow", "short_bow", "long_bow", "crossbow", "rapid_crossbow", "heavy_crossbow"
                            -> EnchantmentEntry.Category.RANGED;
                    case "healing_staff", "healing_wand", "spell_blade", "spell_staff"
                            -> EnchantmentEntry.Category.SPELL;
                    default -> EnchantmentEntry.Category.WEAPON;
                };
            }
            if (path.startsWith("armor_type/")) return EnchantmentEntry.Category.ARMOR;
            if (path.startsWith("tool_type/"))  return EnchantmentEntry.Category.TOOL;
        }
        if (ns.equals("spell_power") && path.startsWith("enchantable/")) {
            return EnchantmentEntry.Category.SPELL;
        }
        return null;
    }

    private static boolean intersectsTag(HolderSet<Item> supported, String namespace, String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (supported instanceof HolderSet.Named<Item> named && named.key().equals(tag)) return true;
        var tagOpt = BuiltInRegistries.ITEM.getTag(tag);
        if (tagOpt.isEmpty()) return false;
        for (Holder<Item> h : supported) {
            for (Holder<Item> tagH : tagOpt.get()) {
                if (h.value() == tagH.value()) return true;
            }
        }
        return false;
    }

    private static EnchantmentEntry.Rarity rarityFromWeight(int weight) {
        if (weight >= 10) return EnchantmentEntry.Rarity.COMMON;
        if (weight >= 5)  return EnchantmentEntry.Rarity.UNCOMMON;
        if (weight >= 2)  return EnchantmentEntry.Rarity.RARE;
        return EnchantmentEntry.Rarity.EPIC;
    }
}