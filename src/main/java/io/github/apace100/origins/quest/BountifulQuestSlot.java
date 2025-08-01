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
        
        // Для билетов квестов проверяем совместимость класса
        if (stack.getItem() instanceof QuestTicketItem) {
            Quest quest = QuestItem.getQuestFromStack(stack);
            if (quest != null) {
                // Используем QuestTicketAcceptanceHandler для проверки
                QuestTicketAcceptanceHandler handler = QuestTicketAcceptanceHandler.getInstance();
                boolean canAccept = handler.canAcceptQuest(player, quest);
                
                if (!canAccept) {
                    // Отправляем сообщение игроку о том, почему нельзя взять квест
                    if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                        String playerClass = QuestIntegration.getPlayerClass(player);
                        String questClass = quest.getPlayerClass();
                        
                        if (!isClassCompatible(playerClass, questClass)) {
                            serverPlayer.sendMessage(
                                net.minecraft.text.Text.literal("❌ Этот квест предназначен для класса: " + QuestItem.getClassDisplayName(questClass))
                                    .formatted(net.minecraft.util.Formatting.RED), 
                                false
                            );
                        }
                    }
                }
                
                return canAccept;
            }
        }
        
        return super.canTakeItems(player);
    }
    
    /**
     * Проверяет совместимость классов
     */
    private boolean isClassCompatible(String playerClass, String questClass) {
        if (playerClass == null || questClass == null) {
            return false;
        }
        
        // Нормализуем названия классов
        String normalizedPlayerClass = normalizeClassName(playerClass);
        String normalizedQuestClass = normalizeClassName(questClass);
        
        return normalizedPlayerClass.equals(normalizedQuestClass);
    }
    
    /**
     * Нормализует название класса
     */
    private String normalizeClassName(String className) {
        if (className == null) {
            return "human";
        }
        
        // Убираем префикс "origins:" если есть
        if (className.startsWith("origins:")) {
            className = className.substring(8);
        }
        
        return className.toLowerCase();
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