package com.vgl.disenchanting.item;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DisenChantingMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DISEN_CHANTING =
            TABS.register("disen_chanting", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DisenChantingMod.MODID + ".disen_chanting"))
                    .icon(() -> new ItemStack(ModItems.ENCHANTING_WAND.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ENCHANT_DUST.get());
                        output.accept(ModItems.ENCHANT_SHARD.get());
                        output.accept(ModItems.ENCHANTING_WAND.get());
                        output.accept(ModItems.DISENCHANTING_WAND.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}