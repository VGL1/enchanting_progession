package com.vgl.disenchanting.network;

import com.vgl.disenchanting.DisenChantingMod;
import com.vgl.disenchanting.enchant.EnchantmentEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record S2CSyncPool(Map<ResourceLocation, EnchantmentEntry> pool) implements CustomPacketPayload {

    public static final Type<S2CSyncPool> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DisenChantingMod.MODID, "sync_pool"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncPool> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> {
                        buf.writeVarInt(p.pool.size());
                        p.pool.forEach((id, e) -> {
                            buf.writeResourceLocation(id);
                            buf.writeVarInt(e.maxLevel());
                            buf.writeUtf(e.rarity().name());
                            buf.writeVarInt(e.dustPerLevel());
                            buf.writeVarInt(e.shardCost());
                            buf.writeUtf(e.category().name());
                            int[] reqs = e.levelRequirements();
                            buf.writeVarInt(reqs.length);
                            for (int r : reqs) buf.writeVarInt(r);
                        });
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        Map<ResourceLocation, EnchantmentEntry> map = new HashMap<>(n);
                        for (int i = 0; i < n; i++) {
                            ResourceLocation id = buf.readResourceLocation();
                            int maxLevel = buf.readVarInt();
                            EnchantmentEntry.Rarity rarity = EnchantmentEntry.Rarity.byName(buf.readUtf());
                            int dust = buf.readVarInt();
                            int shards = buf.readVarInt();
                            EnchantmentEntry.Category category = EnchantmentEntry.Category.byName(buf.readUtf());
                            int reqLen = buf.readVarInt();
                            int[] reqs = new int[reqLen];
                            for (int j = 0; j < reqLen; j++) reqs[j] = buf.readVarInt();
                            map.put(id, new EnchantmentEntry(id, maxLevel, rarity, dust, shards, category, reqs));
                        }
                        return new S2CSyncPool(map);
                    });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}