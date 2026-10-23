package com.vgl.disenchanting.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DisenchantingWandItem extends Item {

    public DisenchantingWandItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Вся логика распыления живёт в InventorySlotHandler (клиент) + C2SDisenchantSlot.
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}