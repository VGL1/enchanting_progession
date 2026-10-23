package com.vgl.disenchanting.cast;

import net.minecraft.resources.ResourceLocation;

public class CastInstance {
    public final CastType type;
    public final int totalTicks;
    public int remainingTicks;
    public final ResourceLocation enchantId;
    public final int level;
    public final int targetSlot;
    public final boolean confirmed;
    public final double startX;
    public final double startY;
    public final double startZ;

    public CastInstance(CastType type, int totalTicks, ResourceLocation enchantId,
                        int level, int targetSlot, boolean confirmed,
                        double startX, double startY, double startZ) {
        this.type = type;
        this.totalTicks = totalTicks;
        this.remainingTicks = totalTicks;
        this.enchantId = enchantId;
        this.level = level;
        this.targetSlot = targetSlot;
        this.confirmed = confirmed;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
    }
}