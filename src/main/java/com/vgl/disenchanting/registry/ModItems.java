package com.vgl.disenchanting.registry;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.item.DisenchantingWandItem;
import com.vgl.disenchanting.item.EnchantingWandItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(DisenChantingMod.MODID);

    // ---- COMMON ----
    public static final DeferredItem<Item> ENCHANT_DUST =
            ITEMS.registerSimpleItem("enchant_dust");
    public static final DeferredItem<Item> ENCHANT_SHARD =
            ITEMS.registerSimpleItem("enchant_shard");

    // ---- RARE ----
    public static final DeferredItem<Item> SPECTRAL_DUST =
            ITEMS.registerSimpleItem("spectral_dust");
    public static final DeferredItem<Item> SPIRIT_SHARD =
            ITEMS.registerSimpleItem("spirit_shard");

    // ---- EPIC ----
    public static final DeferredItem<Item> VISION_DUST =
            ITEMS.registerSimpleItem("vision_dust");
    public static final DeferredItem<Item> RADIANT_SHARD =
            ITEMS.registerSimpleItem("radiant_shard");

    // ---- Жезлы ----
    public static final DeferredItem<Item> ENCHANTING_WAND =
            ITEMS.register("enchanting_wand",
                    () -> new EnchantingWandItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DISENCHANTING_WAND =
            ITEMS.register("disenchanting_wand",
                    () -> new DisenchantingWandItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}