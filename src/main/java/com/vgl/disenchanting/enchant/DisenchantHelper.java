package com.vgl.disenchanting.enchant;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.config.DisenChantingConfig;
import com.vgl.disenchanting.registry.ModItems;
import com.vgl.disenchanting.skill.SkillManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class DisenchantHelper {

    public enum Result {
        OK, NO_TARGET, IS_WAND, NO_ENCHANTS, NOT_DISENCHANTABLE, SKILL_TOO_LOW
    }

    private DisenchantHelper() {}

    private static ItemStack getTarget(Player player, int targetSlot) {
        if (targetSlot < 0) return player.getOffhandItem();
        var inv = player.getInventory();
        if (targetSlot >= inv.getContainerSize()) return ItemStack.EMPTY;
        return inv.getItem(targetSlot);
    }

    private static void setTarget(Player player, int targetSlot, ItemStack stack) {
        if (targetSlot < 0) {
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, stack);
        } else {
            player.getInventory().setItem(targetSlot, stack);
        }
        player.containerMenu.broadcastChanges();
    }

    public static Result validate(Player player, int targetSlot) {
        ItemStack target = getTarget(player, targetSlot);
        if (target.isEmpty()) return Result.NO_TARGET;
        if (isWand(target)) return Result.IS_WAND;

        ItemEnchantments enchants = target.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) return Result.NO_ENCHANTS;

        if (DisenChantingConfig.isSkillRequirementsEnabled()) {
            int required = maxRequiredSkill(player, enchants);
            if (SkillManager.getLevel(player) < required) return Result.SKILL_TOO_LOW;
        }

        return Result.OK;
    }

    public static Result disenchant(Player player, int targetSlot) {
        Result pre = validate(player, targetSlot);
        if (pre != Result.OK) return pre;

        ItemStack target = getTarget(player, targetSlot);
        ItemEnchantments enchants = target.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        String targetName = target.getHoverName().getString();
        EnchantmentEntry.Rarity rarity = computeRarity(player, enchants);
        Reward reward = rollReward(player.getRandom(), rarity);

        var tier = EnchantmentEntry.MaterialTier.forRarity(rarity);
        Item dustItem  = EnchantMaterial.dustFor(tier);
        Item shardItem = EnchantMaterial.shardFor(tier);

        int required = maxRequiredSkill(player, enchants);

        setTarget(player, targetSlot, ItemStack.EMPTY);

        giveOrDrop(player, dustItem, reward.dust);
        if (reward.shards > 0) {
            giveOrDrop(player, shardItem, reward.shards);
        }

        int gain = SkillManager.skillGainForRequirement(player, required);
        if (gain > 0) {
            int oldLevel = SkillManager.getLevel(player);
            SkillManager.addLevels(player, gain);
            int newLevel = SkillManager.getLevel(player);
            if (newLevel > oldLevel) {
                player.displayClientMessage(
                        Component.translatable("message.disen_chanting.skill_levelup", newLevel)
                                .withStyle(ChatFormatting.BLUE),
                        true);
            }
        }

        DisenChantingMod.LOGGER.info(
                "[Disen-Chanting] {} disenchanted {} -> {} dust, {} shards (rarity={}, skill +{})",
                player.getName().getString(), targetName,
                reward.dust, reward.shards, rarity, gain);

        return Result.OK;
    }

    private static boolean isWand(ItemStack s) {
        return s.is(ModItems.ENCHANTING_WAND.get())
                || s.is(ModItems.DISENCHANTING_WAND.get());
    }

    private static int maxRequiredSkill(Player player, ItemEnchantments enchants) {
        var registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        int max = 1;
        for (var e : enchants.entrySet()) {
            Holder<Enchantment> h = e.getKey();
            int lvl = e.getIntValue();
            ResourceLocation id;
            try { id = registry.getKey(h.value()); } catch (Exception ex) { continue; }
            if (id == null) continue;
            EnchantmentEntry entry = EnchantmentPool.get(id);
            if (entry != null) max = Math.max(max, entry.requiredSkillForLevel(lvl));
        }
        return max;
    }

    private static EnchantmentEntry.Rarity computeRarity(Player player, ItemEnchantments enchants) {
        var registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        EnchantmentEntry.Rarity best = EnchantmentEntry.Rarity.COMMON;
        for (Holder<Enchantment> h : enchants.keySet()) {
            ResourceLocation id;
            try { id = registry.getKey(h.value()); } catch (Exception ex) { continue; }
            if (id == null) continue;
            EnchantmentEntry e = EnchantmentPool.get(id);
            EnchantmentEntry.Rarity r = (e != null) ? e.rarity() : EnchantmentEntry.Rarity.COMMON;
            if (r.ordinal() > best.ordinal()) best = r;
        }
        return best;
    }

    private record Reward(int dust, int shards) {}

    private static Reward rollReward(RandomSource rnd, EnchantmentEntry.Rarity rarity) {
        return switch (rarity) {
            case COMMON   -> new Reward(1 + rnd.nextInt(2), 0);
            case UNCOMMON -> new Reward(2 + rnd.nextInt(3), rnd.nextFloat() < 0.20f ? 1 : 0);
            case RARE     -> new Reward(4 + rnd.nextInt(3), rnd.nextFloat() < 0.50f ? 1 : 0);
            case EPIC     -> new Reward(6 + rnd.nextInt(5), 1 + rnd.nextInt(2));
        };
    }

    private static void giveOrDrop(Player player, Item item, int count) {
        if (count <= 0) return;
        ItemStack s = new ItemStack(item, count);
        if (!player.getInventory().add(s)) player.drop(s, false);
    }

    public static String describe(Result r) {
        return switch (r) {
            case OK -> "";
            case NO_TARGET -> "message.disen_chanting.result.no_target";
            case IS_WAND -> "message.disen_chanting.result.is_wand";
            case NO_ENCHANTS -> "message.disen_chanting.result.no_enchants";
            case NOT_DISENCHANTABLE -> "message.disen_chanting.result.not_disenchantable";
            case SKILL_TOO_LOW -> "message.disen_chanting.result.skill_too_low";
        };
    }
}