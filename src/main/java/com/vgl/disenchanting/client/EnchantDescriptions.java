package com.vgl.disenchanting.client;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class EnchantDescriptions {

    private EnchantDescriptions() {}

    /**
     * Отображаемое имя чары. Ищет ключ "enchantment.<ns>.<path>".
     * Если ключа нет — возвращает prettified-версию path (aqua_affinity → Aqua Affinity).
     */
    public static Component name(ResourceLocation id) {
        String key = "enchantment." + id.getNamespace() + "." + id.getPath();
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.literal(prettify(id.getPath()));
    }

    /**
     * Ищет описание чары по ключам локализации.
     * Поддерживает оба формата:
     *   - enchantment.<ns>.<path>.desc         (EnchantmentDescriptions, старый)
     *   - enchantment.<ns>.<path>.description  (Spell Power, RPG Series, новый)
     *   - enchantment.<ns>.<path>.tooltip      (некоторые моды)
     */
    public static Component get(ResourceLocation id) {
        String base = "enchantment." + id.getNamespace() + "." + id.getPath();

        String[] keys = {
                base + ".description",
                base + ".desc",
                base + ".tooltip"
        };

        for (String key : keys) {
            if (I18n.exists(key)) {
                return Component.translatable(key);
            }
        }
        return null;
    }

    /** aqua_affinity → Aqua Affinity. */
    private static String prettify(String path) {
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }
}