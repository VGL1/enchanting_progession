package com.vgl.disenchanting.client;

import com.vgl.disenchanting.enchant.EnchantmentEntry;
import com.vgl.disenchanting.network.C2SRequestEnchant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EnchantingScreen extends Screen {

    private static final int PANEL_W = 140;
    private static final int BG_COLOR = 0xF0101018;
    private static final int BORDER_COLOR = 0xFF8060C0;
    private static final int SEL_COLOR = 0x803F60B0;

    private final int targetSlot;

    private EnchantList list;
    private EnchantmentEntry selected;
    private int selectedLevel = 1;

    private int leftX, rightX, top, bottom;

    public EnchantingScreen(int targetSlot) {
        super(Component.translatable("screen.disen_chanting.enchanting"));
        this.targetSlot = targetSlot;
    }

    @Override
    protected void init() {
        super.init();
        top = 40;
        bottom = this.height - 40;
        leftX = this.width / 2 - PANEL_W - 10;
        rightX = this.width / 2 + 10;

        list = new EnchantList(this.minecraft, PANEL_W, bottom - top, top, 24);
        addRenderableWidget(list);

        rebuildDetailWidgets();
    }

    private void rebuildDetailWidgets() {
        this.clearWidgets();
        addRenderableWidget(list);

        if (selected == null) return;

        int cx = rightX + PANEL_W / 2;
        int y = top + 60;

        addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        b -> {
                            if (selectedLevel > 1) {
                                selectedLevel--;
                                rebuildDetailWidgets();
                            }
                        })
                .bounds(cx - 40, y, 20, 20).build());

        addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        b -> {
                            if (selectedLevel < selected.maxLevel()) {
                                selectedLevel++;
                                rebuildDetailWidgets();
                            }
                        })
                .bounds(cx + 20, y, 20, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("button.disen_chanting.enchant"),
                        b -> doEnchant())
                .bounds(cx - 50, y + 40, 100, 20).build());
    }

    private void doEnchant() {
        if (selected == null) return;
        PacketDistributor.sendToServer(
                new C2SRequestEnchant(selected.enchantment(), selectedLevel, targetSlot, false));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        drawPanel(g, leftX, top, PANEL_W, bottom - top);
        drawPanel(g, rightX, top, PANEL_W, bottom - top);

        g.drawString(this.font, Component.translatable("screen.disen_chanting.list"),
                leftX + 6, top - 12, 0xFFFFFF, true);
        g.drawString(this.font, Component.translatable("screen.disen_chanting.details"),
                rightX + 6, top - 12, 0xFFFFFF, true);

        super.render(g, mouseX, mouseY, partialTick);

        if (selected != null) {
            int x = rightX + 8;
            int y = top + 8;
            g.drawString(this.font, selected.enchantment().toString(), x, y, 0xFFFF80, true);
            y += 14;
            g.drawString(this.font, Component.literal("Rarity: " + selected.rarity()),
                    x, y, 0xC0C0C0, true);
            y += 14;
            g.drawString(this.font, Component.literal("Level: " + selectedLevel
                    + " / " + selected.maxLevel()), x, y, 0xFFFFFF, true);
            y += 24;

            int dustCost  = selected.dustFor(selectedLevel);
            int shardCost = selected.shardsFor(selectedLevel);
            g.drawString(this.font, Component.literal("Dust: " + dustCost),
                    x, y, 0x80FF80, true);
            y += 12;
            if (shardCost > 0) {
                g.drawString(this.font, Component.literal("Shards: " + shardCost),
                        x, y, 0xFFAA00, true);
            }
        } else {
            g.drawString(this.font, Component.literal("Select an enchantment"),
                    rightX + 8, top + 8, 0xA0A0A0, true);
        }

        if (ClientPoolCache.isEmpty()) {
            g.drawString(this.font, Component.literal("Pool not synced from server"),
                    leftX + 8, top + 8, 0xFF6060, true);
        }
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDER_COLOR);
        g.fill(x, y, x + w, y + h, BG_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class EnchantList extends ObjectSelectionList<EnchantList.Entry> {

        public EnchantList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
            this.setX(leftX);
            List<Map.Entry<ResourceLocation, EnchantmentEntry>> sorted =
                    new ArrayList<>(ClientPoolCache.all().entrySet());
            sorted.sort(Comparator.comparing(e -> e.getKey().toString()));
            for (var e : sorted) {
                addEntry(new Entry(e.getKey(), e.getValue()));
            }
        }

        @Override
        public int getRowWidth() {
            return PANEL_W - 8;
        }

        @Override
        protected int getScrollbarPosition() {
            return leftX + PANEL_W - 6;
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            final ResourceLocation id;
            final EnchantmentEntry entry;

            Entry(ResourceLocation id, EnchantmentEntry entry) {
                this.id = id;
                this.entry = entry;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int w, int h,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                boolean sel = selected != null && selected.enchantment().equals(id);
                if (sel || hovered) {
                    g.fill(x, y, x + w, y + h, SEL_COLOR);
                }
                g.drawString(EnchantingScreen.this.font,
                        id.getPath(), x + 4, y + (h - 8) / 2, 0xFFFFFF, false);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                selected = entry;
                selectedLevel = 1;
                rebuildDetailWidgets();
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(id.toString());
            }
        }
    }
}