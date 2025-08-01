package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Слот для декретов в правой части доски объявлений
 */
public class DecreeSlot extends Slot {
    
    public DecreeSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
    
    @Override
    public boolean canTakeItems(PlayerEntity player) {
        // Декреты могут брать все игроки
        return super.canTakeItems(player);
    }
    
    @Override
    public boolean canInsert(ItemStack stack) {
        // В слоты декретов можно помещать только определенные предметы
        // Пока что разрешаем все
        return super.canInsert(stack);
    }
}