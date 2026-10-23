package com.vgl.disenchanting.item;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(DisenChantingMod.MODID);

    public static final DeferredItem<Item> ENCHANT_DUST =
            ITEMS.registerSimpleItem("enchant_dust");

    public static final DeferredItem<Item> ENCHANT_SHARD =
            ITEMS.registerSimpleItem("enchant_shard");

    public static final DeferredItem<Item> ENCHANTING_WAND =
            ITEMS.registerSimpleItem("enchanting_wand",
                    new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> DISENCHANTING_WAND =
            ITEMS.registerSimpleItem("disenchanting_wand",
                    new Item.Properties().stacksTo(1));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}