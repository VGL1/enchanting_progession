package com.vgl.disenchanting.client;

import net.minecraft.client.Minecraft;

public final class ClientScreenOpener {

    private ClientScreenOpener() {}

    /** Открыть окно зачарования, привязанное к конкретному слоту инвентаря. */
    public static void openEnchantingScreenForSlot(int slotIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new EnchantingScreen(slotIndex));
        }
    }

    /** Fallback: открыть без цели (не используется в текущей логике, оставлено на будущее). */
    public static void openEnchantingScreen() {
        openEnchantingScreenForSlot(-1);
    }
}