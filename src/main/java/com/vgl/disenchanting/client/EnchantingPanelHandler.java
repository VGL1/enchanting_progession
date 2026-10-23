package com.vgl.disenchanting.client;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.enchant.EnchantMaterial;
import com.vgl.disenchanting.enchant.EnchantmentEntry;
import com.vgl.disenchanting.enchant.EnchantmentHelper;
import com.vgl.disenchanting.network.C2SRequestEnchant;
import com.vgl.disenchanting.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class EnchantingPanelHandler {

    private EnchantingPanelHandler() {}

    // ==================== INIT ====================

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> cs)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack main = mc.player.getMainHandItem();
        ItemStack off  = mc.player.getOffhandItem();
        boolean hasWand = main.is(ModItems.ENCHANTING_WAND.get())
                || off.is(ModItems.ENCHANTING_WAND.get());
        if (!hasWand) {
            EnchantingPanel.CURRENT = null;
            return;
        }

        int pw = 190;
        int desired = cs.getYSize() + 80;
        int ph = Math.max(360, desired);
        ph = Math.min(ph, mc.getWindow().getGuiScaledHeight() - 40);

        int py = cs.getGuiTop();
        int screenH = mc.getWindow().getGuiScaledHeight();
        if (py + ph > screenH - 10) py = Math.max(10, screenH - ph - 10);

        int px = cs.getGuiLeft() - pw - 4;
        if (px < 4) px = cs.getGuiLeft() + cs.getXSize() + 4;

        EnchantingPanel panel = new EnchantingPanel(px, py, pw, ph);
        EnchantingPanel.CURRENT = panel;
        event.addListener(panel);
        event.addListener(panel.getSearchBox());
    }

    // ==================== KEYBOARD ====================

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_ESCAPE) return;
        EnchantingPanel panel = EnchantingPanel.CURRENT;
        if (panel == null) return;
        if (panel.getSearchBox().isFocused()) {
            panel.getSearchBox().setFocused(false);
            event.setCanceled(true);
        }
    }

    // ==================== SCROLL (wheel) ====================

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        EnchantingPanel panel = EnchantingPanel.CURRENT;
        if (panel == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();

        if (panel.isMouseOver(mx, my)) {
            panel.mouseScrolled(mx, my, event.getScrollDeltaX(), event.getScrollDeltaY());
            event.setCanceled(true);
        }
    }

    // ==================== DRAG (scrollbar) ====================

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        EnchantingPanel panel = EnchantingPanel.CURRENT;
        if (panel == null) return;
        if (!panel.isDraggingScrollbar()) return;

        panel.dragTo(event.getMouseY());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        EnchantingPanel panel = EnchantingPanel.CURRENT;
        if (panel == null) return;
        if (event.getButton() == 0) {
            panel.stopDrag();
        }
    }

    // ==================== MOUSE CLICK ====================

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> cs)) return;
        EnchantingPanel panel = EnchantingPanel.CURRENT;
        if (panel == null) return;

        DisenChantingMod.LOGGER.info(
                "[Disen-Chanting DBG] onMousePressed: btn={} armed={} screen={}",
                event.getButton(), panel.isArmed(), cs.getClass().getSimpleName());

        if (event.getButton() == 0 && panel.isArmed()) {
            Slot slot = cs.getSlotUnderMouse();
            if (slot == null || slot.getItem().isEmpty()) {
                DisenChantingMod.LOGGER.info(
                        "[Disen-Chanting DBG] slot is null or empty, disarm");
                panel.disarm();
                return;
            }

            DisenChantingMod.LOGGER.info(
                    "[Disen-Chanting DBG] slotIdx={} containerSlot={} container={} stack={}",
                    slot.index, slot.getContainerSlot(),
                    slot.container.getClass().getSimpleName(), slot.getItem());

            ItemStack hovered = slot.getItem();
            if (hovered.is(ModItems.ENCHANTING_WAND.get())
                    || hovered.is(ModItems.DISENCHANTING_WAND.get())) {
                DisenChantingMod.LOGGER.info(
                        "[Disen-Chanting DBG] hovered is wand, disarm");
                panel.disarm();
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) {
                panel.disarm();
                return;
            }

            int targetIndex = resolvePlayerInventoryIndex(player, slot);
            DisenChantingMod.LOGGER.info(
                    "[Disen-Chanting DBG] resolvePlayerInventoryIndex => {}", targetIndex);

            if (targetIndex < 0) {
                DisenChantingMod.LOGGER.info(
                        "[Disen-Chanting DBG] cannot resolve index, disarm");
                panel.disarm();
                return;
            }

            ResourceLocation id = panel.getArmedId();
            if (id == null) {
                panel.disarm();
                return;
            }

            int level = panel.getArmedLevel();

            boolean sameEnchantSameLevel = hasSameEnchantAtLevel(hovered, id, level);
            boolean hasReagents = hasEnoughReagents(player, hovered, id, level);

            DisenChantingMod.LOGGER.info(
                    "[Disen-Chanting DBG] same={} reagents={} id={} lvl={} idx={}",
                    sameEnchantSameLevel, hasReagents, id, level, targetIndex);

            if (sameEnchantSameLevel && hasReagents) {
                DisenChantingMod.LOGGER.info(
                        "[Disen-Chanting DBG] opening ConfirmEnchantScreen");
                mc.setScreen(new ConfirmEnchantScreen(id, level, targetIndex, cs));
                panel.disarm();
                event.setCanceled(true);
                return;
            }

            DisenChantingMod.LOGGER.info(
                    "[Disen-Chanting DBG] sending C2SRequestEnchant");
            PacketDistributor.sendToServer(
                    new C2SRequestEnchant(id, level, targetIndex, false));
            panel.disarm();
            event.setCanceled(true);
            return;
        }

        if (event.getButton() == 1 && panel.isArmed()) {
            panel.disarm();
        }
    }

    // ==================== HELPERS ====================

    public static int resolvePlayerInventoryIndex(Player player, Slot slot) {
        if (slot.container instanceof Inventory inv && inv.player == player) {
            int idx = slot.getContainerSlot();
            if (idx >= 0 && idx < inv.getContainerSize()) {
                if (inv.getItem(idx) == slot.getItem()) {
                    return idx;
                }
            }
        }

        ItemStack hovered = slot.getItem();
        if (!hovered.isEmpty()) {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.getItem(i) == hovered) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static boolean hasSameEnchantAtLevel(ItemStack target, ResourceLocation enchantId, int level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        var reg = mc.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var opt = reg.getHolder(ResourceKey.create(Registries.ENCHANTMENT, enchantId));
        if (opt.isEmpty()) return false;
        Holder<Enchantment> holder = opt.get();

        ItemEnchantments current = target.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return current.getLevel(holder) == level;
    }

    private static boolean hasEnoughReagents(Player player, ItemStack target,
                                             ResourceLocation enchantId, int level) {
        EnchantmentEntry entry = ClientPoolCache.all().get(enchantId);
        if (entry == null) return false;

        ItemEnchantments current = target.getOrDefault(
                DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int existingCount = current.size();

        int dustCost  = entry.dustFor(level, existingCount);
        int shardCost = entry.shardsFor(level, existingCount);

        var tier = entry.materialTier();
        Item dustItem  = EnchantMaterial.dustFor(tier);
        Item shardItem = EnchantMaterial.shardFor(tier);

        int dustHave = 0;
        int shardHave = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(dustItem))  dustHave  += s.getCount();
            if (s.is(shardItem)) shardHave += s.getCount();
        }

        if (dustCost > 0 && dustHave < dustCost) return false;
        if (shardCost > 0 && shardHave < shardCost) return false;
        return true;
    }

    // ==================== RENDER ====================

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        EnchantingPanel panel = EnchantingPanel.CURRENT;
        if (panel == null || !panel.isArmed()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> cs)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Slot slot = cs.getSlotUnderMouse();
        int color = 0xFFFF6060;

        if (slot != null && !slot.getItem().isEmpty()) {
            var id = panel.getArmedId();
            if (id != null && EnchantmentHelper.canApply(
                    id, slot.getItem(), mc.level.registryAccess())) {
                color = 0xFF80FF80;
            }
        }

        GuiGraphics g = event.getGuiGraphics();
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();

        g.fill(mx - 8, my - 8, mx - 6, my + 8, color);
        g.fill(mx + 6, my - 8, mx + 8, my + 8, color);
        g.fill(mx - 8, my - 8, mx + 8, my - 6, color);
        g.fill(mx - 8, my + 6, mx + 8, my + 8, color);
    }
}