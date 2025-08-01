package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Слот для квестов в центральной части доски объявлений (сетка 3x7)
 */
public class BountySlot extends Slot {
    
    public BountySlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
    
    @Override
    public boolean canTakeItems(PlayerEntity player) {
        ItemStack stack = getStack();
        
        io.github.apace100.origins.Origins.LOGGER.info("BountySlot.canTakeItems вызван для игрока {} со стеком {}", 
            player.getName().getString(), stack.isEmpty() ? "пустой" : stack.getItem().getClass().getSimpleName());
        
        // Если слот пустой, разрешаем взять
        if (stack.isEmpty()) {
            return super.canTakeItems(player);
        }
        
        // Для билетов квестов проверяем совместимость класса
        if (stack.getItem() instanceof QuestTicketItem) {
            Quest quest = QuestItem.getQuestFromStack(stack);
            if (quest != null) {
                io.github.apace100.origins.Origins.LOGGER.info("Проверяем квест '{}' для игрока '{}'", quest.getTitle(), player.getName().getString());
                
                // Используем QuestTicketAcceptanceHandler для проверки
                QuestTicketAcceptanceHandler handler = QuestTicketAcceptanceHandler.getInstance();
                boolean canAccept = handler.canAcceptQuest(player, quest);
                
                io.github.apace100.origins.Origins.LOGGER.info("Результат проверки canAcceptQuest: {}", canAccept);
                
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
        
        // Для BountifulQuestItem используем существующую логику
        if (stack.getItem() instanceof BountifulQuestItem) {
            return BountifulQuestEventHandler.canPlayerTakeQuest(player, stack);
        }
        
        return super.canTakeItems(player);
    }
    
    @Override
    public boolean canInsert(ItemStack stack) {
        io.github.apace100.origins.Origins.LOGGER.info("BountySlot.canInsert вызван для стека {}", 
            stack.isEmpty() ? "пустой" : stack.getItem().getClass().getSimpleName());
        
        // Разрешаем вставку только билетов квестов и BountifulQuestItem
        if (stack.getItem() instanceof QuestTicketItem || stack.getItem() instanceof BountifulQuestItem) {
            return super.canInsert(stack);
        }
        
        return false;
    }
    
    @Override
    public ItemStack takeStack(int amount) {
        ItemStack stack = super.takeStack(amount);
        
        io.github.apace100.origins.Origins.LOGGER.info("BountySlot.takeStack вызван для стека {}", 
            stack.isEmpty() ? "пустой" : stack.getItem().getClass().getSimpleName());
        
        // Если взяли билет квеста, можем добавить дополнительную логику
        if (stack.getItem() instanceof QuestTicketItem) {
            // Логика принятия квеста уже обработана в canTakeItems
            // Здесь можем добавить дополнительные действия если нужно
        }
        
        return stack;
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
}