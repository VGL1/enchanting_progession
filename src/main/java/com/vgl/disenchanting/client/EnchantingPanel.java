package com.vgl.disenchanting.client;

import com.vgl.disenchanting.config.DisenChantingConfig;
import com.vgl.disenchanting.enchant.EnchantMaterial;
import com.vgl.disenchanting.enchant.EnchantmentEntry;
import com.vgl.disenchanting.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EnchantingPanel extends AbstractWidget {

    public static EnchantingPanel CURRENT;

    private static final int PANEL_BG_TOP     = 0xFF231510;
    private static final int PANEL_BG_BOTTOM  = 0xFF120A06;
    private static final int PANEL_BORDER_O   = 0xFF0A0604;
    private static final int PANEL_BORDER_I   = 0xFF8B6914;

    private static final int HEADER_BG_TOP    = 0xFF3A2210;
    private static final int HEADER_BG_BOTTOM = 0xFF1A0F08;
    private static final int HEADER_TEXT      = 0xFFFFD700;

    private static final int BAR_BG           = 0xFF0A0604;
    private static final int BAR_FILL         = 0xFF2E5FBF;
    private static final int BAR_BORDER       = 0xFF4A2810;
    private static final int BAR_TEXT         = 0xFFFFD700;

    private static final int LIST_BG          = 0xFF0A0604;
    private static final int LIST_BORDER      = 0xFF4A2810;
    private static final int LIST_INNER_HI    = 0x30FFE0A0;
    private static final int LIST_INNER_SH    = 0x80000000;
    private static final int CAT_BG           = 0xFF2A1808;
    private static final int CAT_HOVER        = 0xFF3A2210;
    private static final int CAT_TEXT         = 0xFFFF8000;
    private static final int CAT_BAR          = 0xFFFF8000;
    private static final int ENTRY_HOVER      = 0xFF3A2008;
    private static final int ENTRY_SELECTED   = 0xFF5A3008;
    private static final int ENTRY_SEL_BORDER = 0xFFD4A017;

    private static final int COLOR_LOCKED = 0xFFFF3030;
    private static final int COLOR_ORANGE = 0xFFFF8000;
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    private static final int COLOR_GREEN  = 0xFF40D040;
    private static final int COLOR_GRAY   = 0xFF808080;

    private static final int DETAILS_BG       = 0xFF0F0804;
    private static final int DETAILS_BORDER   = 0xFF4A2810;
    private static final int DETAILS_INNER_HI = 0x30FFE0A0;
    private static final int DETAILS_INNER_SH = 0x80000000;

    private static final int BTN_BG           = 0xFF2A1810;
    private static final int BTN_BG_HOVER     = 0xFF3A2210;
    private static final int BTN_BORDER       = 0xFF8B6914;
    private static final int BTN_TEXT         = 0xFFC9B896;

    private static final int ENCHANT_BG        = 0xFF8B1A1A;
    private static final int ENCHANT_BG_HOVER  = 0xFFB02525;
    private static final int ENCHANT_BORDER    = 0xFFFF4040;
    private static final int ENCHANT_TEXT      = 0xFFFFE0E0;

    private static final int EXIT_BG           = 0xFF5A1010;
    private static final int EXIT_BG_HOVER     = 0xFF802020;
    private static final int EXIT_BORDER       = 0xFFB02525;

    private static final int ARMED_BG          = 0xFF206020;
    private static final int ARMED_BG_HOVER    = 0xFF2A8030;
    private static final int ARMED_BORDER      = 0xFF60FF60;

    private static final int HEADER_H  = 14;
    private static final int SKILLBAR_H = 12;
    private static final int SEARCH_H  = 12;
    private static final int ROW_H     = 14;
    private static final int PAD       = 6;

    private static final int DETAIL_LINE_H = 11;
    private static final int DETAILS_MIN_H = 60;
    private static final int BUTTONS_H = 18;

    private static final int ICON_SLOT = 18;
    private static final int ICON_ROW_H = 22;

    private sealed interface Row permits HeaderRow, EntryRow {}
    private record HeaderRow(EnchantmentEntry.Category category) implements Row {}
    private record EntryRow(ResourceLocation id, EnchantmentEntry entry, int level) implements Row {}

    private record TextLine(FormattedCharSequence seq, int color) {}
    private record CostLine(ItemStack icon, Component name, int amount) {}

    private final List<Row> rows = new ArrayList<>();
    private final Map<EnchantmentEntry.Category,
            List<Map.Entry<ResourceLocation, EnchantmentEntry>>> grouped
            = new EnumMap<>(EnchantmentEntry.Category.class);

    private final EditBox searchBox;

    private int scroll = 0;
    private boolean armed = false;

    private int minusX, minusY, plusX, plusY;
    private int enchantX, enchantY, enchantW, enchantH;
    private int exitX, exitY, exitW, exitH;
    private boolean hoverMinus, hoverPlus, hoverEnchant, hoverExit;

    private int listTop, listHeight;

    // ---- Скроллбар: сохранённые при рендере координаты для drag ----
    private int sbX, sbY, sbW, sbH;
    private int sbMaxScroll = 0;
    private int sbTrackX, sbTrackY, sbTrackW, sbTrackH;
    /** Точка, за которую тащим ползунок (смещение от sbY). */
    private double scrollbarDragOffset = 0;
    private boolean draggingScrollbar = false;

    private int dustIconX, dustIconY, shardIconX, shardIconY;
    private boolean dustIconVisible, shardIconVisible;
    private ItemStack dustIconStack, shardIconStack;
    private int dustIconAmount, shardIconAmount;

    private final int[] dustCounts  = new int[3];
    private final int[] shardCounts = new int[3];

    public EnchantingPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("screen.disen_chanting.enchanting"));

        for (var e : ClientPoolCache.all().entrySet()) {
            grouped.computeIfAbsent(e.getValue().category(), k -> new ArrayList<>()).add(e);
        }
        for (var list : grouped.values()) {
            list.sort(Comparator.comparing(e -> e.getKey().toString()));
        }

        ResourceLocation saved = ClientPanelState.getSelectedId();
        if (saved != null && !ClientPoolCache.all().containsKey(saved)) {
            ClientPanelState.clearSelected();
        }

        Minecraft mc = Minecraft.getInstance();
        searchBox = new EditBox(mc.font,
                x + PAD, y + HEADER_H + SKILLBAR_H + 6,
                width - PAD * 2, SEARCH_H,
                Component.translatable("gui.disen_chanting.search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.translatable("gui.disen_chanting.search"));
        searchBox.setValue(ClientPanelState.getSearchQuery());
        searchBox.setBordered(true);
        searchBox.setResponder(q -> {
            ClientPanelState.setSearchQuery(q);
            rebuildRows();
            scroll = 0;
        });

        rebuildRows();
    }

    public EditBox getSearchBox() { return searchBox; }

    private EnchantmentEntry getSelectedEntry() {
        ResourceLocation id = ClientPanelState.getSelectedId();
        return id == null ? null : ClientPoolCache.all().get(id);
    }

    private int getSelectedLevel() { return ClientPanelState.getSelectedLevel(); }

    private int colorFor(int tier) {
        return switch (tier) {
            case 0 -> COLOR_LOCKED;
            case 1 -> COLOR_ORANGE;
            case 2 -> COLOR_YELLOW;
            case 3 -> COLOR_GREEN;
            default -> COLOR_GRAY;
        };
    }

    private int tierForLevel(EnchantmentEntry entry, int level) {
        if (!ClientSkillCache.isRequirementsEnabled()) return 2;
        int req = entry.requiredSkillForLevel(level);
        return EnchantmentEntry.colorTier(ClientSkillCache.getLevel(), req);
    }

    private boolean isVisibleAtLevel(EnchantmentEntry entry, int level) {
        if (!ClientSkillCache.isRequirementsEnabled()) return true;
        int tier = tierForLevel(entry, level);
        if (tier == 0) return DisenChantingConfig.isShowLockedEnchantments();
        return true;
    }

    private void rebuildRows() {
        rows.clear();
        String q = ClientPanelState.getSearchQuery();
        boolean searching = q != null && !q.isEmpty();
        String lower = searching ? q.toLowerCase() : "";

        for (var cat : EnchantmentEntry.Category.values()) {
            var list = grouped.get(cat);
            if (list == null || list.isEmpty()) continue;

            List<EntryRow> visible = new ArrayList<>();
            for (var e : list) {
                var entry = e.getValue();
                for (int lvl = 1; lvl <= entry.maxLevel(); lvl++) {
                    if (!isVisibleAtLevel(entry, lvl)) continue;
                    if (searching && !matches(e.getKey(), entry, lower)) continue;
                    visible.add(new EntryRow(e.getKey(), entry, lvl));
                }
            }
            if (visible.isEmpty()) continue;

            rows.add(new HeaderRow(cat));
            boolean expanded = searching || ClientPanelState.isExpanded(cat);
            if (!expanded) continue;
            rows.addAll(visible);
        }
    }

    private static boolean matches(ResourceLocation id, EnchantmentEntry entry, String lowerQuery) {
        String localized = EnchantDescriptions.name(id).getString();
        if (localized.toLowerCase().contains(lowerQuery)) return true;
        if (id.toString().toLowerCase().contains(lowerQuery)) return true;
        if (id.getPath().toLowerCase().contains(lowerQuery)) return true;
        return false;
    }

    public boolean isArmed() { return armed; }
    public ResourceLocation getArmedId() {
        EnchantmentEntry sel = getSelectedEntry();
        return sel != null ? sel.enchantment() : null;
    }
    public int getArmedLevel() { return getSelectedLevel(); }
    public void disarm() { armed = false; }

    private void refreshReagentCounts() {
        dustCounts[0] = dustCounts[1] = dustCounts[2] = 0;
        shardCounts[0] = shardCounts[1] = shardCounts[2] = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;

            if (s.is(ModItems.ENCHANT_DUST.get()))        dustCounts[0]  += s.getCount();
            else if (s.is(ModItems.SPECTRAL_DUST.get()))  dustCounts[1]  += s.getCount();
            else if (s.is(ModItems.VISION_DUST.get()))    dustCounts[2]  += s.getCount();
            else if (s.is(ModItems.ENCHANT_SHARD.get()))  shardCounts[0] += s.getCount();
            else if (s.is(ModItems.SPIRIT_SHARD.get()))   shardCounts[1] += s.getCount();
            else if (s.is(ModItems.RADIANT_SHARD.get()))  shardCounts[2] += s.getCount();
        }
    }

    private int possibleCount(EnchantmentEntry entry, int level) {
        var tier = entry.materialTier();
        int idx = tier.ordinal();
        int dustCost  = entry.baseDustFor(level);
        int shardCost = entry.baseShardsFor(level);

        int possible = Integer.MAX_VALUE;
        if (dustCost > 0)  possible = Math.min(possible, dustCounts[idx] / dustCost);
        if (shardCost > 0) possible = Math.min(possible, shardCounts[idx] / shardCost);
        if (possible == Integer.MAX_VALUE) possible = 0;
        return possible;
    }

    private List<TextLine> buildDetailTextLines(int width) {
        List<TextLine> out = new ArrayList<>();
        EnchantmentEntry selected = getSelectedEntry();
        if (selected == null) return out;

        int selectedLevel = getSelectedLevel();
        Minecraft mc = Minecraft.getInstance();

        addText(out, mc, EnchantDescriptions.name(selected.enchantment())
                        .copy().append(" " + romanNumeral(selectedLevel)),
                width, 0xFFFFD700);

        Component rarity = Component.translatable(
                "rarity.disen_chanting." + selected.rarity().name().toLowerCase());
        addText(out, mc, Component.translatable("gui.disen_chanting.rarity", rarity),
                width, 0xFFC9B896);

        Component category = Component.translatable(
                "category.disen_chanting." + selected.category().name().toLowerCase());
        addText(out, mc, Component.translatable("gui.disen_chanting.category", category),
                width, 0xFFC9B896);

        addText(out, mc, Component.translatable("gui.disen_chanting.level",
                        selectedLevel, selected.maxLevel()),
                width, 0xFFFFFFFF);

        if (ClientSkillCache.isRequirementsEnabled()) {
            int req = selected.requiredSkillForLevel(selectedLevel);
            boolean ok = ClientSkillCache.getLevel() >= req;
            addText(out, mc,
                    Component.translatable("gui.disen_chanting.requires_level", req),
                    width, ok ? 0xFF80FF80 : 0xFFFF4040);

            int tier = EnchantmentEntry.colorTier(ClientSkillCache.getLevel(), req);
            int gain = EnchantmentEntry.skillGainForTier(tier);
            addText(out, mc,
                    Component.translatable("gui.disen_chanting.skill_gain", gain),
                    width, colorFor(tier));
        }

        addText(out, mc, Component.translatable("gui.disen_chanting.penalty"),
                width, 0xFF808080);

        Component desc = EnchantDescriptions.get(selected.enchantment());
        if (desc != null) {
            addText(out, mc, desc, width, 0xFFE0D0A0);
        }

        return out;
    }

    private static String romanNumeral(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
            case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private static void addText(List<TextLine> out, Minecraft mc,
                                Component text, int width, int color) {
        for (FormattedCharSequence seq : mc.font.split(text, width)) {
            out.add(new TextLine(seq, color));
        }
    }

    private List<CostLine> buildCostLines() {
        List<CostLine> out = new ArrayList<>();
        EnchantmentEntry selected = getSelectedEntry();
        if (selected == null) return out;

        int selectedLevel = getSelectedLevel();
        var tier = selected.materialTier();
        int dustBase  = selected.baseDustFor(selectedLevel);
        int shardBase = selected.baseShardsFor(selectedLevel);

        out.add(new CostLine(new ItemStack(EnchantMaterial.dustFor(tier)),
                EnchantMaterial.dustName(tier), dustBase));
        if (shardBase > 0) {
            out.add(new CostLine(new ItemStack(EnchantMaterial.shardFor(tier)),
                    EnchantMaterial.shardName(tier), shardBase));
        }
        return out;
    }

    // ==================== SCROLLBAR HELPERS ====================

    /** Пересчитывает scroll на основе позиции мыши по Y. */
    private void updateScrollFromMouse(double my) {
        if (sbMaxScroll <= 0) return;
        int trackH = sbTrackH - sbH;
        if (trackH <= 0) return;

        double targetY = my - scrollbarDragOffset - sbTrackY;
        double ratio = targetY / trackH;
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        scroll = (int) Math.round(ratio * sbMaxScroll);
    }
        // ---- Публичные методы для drag-обработки через ScreenEvent ----

    public boolean isDraggingScrollbar() {
        return draggingScrollbar;
    }

    public void dragTo(double mouseY) {
        updateScrollFromMouse(mouseY);
    }

    public void stopDrag() {
        draggingScrollbar = false;
    }

    // ==================== RENDER ====================

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        Minecraft mc = Minecraft.getInstance();

        dustIconVisible = shardIconVisible = false;

        refreshReagentCounts();

        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, PANEL_BORDER_O);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PANEL_BORDER_I);
        int halfH = h / 2;
        g.fill(x, y, x + w, y + halfH, PANEL_BG_TOP);
        g.fill(x, y + halfH, x + w, y + h, PANEL_BG_BOTTOM);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x50000000);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x50000000);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x30FFE0A0);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x30FFE0A0);

        int hdrY = y + 3;
        g.fill(x + 3, hdrY, x + w - 3, hdrY + HEADER_H / 2, HEADER_BG_TOP);
        g.fill(x + 3, hdrY + HEADER_H / 2, x + w - 3, hdrY + HEADER_H, HEADER_BG_BOTTOM);
        g.fill(x + 3, hdrY, x + w - 3, hdrY + 1, 0x60000000);
        g.fill(x + 3, hdrY + HEADER_H - 1, x + w - 3, hdrY + HEADER_H, PANEL_BORDER_I);
        g.fill(x + 3, hdrY, x + 4, hdrY + HEADER_H, 0x60000000);
        g.fill(x + w - 4, hdrY, x + w - 3, hdrY + HEADER_H, PANEL_BORDER_I);

        Component title = Component.translatable("screen.disen_chanting.enchanting");
        g.drawString(mc.font, title, x + (w - mc.font.width(title)) / 2, hdrY + 3, HEADER_TEXT, false);

        int barY = hdrY + HEADER_H + 2;
        int barX = x + PAD;
        int barW = w - PAD * 2;

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + SKILLBAR_H + 1, BAR_BORDER);
        g.fill(barX, barY, barX + barW, barY + SKILLBAR_H, BAR_BG);

        int lvl = ClientSkillCache.getLevel();
        int maxLvl = ClientSkillCache.MAX_LEVEL;
        float progress = maxLvl <= 1 ? 0f : (lvl - 1) / (float) (maxLvl - 1);
        progress = Math.max(0f, Math.min(1f, progress));
        int fillW = (int) (barW * progress);
        g.fill(barX, barY, barX + fillW, barY + SKILLBAR_H, BAR_FILL);

        Component barText = Component.literal("Enchanting      " + lvl + "/" + maxLvl);
        g.drawString(mc.font, barText,
                barX + (barW - mc.font.width(barText)) / 2,
                barY + 2, BAR_TEXT, false);

        int listX = x + PAD;
        int listY = barY + SKILLBAR_H + 2 + SEARCH_H + 4;
        int listW = w - PAD * 2;

        List<TextLine> detailText = buildDetailTextLines(listW - 8);
        List<CostLine> costLines = buildCostLines();
        int costH = costLines.isEmpty() ? 0 : costLines.size() * ICON_ROW_H + 6;
        int textH = detailText.isEmpty() ? DETAILS_MIN_H : detailText.size() * DETAIL_LINE_H + 8;
        int detailsH = textH + costH + 6;

        listTop = listY;
        listHeight = h - (listY - y) - detailsH - BUTTONS_H - PAD * 3 - 4;
        if (listHeight < 30) listHeight = 30;

        g.fill(listX - 1, listY - 1, listX + listW + 1, listY + listHeight + 1, LIST_BORDER);
        g.fill(listX, listY, listX + listW, listY + listHeight, LIST_BG);
        g.fill(listX + 1, listY + 1, listX + listW - 1, listY + 2, LIST_INNER_SH);
        g.fill(listX + 1, listY + 1, listX + 2, listY + listHeight - 1, LIST_INNER_SH);
        g.fill(listX + 1, listY + listHeight - 2, listX + listW - 1, listY + listHeight - 1, LIST_INNER_HI);
        g.fill(listX + listW - 2, listY + 1, listX + listW - 1, listY + listHeight - 1, LIST_INNER_HI);

        int visible = listHeight / ROW_H;
        int maxScroll = Math.max(0, rows.size() - visible);
        if (scroll > maxScroll) scroll = maxScroll;

        ResourceLocation selectedId = ClientPanelState.getSelectedId();
        int selectedLevel = ClientPanelState.getSelectedLevel();

        g.enableScissor(listX, listY, listX + listW, listY + listHeight);
        if (rows.isEmpty()) {
            g.drawString(mc.font, Component.translatable("gui.disen_chanting.no_results"),
                    listX + 4, listY + 4, 0xFF808080, false);
        }
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= rows.size()) break;
            Row row = rows.get(idx);
            int rowY = listY + i * ROW_H;

            boolean rowHover = mouseX >= listX && mouseX < listX + listW
                    && mouseY >= rowY && mouseY < rowY + ROW_H;

            if (row instanceof HeaderRow hr) {
                int bg = rowHover ? CAT_HOVER : CAT_BG;
                g.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_H, bg);
                g.fill(listX + 1, rowY, listX + 3, rowY + ROW_H, CAT_BAR);

                boolean isExpanded = ClientPanelState.isExpanded(hr.category())
                        || !ClientPanelState.getSearchQuery().isEmpty();
                String marker = isExpanded ? "[-]" : "[+]";
                g.drawString(mc.font, marker, listX + 5, rowY + (ROW_H - 8) / 2, 0xFFFFD700, false);

                Component header = Component.translatable(
                        "category.disen_chanting." + hr.category().name().toLowerCase());
                g.drawString(mc.font, header, listX + 25, rowY + (ROW_H - 8) / 2, CAT_TEXT, false);

                var list = grouped.get(hr.category());
                if (list != null) {
                    String cnt = "(" + list.size() + ")";
                    g.drawString(mc.font, cnt,
                            listX + listW - mc.font.width(cnt) - 4,
                            rowY + (ROW_H - 8) / 2, 0xFF808080, false);
                }
            } else if (row instanceof EntryRow er) {
                boolean isSelected = selectedId != null
                        && selectedId.equals(er.id())
                        && selectedLevel == er.level();

                if (isSelected) {
                    g.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_H, ENTRY_SELECTED);
                    g.fill(listX + 1, rowY, listX + listW - 1, rowY + 1, ENTRY_SEL_BORDER);
                    g.fill(listX + 1, rowY + ROW_H - 1, listX + listW - 1, rowY + ROW_H, ENTRY_SEL_BORDER);
                } else if (rowHover) {
                    g.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_H, ENTRY_HOVER);
                }

                int tier = tierForLevel(er.entry(), er.level());
                int nameColor = colorFor(tier);

                Component label = EnchantDescriptions.name(er.id())
                        .copy().append(" " + romanNumeral(er.level()));
                g.drawString(mc.font, label, listX + 22, rowY + (ROW_H - 8) / 2, nameColor, false);

                String rightLabel;
                int rightColor;
                if (tier == 0) {
                    rightLabel = String.valueOf(er.entry().requiredSkillForLevel(er.level()));
                    rightColor = COLOR_LOCKED;
                } else {
                    int possible = possibleCount(er.entry(), er.level());
                    rightLabel = possible > 0 ? "[" + possible + "]" : "";
                    rightColor = nameColor;
                }
                if (!rightLabel.isEmpty()) {
                    g.drawString(mc.font, rightLabel,
                            listX + listW - mc.font.width(rightLabel) - 4,
                            rowY + (ROW_H - 8) / 2, rightColor, false);
                }
            }
        }
        g.disableScissor();

        // ---- Скроллбар ----
        if (maxScroll > 0) {
            int trackX = listX + listW - 8;
            int trackY = listY;
            int trackH = listHeight;
            int trackW = 8;

            int barW2 = 3;
            int barX2 = listX + listW - 3;
            int barH2 = Math.max(10, listHeight * visible / rows.size());
            int barY2 = listY + (listHeight - barH2) * scroll / maxScroll;

            // Фон дорожки
            g.fill(barX2, listY, barX2 + barW2, listY + listHeight, 0xFF0A0604);
            // Ползунок
            g.fill(barX2, barY2, barX2 + barW2, barY2 + barH2, 0xFF8B6914);
            // Подсветка ползунка при drag
            if (draggingScrollbar) {
                g.fill(barX2, barY2, barX2 + barW2, barY2 + 1, 0xFFFFD700);
                g.fill(barX2, barY2 + barH2 - 1, barX2 + barW2, barY2 + barH2, 0xFFFFD700);
            }

            // Сохраняем параметры для обработки клика
            this.sbX = barX2;
            this.sbY = barY2;
            this.sbW = barW2;
            this.sbH = barH2;
            this.sbMaxScroll = maxScroll;
            this.sbTrackX = trackX;
            this.sbTrackY = trackY;
            this.sbTrackW = trackW;
            this.sbTrackH = trackH;
        } else {
            this.sbMaxScroll = 0;
            this.sbH = 0;
            this.sbTrackH = 0;
        }

        // Детали
        int detY = listY + listHeight + 4;
        int detH = h - (detY - y) - BUTTONS_H - PAD - 6;
        g.fill(x + PAD - 1, detY - 1, x + w - PAD + 1, detY + detH + 1, DETAILS_BORDER);
        g.fill(x + PAD, detY, x + w - PAD, detY + detH, DETAILS_BG);
        g.fill(x + PAD + 1, detY + 1, x + w - PAD - 1, detY + 2, DETAILS_INNER_SH);
        g.fill(x + PAD + 1, detY + 1, x + PAD + 2, detY + detH - 1, DETAILS_INNER_SH);
        g.fill(x + PAD + 1, detY + detH - 2, x + w - PAD - 1, detY + detH - 1, DETAILS_INNER_HI);
        g.fill(x + w - PAD - 2, detY + 1, x + w - PAD - 1, detY + detH - 1, DETAILS_INNER_HI);

        int lineY = detY + 4;
        if (detailText.isEmpty()) {
            g.drawString(mc.font, Component.translatable("gui.disen_chanting.pick"),
                    x + PAD + 4, lineY, 0xFF808080, false);
            lineY += DETAIL_LINE_H;
        } else {
            g.enableScissor(x + PAD, detY, x + w - PAD, detY + detH);
            for (TextLine line : detailText) {
                g.drawString(mc.font, line.seq(), x + PAD + 4, lineY, line.color(), false);
                lineY += DETAIL_LINE_H;
            }
            g.disableScissor();
        }

        if (!costLines.isEmpty()) {
            lineY += 2;
            Component reagentsLabel = Component.translatable("gui.disen_chanting.reagents");
            g.drawString(mc.font, reagentsLabel, x + PAD + 4, lineY, 0xFFFFD700, false);
            lineY += 11;

            int costStartY = lineY;
            int costX = x + PAD + 4;

            for (int i = 0; i < costLines.size(); i++) {
                CostLine cost = costLines.get(i);
                int cy = costStartY + i * ICON_ROW_H;

                if (i == 0) { dustIconX = costX; dustIconY = cy; dustIconVisible = true;
                    dustIconStack = cost.icon(); dustIconAmount = cost.amount(); }
                else if (i == 1) { shardIconX = costX; shardIconY = cy; shardIconVisible = true;
                    shardIconStack = cost.icon(); shardIconAmount = cost.amount(); }

                g.fill(costX - 2, cy - 2, costX + ICON_SLOT + 2, cy + ICON_SLOT + 2, 0xFF000000);
                g.fill(costX - 1, cy - 1, costX + ICON_SLOT + 1, cy + ICON_SLOT + 1, 0xFF5A3818);
                g.fill(costX, cy, costX + ICON_SLOT, cy + ICON_SLOT, 0xFF0A0604);
                g.fill(costX, cy, costX + ICON_SLOT, cy + 1, 0xFF000000);
                g.fill(costX, cy, costX + 1, cy + ICON_SLOT, 0xFF000000);
                g.fill(costX + 1, cy + ICON_SLOT - 1, costX + ICON_SLOT, cy + ICON_SLOT, 0xFF4A2A10);
                g.fill(costX + ICON_SLOT - 1, cy + 1, costX + ICON_SLOT, cy + ICON_SLOT - 1, 0xFF4A2A10);

                g.renderItem(cost.icon(), costX + 1, cy + 1);

                g.pose().pushPose();
                g.pose().translate(0, 0, 300);
                String cnt = String.valueOf(cost.amount());
                int cntW = mc.font.width(cnt);
                int cntX = costX + ICON_SLOT - cntW - 1;
                int cntY = cy + ICON_SLOT - 8;
                g.drawString(mc.font, cnt, cntX + 1, cntY + 1, 0xFF000000, false);
                g.drawString(mc.font, cnt, cntX, cntY, 0xFFFFFFFF, false);
                g.pose().popPose();

                g.drawString(mc.font, cost.name(),
                        costX + ICON_SLOT + 6,
                        cy + (ICON_SLOT - 8) / 2,
                        0xFFFFFFFF, false);
            }
        }

        // Кнопки
        int btnY = y + h - PAD - BUTTONS_H;
        int btnH = BUTTONS_H;
        int totalW = x + w - PAD;

        boolean levelSelEnabled = DisenChantingConfig.isLevelSelectionEnabled();
        if (levelSelEnabled) {
            minusX = x + PAD; minusY = btnY;
            drawButton(g, mc, minusX, minusY, 18, btnH, Component.literal("−"),
                    hoverMinus, BTN_BG, BTN_BG_HOVER, BTN_BORDER, BTN_TEXT);
            plusX = minusX + 20; plusY = btnY;
            drawButton(g, mc, plusX, plusY, 18, btnH, Component.literal("+"),
                    hoverPlus, BTN_BG, BTN_BG_HOVER, BTN_BORDER, BTN_TEXT);
        } else {
            minusX = plusX = -1000;
            minusY = plusY = -1000;
        }

        exitW = 44;
        exitX = totalW - exitW; exitY = btnY; exitH = btnH;
        drawButton(g, mc, exitX, exitY, exitW, exitH,
                Component.translatable("button.disen_chanting.exit"),
                hoverExit, EXIT_BG, EXIT_BG_HOVER, EXIT_BORDER, ENCHANT_TEXT);

        enchantX = levelSelEnabled ? plusX + 20 : x + PAD;
        enchantY = btnY;
        enchantW = exitX - 4 - enchantX;
        enchantH = btnH;

        EnchantmentEntry sel = getSelectedEntry();
        int tier = sel == null ? 2 : tierForLevel(sel, selectedLevel);
        boolean canBySkill = sel != null && (tier > 0);

        int eBg = armed ? ARMED_BG : (canBySkill ? ENCHANT_BG : 0xFF3A1010);
        int eBgHover = armed ? ARMED_BG_HOVER : (canBySkill ? ENCHANT_BG_HOVER : 0xFF401818);
        int eBorder = armed ? ARMED_BORDER : (canBySkill ? ENCHANT_BORDER : 0xFF602020);
        Component lbl = Component.translatable(armed
                ? "gui.disen_chanting.click_item"
                : "button.disen_chanting.enchant");
        drawButton(g, mc, enchantX, enchantY, enchantW, enchantH, lbl,
                hoverEnchant && canBySkill, eBg, eBgHover, eBorder, ENCHANT_TEXT);

        if (armed) {
            Component hint = Component.translatable("gui.disen_chanting.lmb_hint");
            g.drawString(mc.font, hint,
                    x + (w - mc.font.width(hint)) / 2,
                    enchantY - 12, 0xFF80FF80, false);
        }

        if (dustIconVisible && inRect(mouseX, mouseY, dustIconX, dustIconY, ICON_SLOT, ICON_SLOT)) {
            renderCostTooltip(g, mc, mouseX, mouseY, dustIconStack, dustIconAmount, x, y, w, h);
        } else if (shardIconVisible && inRect(mouseX, mouseY, shardIconX, shardIconY, ICON_SLOT, ICON_SLOT)) {
            renderCostTooltip(g, mc, mouseX, mouseY, shardIconStack, shardIconAmount, x, y, w, h);
        }

        updateHover(mouseX, mouseY);
    }

    private void renderCostTooltip(GuiGraphics g, Minecraft mc, int mx, int my,
                                   ItemStack stack, int amount, int panelX, int panelY,
                                   int panelW, int panelH) {
        Component line = Component.translatable("gui.disen_chanting.tooltip_cost",
                stack.getHoverName(), amount);
        int tw = mc.font.width(line);
        int th = mc.font.lineHeight;
        int padding = 4;
        int boxW = tw + padding * 2;
        int boxH = th + padding * 2;

        int tx = mx + 10;
        int ty = my + 10;
        if (tx + boxW > panelX + panelW - 2) tx = panelX + panelW - 2 - boxW;
        if (ty + boxH > panelY + panelH - 2) ty = panelY + panelH - 2 - boxH;
        if (tx < panelX + 2) tx = panelX + 2;
        if (ty < panelY + 2) ty = panelY + 2;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(tx - 2, ty - 2, tx + boxW + 2, ty + boxH + 2, 0xFF000000);
        g.fill(tx - 1, ty - 1, tx + boxW + 1, ty + boxH + 1, 0xFF8B6914);
        g.fill(tx, ty, tx + boxW, ty + boxH, 0xF0100804);
        g.drawString(mc.font, line, tx + padding, ty + padding, 0xFFFFD700, false);
        g.pose().popPose();
    }

    private void drawButton(GuiGraphics g, Minecraft mc, int x, int y, int w, int h,
                            Component label, boolean hovered,
                            int bg, int bgHover, int border, int textColor) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PANEL_BORDER_O);
        g.fill(x, y, x + w, y + h, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hovered ? bgHover : bg);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
        g.drawCenteredString(mc.font, label, x + w / 2, y + (h - 8) / 2, textColor);
    }

    private void updateHover(int mx, int my) {
        hoverMinus   = inRect(mx, my, minusX, minusY, 18, BUTTONS_H);
        hoverPlus    = inRect(mx, my, plusX, plusY, 18, BUTTONS_H);
        hoverEnchant = inRect(mx, my, enchantX, enchantY, enchantW, enchantH);
        hoverExit    = inRect(mx, my, exitX, exitY, exitW, exitH);
    }

    private static boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ==================== INPUT ====================

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (searchBox.isMouseOver(mx, my)) return false;
        searchBox.setFocused(false);

        if (!isMouseOver(mx, my)) return false;
        if (button != 0) return true;

        // ---- Скроллбар: клик по дорожке / ползунку ----
        if (sbMaxScroll > 0 && inRect((int) mx, (int) my, sbTrackX, sbTrackY, sbTrackW, sbTrackH)) {
            boolean onThumb = inRect((int) mx, (int) my, sbX, sbY, sbW, sbH);
            if (onThumb) {
                scrollbarDragOffset = my - sbY;
            } else {
                scrollbarDragOffset = sbH / 2.0;
                updateScrollFromMouse(my);
            }
            draggingScrollbar = true;
            return true;
        }

        if (hoverExit) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) mc.screen.onClose();
            return true;
        }

        int listX = getX() + PAD;
        int listY = listTop;
        int listW = getWidth() - PAD * 2;

        if (inRect((int) mx, (int) my, listX, listY, listW, listHeight)) {
            int i = (int) ((my - listY) / ROW_H);
            int idx = scroll + i;
            if (idx >= 0 && idx < rows.size()) {
                Row row = rows.get(idx);
                if (row instanceof HeaderRow hr) {
                    if (ClientPanelState.getSearchQuery().isEmpty()) {
                        ClientPanelState.toggle(hr.category());
                        rebuildRows();
                        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - 1)));
                    }
                } else if (row instanceof EntryRow er) {
                    ClientPanelState.setSelected(er.id(), er.level());
                    armed = false;
                }
            }
            return true;
        }

        if (DisenChantingConfig.isLevelSelectionEnabled()) {
            if (hoverMinus) {
                int lvl = getSelectedLevel();
                if (lvl > 1) ClientPanelState.setSelectedLevel(lvl - 1);
                armed = false;
                return true;
            }
            if (hoverPlus) {
                EnchantmentEntry sel = getSelectedEntry();
                int lvl = getSelectedLevel();
                if (sel != null && lvl < sel.maxLevel()) ClientPanelState.setSelectedLevel(lvl + 1);
                armed = false;
                return true;
            }
        }

        if (hoverEnchant) {
            EnchantmentEntry sel = getSelectedEntry();
            if (sel != null) {
                int tier = tierForLevel(sel, getSelectedLevel());
                if (tier > 0) armed = true;
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollFromMouse(my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingScrollbar && button == 0) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (!isMouseOver(mx, my)) return false;
        int visible = listHeight / ROW_H;
        int maxScroll = Math.max(0, rows.size() - visible);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - scrollY));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}