package com.vgl.disenchanting.client;

import com.vgl.disenchanting.DisenChantingMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = DisenChantingMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "cast_bar"),
                new CastBarOverlay()
        );
    }

    public static void registerGameEvents() {
        NeoForge.EVENT_BUS.addListener(InventorySlotHandler::onMousePressed);
        NeoForge.EVENT_BUS.addListener(EnchantingPanelHandler::onScreenInit);

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                EnchantingPanelHandler::onMousePressed);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                EnchantingPanelHandler::onMouseScrolled);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                EnchantingPanelHandler::onMouseDragged);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                EnchantingPanelHandler::onMouseReleased);

        NeoForge.EVENT_BUS.addListener(EnchantingPanelHandler::onScreenRender);
        NeoForge.EVENT_BUS.addListener(EnchantingPanelHandler::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientCastState.tick();
    }
}