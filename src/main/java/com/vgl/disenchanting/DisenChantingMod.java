package com.vgl.disenchanting;

import com.mojang.logging.LogUtils;
import com.vgl.disenchanting.cast.CastManager;
import com.vgl.disenchanting.command.ModCommands;
import com.vgl.disenchanting.config.DisenChantingConfig;
import com.vgl.disenchanting.enchant.EnchantmentAutoLoader;
import com.vgl.disenchanting.enchant.EnchantmentPoolReloadListener;
import com.vgl.disenchanting.event.PlayerJoinHandler;
import com.vgl.disenchanting.network.ModNetwork;
import com.vgl.disenchanting.registry.ModAttachments;
import com.vgl.disenchanting.registry.ModCreativeTabs;
import com.vgl.disenchanting.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(DisenChantingMod.MODID)
public class DisenChantingMod {

    public static final String MODID = "disen_chanting";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DisenChantingMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetwork::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, DisenChantingConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(ModCommands::onRegister);
        NeoForge.EVENT_BUS.addListener(CastManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerJoinHandler::onJoin);
        NeoForge.EVENT_BUS.addListener(EnchantmentAutoLoader::onServerStarted);

        if (FMLEnvironment.dist.isClient()) {
            com.vgl.disenchanting.client.ClientEvents.registerGameEvents();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[Disen-Chanting] common setup done");
    }
    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new EnchantmentPoolReloadListener());
    }
}