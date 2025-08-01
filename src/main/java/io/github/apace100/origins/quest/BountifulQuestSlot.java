package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Слот для квестов в стиле Bountiful
 */
public class BountifulQuestSlot extends Slot {
    
    public BountifulQuestSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
    
    @Override
    public boolean canTakeItems(PlayerEntity player) {
        ItemStack stack = getStack();
        
        // Проверяем, может ли игрок взять этот квест
        if (stack.getItem() instanceof BountifulQuestItem) {
            return BountifulQuestEventHandler.canPlayerTakeQuest(player, stack);
        }
        
        // Для старых квестов используем старую логику
        if (stack.getItem() instanceof QuestTicketItem) {
            // Упрощенная проверка для старых квестов
            return true;
        }
        
        return super.canTakeItems(player);
    }
    
    @Override
    public ItemStack takeStack(int amount) {
        ItemStack stack = super.takeStack(amount);
        
        // Если взяли квест Bountiful, устанавливаем время начала
        if (stack.getItem() instanceof BountifulQuestItem) {
            BountifulQuestInfo info = BountifulQuestInfo.get(stack);
            if (info != null && inventory != null) {
                // Создаем новую информацию с текущим временем
                BountifulQuestInfo newInfo = new BountifulQuestInfo(
                    info.getRarity(),
                    0L, // Время начала установим позже
                    info.timeLeft(null),
                    info.getProfession()
                );
                BountifulQuestInfo.set(stack, newInfo);
            }
        }
        
        return stack;
    }
    
    @Override
    public boolean canInsert(ItemStack stack) {
        // В слоты квестов нельзя вставлять предметы
        return false;
    }
    
    /**
     * Получает профессию игрока
     */
    private String getPlayerProfession(PlayerEntity player) {
        try {
            io.github.apace100.origins.component.OriginComponent originComponent = 
                io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            
            if (originComponent != null) {
                // Упрощенная версия - возвращаем "any"
                return "any";
            }
        } catch (Exception e) {
            // Если не удалось получить профессию, возвращаем "any"
        }
        
        return "any";
    }
}