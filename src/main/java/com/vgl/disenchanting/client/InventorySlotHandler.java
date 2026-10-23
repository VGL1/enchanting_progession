package com.vgl.disenchanting.client;

import com.vgl.disenchanting.network.C2SDisenchantSlot;
import com.vgl.disenchanting.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class InventorySlotHandler {

    private InventorySlotHandler() {}

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> cs)) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Slot slot = cs.getSlotUnderMouse();
        if (slot == null || slot.getItem().isEmpty()) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();

        if (!main.is(ModItems.DISENCHANTING_WAND.get())
                && !off.is(ModItems.DISENCHANTING_WAND.get())) return;

        int targetIndex = EnchantingPanelHandler.resolvePlayerInventoryIndex(player, slot);
        if (targetIndex < 0) return;

        event.setCanceled(true);
        PacketDistributor.sendToServer(new C2SDisenchantSlot(targetIndex));
    }
}