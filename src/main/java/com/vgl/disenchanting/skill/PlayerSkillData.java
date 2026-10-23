package com.vgl.disenchanting.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerSkillData {

    public static final int MAX_LEVEL = 300;

    private int level = 1;

    public PlayerSkillData() {}

    public PlayerSkillData(int level) {
        this.level = clamp(level);
    }

    public int getLevel() { return level; }

    public void setLevel(int level) {
        this.level = clamp(level);
    }

    /** +1, если не достигнут максимум. Возвращает true, если уровень вырос. */
    public boolean increaseLevel() {
        if (level >= MAX_LEVEL) return false;
        level++;
        return true;
    }

    private static int clamp(int level) {
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }

    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Codec.INT.optionalFieldOf("level", 1).forGetter(PlayerSkillData::getLevel)
            ).apply(inst, PlayerSkillData::new));
}