package com.vgl.disenchanting.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EnchantingWandItem extends Item {

    public EnchantingWandItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Вся логика открытия панели живёт в EnchantingPanelHandler (клиент).
        // Здесь просто возвращаем успех, чтобы не мешать ванильному поведению.
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}