package com.vgl.disenchanting.enchant;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.config.DisenChantingConfig;
import com.vgl.disenchanting.skill.SkillManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

public final class EnchantmentHelper {

    public enum Result {
        OK, UNKNOWN_ENCHANTMENT, LEVEL_OUT_OF_RANGE,
        NOT_ENOUGH_DUST, NOT_ENOUGH_SHARDS, NO_TARGET,
        INCOMPATIBLE_TARGET, ENCHANTMENT_REGISTRY_MISSING,
        ALREADY_HIGHER, SKILL_TOO_LOW, NEEDS_CONFIRMATION
    }

    private EnchantmentHelper() {}

    /** targetSlot: -1 = офф-хенд, >=0 = индекс в player.getInventory(). */
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

    public static Result validate(Player player, ResourceLocation enchantId,
                                  int level, int targetSlot, boolean confirmed) {
        EnchantmentEntry entry = EnchantmentPool.get(enchantId);
        if (entry == null) return Result.UNKNOWN_ENCHANTMENT;
        if (level < 1 || level > entry.maxLevel()) return Result.LEVEL_OUT_OF_RANGE;

        if (DisenChantingConfig.isSkillRequirementsEnabled()
                && SkillManager.getLevel(player) < entry.requiredSkillForLevel(level)) {
            return Result.SKILL_TOO_LOW;
        }

        ItemStack target = getTarget(player, targetSlot);
        if (target.isEmpty()) return Result.NO_TARGET;

        var registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> opt =
                registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantId));
        if (opt.isEmpty()) return Result.ENCHANTMENT_REGISTRY_MISSING;
        Holder<Enchantment> holder = opt.get();

        if (!isCompatible(holder.value(), target)) return Result.INCOMPATIBLE_TARGET;

        ItemEnchantments current = target.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int existingLevel = current.getLevel(holder);

        if (existingLevel > level) return Result.ALREADY_HIGHER;

        int existingCount = current.size();
        var tier = entry.materialTier();
        Item dustItem  = EnchantMaterial.dustFor(tier);
        Item shardItem = EnchantMaterial.shardFor(tier);
        int dustCost  = entry.dustFor(level, existingCount);
        int shardCost = entry.shardsFor(level, existingCount);

        if (countItem(player, dustItem) < dustCost) return Result.NOT_ENOUGH_DUST;
        if (countItem(player, shardItem) < shardCost) return Result.NOT_ENOUGH_SHARDS;

        if (existingLevel == level && !confirmed) return Result.NEEDS_CONFIRMATION;

        return Result.OK;
    }

    public static Result applyEnchant(Player player, ResourceLocation enchantId,
                                      int level, int targetSlot, boolean confirmed) {
        Result pre = validate(player, enchantId, level, targetSlot, confirmed);
        if (pre != Result.OK) return pre;

        EnchantmentEntry entry = EnchantmentPool.get(enchantId);
        ItemStack target = getTarget(player, targetSlot);
        if (target.isEmpty()) return Result.NO_TARGET;

        var registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holder = registry
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantId))
                .orElseThrow();

        ItemEnchantments current = target.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int existingCount = current.size();
        var tier = entry.materialTier();
        Item dustItem  = EnchantMaterial.dustFor(tier);
        Item shardItem = EnchantMaterial.shardFor(tier);
        int dustCost  = entry.dustFor(level, existingCount);
        int shardCost = entry.shardsFor(level, existingCount);

        consumeItem(player, dustItem,  dustCost);
        consumeItem(player, shardItem, shardCost);

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.set(holder, level);
        ItemStack updated = target.copy();
        updated.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        setTarget(player, targetSlot, updated);

        int gain = SkillManager.skillGainFor(player, entry, level);
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
                "[Disen-Chanting] {} applied {} level {} to {} (existing={}, tier={}, dust -{}, shards -{}, skill +{}, confirmed={})",
                player.getName().getString(), enchantId, level,
                updated.getHoverName().getString(),
                existingCount, tier, dustCost, shardCost, gain, confirmed);

        return Result.OK;
    }

    public static boolean isCompatible(Enchantment enchantment, ItemStack target) {
        if (target.isEmpty()) return false;
        Holder<Item> itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(target.getItem());
        return enchantment.definition().supportedItems().contains(itemHolder);
    }

    public static boolean canApply(ResourceLocation enchantId, ItemStack target, RegistryAccess access) {
        if (target.isEmpty()) return false;
        var reg = access.registryOrThrow(Registries.ENCHANTMENT);
        var opt = reg.getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantId));
        return opt.isPresent() && isCompatible(opt.get().value(), target);
    }

    public static int countItem(Player player, Item item) {
        int total = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    private static void consumeItem(Player player, Item item, int amount) {
        if (amount <= 0) return;
        int remaining = amount;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        player.containerMenu.broadcastChanges();
    }

    public static String describe(Result r) {
        return switch (r) {
            case OK -> "";
            case UNKNOWN_ENCHANTMENT -> "message.disen_chanting.result.unknown_enchantment";
            case LEVEL_OUT_OF_RANGE -> "message.disen_chanting.result.level_out_of_range";
            case NOT_ENOUGH_DUST -> "message.disen_chanting.result.not_enough_dust";
            case NOT_ENOUGH_SHARDS -> "message.disen_chanting.result.not_enough_shards";
            case NO_TARGET -> "message.disen_chanting.result.no_target";
            case INCOMPATIBLE_TARGET -> "message.disen_chanting.result.incompatible_target";
            case ENCHANTMENT_REGISTRY_MISSING -> "message.disen_chanting.result.enchantment_registry_missing";
            case ALREADY_HIGHER -> "message.disen_chanting.result.already_higher";
            case SKILL_TOO_LOW -> "message.disen_chanting.result.skill_too_low";
            case NEEDS_CONFIRMATION -> "message.disen_chanting.result.needs_confirmation";
        };
    }
}