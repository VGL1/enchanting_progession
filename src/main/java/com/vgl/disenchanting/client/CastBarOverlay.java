package com.vgl.disenchanting.client;

import com.vgl.disenchanting.cast.CastType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class CastBarOverlay implements LayeredDraw.Layer {

    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 12;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!ClientCastState.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        LocalPlayer player = mc.player;
        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        int x = (screenW - BAR_WIDTH) / 2;
        int y = screenH - 70;

        // ---- Прогресс (сглаженный) ----
        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        float remaining = ClientCastState.getSmoothedRemaining(partial);
        float progress = 1.0f - (remaining / (float) ClientCastState.getTotalTicks());
        progress = Math.max(0f, Math.min(1f, progress));

        // ---- Прозрачность при фейле ----
        int alpha = 0xFF;
        if (ClientCastState.isFailing()) {
            float fade = (float) ClientCastState.getFailTicks() / ClientCastState.FAIL_DURATION;
            alpha = (int) (255 * Math.max(0f, Math.min(1f, fade)));
        }

        // ---- Цвета ----
        int borderColor;
        int bgColor;
        int fillColor;
        if (ClientCastState.isFailing()) {
            borderColor = (alpha << 24) | 0xFF5050;
            bgColor     = (alpha << 24) | 0x300000;
            fillColor   = (alpha << 24) | 0xFF3030;
        } else {
            borderColor = (alpha << 24) | 0x8B6914;
            bgColor     = (alpha << 24) | 0x101018;
            fillColor   = (ClientCastState.getType() == CastType.ENCHANT)
                    ? (alpha << 24) | 0x55FF55
                    : (alpha << 24) | 0xFFAA00;
        }

        // ---- Рамка и фон ----
        guiGraphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, borderColor);
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, bgColor);

        // ---- Заполнение ----
        int fillW = (int) (BAR_WIDTH * progress);
        guiGraphics.fill(x, y, x + fillW, y + BAR_HEIGHT, fillColor);

        // ---- Подпись ----
        Component label;
        if (ClientCastState.isFailing()) {
            label = Component.translatable("gui.disen_chanting.cast_bar.interrupted");
        } else {
            label = Component.translatable(
                    ClientCastState.getType() == CastType.ENCHANT
                            ? "gui.disen_chanting.cast_bar.enchanting"
                            : "gui.disen_chanting.cast_bar.disenchanting");
        }

        int textW = mc.font.width(label);
        int textH = mc.font.lineHeight;
        int textX = x + (BAR_WIDTH - textW) / 2;
        int textY = y + (BAR_HEIGHT - textH) / 2;

        int textColor = (alpha << 24) | 0xFFFFFF;
        guiGraphics.drawString(mc.font, label, textX, textY, textColor, true);
    }
}