package com.vgl.disenchanting.enchant;

import com.vgl.disenchanting.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public final class EnchantMaterial {

    private EnchantMaterial() {}

    public static Item dustFor(EnchantmentEntry.MaterialTier tier) {
        return switch (tier) {
            case COMMON -> ModItems.ENCHANT_DUST.get();
            case RARE   -> ModItems.SPECTRAL_DUST.get();
            case EPIC   -> ModItems.VISION_DUST.get();
        };
    }

    public static Item shardFor(EnchantmentEntry.MaterialTier tier) {
        return switch (tier) {
            case COMMON -> ModItems.ENCHANT_SHARD.get();
            case RARE   -> ModItems.SPIRIT_SHARD.get();
            case EPIC   -> ModItems.RADIANT_SHARD.get();
        };
    }

    public static Component dustName(EnchantmentEntry.MaterialTier tier) {
        return switch (tier) {
            case COMMON -> Component.translatable("item.disen_chanting.enchant_dust");
            case RARE   -> Component.translatable("item.disen_chanting.spectral_dust");
            case EPIC   -> Component.translatable("item.disen_chanting.vision_dust");
        };
    }

    public static Component shardName(EnchantmentEntry.MaterialTier tier) {
        return switch (tier) {
            case COMMON -> Component.translatable("item.disen_chanting.enchant_shard");
            case RARE   -> Component.translatable("item.disen_chanting.spirit_shard");
            case EPIC   -> Component.translatable("item.disen_chanting.radiant_shard");
        };
    }
}