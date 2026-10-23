package com.vgl.disenchanting.client;

import com.vgl.disenchanting.network.C2SRequestEnchant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConfirmEnchantScreen extends Screen {

    private static final int BG_COLOR     = 0xF0100804;
    private static final int BORDER_COLOR = 0xFF8B6914;
    private static final int DIM_COLOR    = 0x80000000;

    private final ResourceLocation enchantId;
    private final int level;
    private final int targetSlot;
    private final Screen parent;

    private int panelX, panelY;
    private final int panelW = 240;
    private final int panelH = 90;

    public ConfirmEnchantScreen(ResourceLocation enchantId, int level, int targetSlot, Screen parent) {
        super(Component.translatable("screen.disen_chanting.confirm.title"));
        this.enchantId = enchantId;
        this.level = level;
        this.targetSlot = targetSlot;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int btnY = panelY + panelH - 26;
        int btnW = 100;

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.disen_chanting.confirm.yes"),
                        b -> {
                            PacketDistributor.sendToServer(
                                    new C2SRequestEnchant(enchantId, level, targetSlot, true));
                            Minecraft.getInstance().setScreen(parent);
                        })
                .bounds(panelX + 10, btnY, btnW, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.disen_chanting.confirm.no"),
                        b -> Minecraft.getInstance().setScreen(parent))
                .bounds(panelX + panelW - 10 - btnW, btnY, btnW, 20)
                .build());
    }

    // ---- Отключаем размытие 1.21 ----
    // 1.21 автоматически прогоняет фон через blur-шейдер через renderBlurredBackground().
    // Переопределяем оба метода в no-op, чтобы диалог оставался чётким.

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no-op — намеренно не вызываем super, чтобы не включить blur.
    }

    @Override
    public void renderBlurredBackground(float partialTick) {
        // no-op — отключает размытие фона в 1.21.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Затемнение вместо размытия
        g.fill(0, 0, this.width, this.height, DIM_COLOR);

        // Панель диалога с золотой рамкой
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, BORDER_COLOR);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);

        // Заголовок
        Component title = Component.translatable("screen.disen_chanting.confirm.title");
        g.drawString(this.font, title,
                panelX + (panelW - this.font.width(title)) / 2,
                panelY + 10, 0xFFFFD700, false);

        // Сообщение с именем чары и уровнем
        Component name = EnchantDescriptions.name(enchantId)
                .copy().append(" " + romanNumeral(level));
        Component msg = Component.translatable("screen.disen_chanting.confirm.message", name);
        int msgY = panelY + 34;
        for (var line : this.font.split(msg, panelW - 20)) {
            g.drawString(this.font, line,
                    panelX + (panelW - this.font.width(line)) / 2,
                    msgY, 0xFFFFFFFF, false);
            msgY += 11;
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static String romanNumeral(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
            case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}