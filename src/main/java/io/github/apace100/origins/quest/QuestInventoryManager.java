package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер для управления билетами квестов в инвентаре игрока.
 * Обеспечивает поиск, добавление и удаление билетов квестов.
 */
public class QuestInventoryManager {
    private static QuestInventoryManager instance;
    
    // Максимальное количество активных квестов для игрока
    private static final int MAX_ACTIVE_QUESTS = 5;
    
    private QuestInventoryManager() {}
    
    public static QuestInventoryManager getInstance() {
        if (instance == null) {
            instance = new QuestInventoryManager();
        }
        return instance;
    }
    
    /**
     * Находит все билеты квестов в инвентаре игрока
     */
    public List<ItemStack> findQuestTickets(PlayerEntity player) {
        List<ItemStack> questTickets = new ArrayList<>();
        
        if (player == null) {
            return questTickets;
        }
        
        try {
            // Проверяем основной инвентарь
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (QuestTicketItem.isQuestTicket(stack)) {
                    questTickets.add(stack);
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при поиске билетов квестов в инвентаре: {}", e.getMessage());
        }
        
        return questTickets;
    }
    
    /**
     * Получает активный билет квеста по ID
     */
    public ItemStack getActiveQuestTicket(PlayerEntity player, String questId) {
        if (player == null || questId == null) {
            return ItemStack.EMPTY;
        }
        
        try {
            List<ItemStack> questTickets = findQuestTickets(player);
            
            for (ItemStack ticket : questTickets) {
                Quest quest = QuestItem.getQuestFromStack(ticket);
                if (quest != null && quest.getId().equals(questId)) {
                    return ticket;
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при получении активного билета квеста: {}", e.getMessage());
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Проверяет, достиг ли игрок лимита активных квестов
     */
    public boolean hasReachedQuestLimit(PlayerEntity player) {
        if (player == null) {
            return true;
        }
        
        try {
            List<ItemStack> questTickets = findQuestTickets(player);
            return questTickets.size() >= MAX_ACTIVE_QUESTS;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке лимита квестов: {}", e.getMessage());
            return true; // В случае ошибки считаем, что лимит достигнут
        }
    }
    
    /**
     * Добавляет билет квеста в инвентарь игрока
     */
    public boolean addQuestTicketToInventory(PlayerEntity player, Quest quest) {
        if (player == null || quest == null) {
            return false;
        }
        
        try {
            // Проверяем лимит квестов
            if (hasReachedQuestLimit(player)) {
                Origins.LOGGER.warn("Игрок {} достиг лимита активных квестов", player.getName().getString());
                return false;
            }
            
            // Проверяем, есть ли место в инвентаре
            if (player.getInventory().getEmptySlot() == -1) {
                Origins.LOGGER.warn("Инвентарь игрока {} полон", player.getName().getString());
                return false;
            }
            
            // Создаем билет квеста
            ItemStack questTicket = QuestTicketItem.createQuestTicket(quest);
            if (questTicket.isEmpty()) {
                Origins.LOGGER.error("Не удалось создать билет для квеста: {}", quest.getId());
                return false;
            }
            
            // Добавляем билет в инвентарь
            boolean added = player.getInventory().insertStack(questTicket);
            
            if (added) {
                Origins.LOGGER.info("Билет квеста {} добавлен в инвентарь игрока {}", 
                    quest.getId(), player.getName().getString());
                
                // Синхронизируем инвентарь с клиентом
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.playerScreenHandler.syncState();
                }
            }
            
            return added;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при добавлении билета квеста в инвентарь: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Удаляет билет квеста из инвентаря игрока
     */
    public void removeQuestTicketFromInventory(PlayerEntity player, String questId) {
        if (player == null || questId == null) {
            return;
        }
        
        try {
            // Ищем билет в инвентаре
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                
                if (QuestTicketItem.isQuestTicket(stack)) {
                    Quest quest = QuestItem.getQuestFromStack(stack);
                    if (quest != null && quest.getId().equals(questId)) {
                        // Удаляем билет
                        player.getInventory().setStack(i, ItemStack.EMPTY);
                        
                        Origins.LOGGER.info("Билет квеста {} удален из инвентаря игрока {}", 
                            questId, player.getName().getString());
                        
                        // Синхронизируем инвентарь с клиентом
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            serverPlayer.playerScreenHandler.syncState();
                        }
                        
                        return;
                    }
                }
            }
            
            Origins.LOGGER.warn("Билет квеста {} не найден в инвентаре игрока {}", 
                questId, player.getName().getString());
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при удалении билета квеста из инвентаря: {}", e.getMessage());
        }
    }
    
    /**
     * Получает количество активных квестов у игрока
     */
    public int getActiveQuestCount(PlayerEntity player) {
        if (player == null) {
            return 0;
        }
        
        try {
            return findQuestTickets(player).size();
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при подсчете активных квестов: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Проверяет, есть ли у игрока билет конкретного квеста
     */
    public boolean hasQuestTicket(PlayerEntity player, String questId) {
        if (player == null || questId == null) {
            return false;
        }
        
        try {
            ItemStack ticket = getActiveQuestTicket(player, questId);
            return !ticket.isEmpty();
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке наличия билета квеста: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Очищает все билеты квестов из инвентаря игрока (для отладки)
     */
    public void clearAllQuestTickets(PlayerEntity player) {
        if (player == null) {
            return;
        }
        
        try {
            List<ItemStack> questTickets = findQuestTickets(player);
            
            for (ItemStack ticket : questTickets) {
                Quest quest = QuestItem.getQuestFromStack(ticket);
                if (quest != null) {
                    removeQuestTicketFromInventory(player, quest.getId());
                }
            }
            
            Origins.LOGGER.info("Все билеты квестов удалены из инвентаря игрока {}", 
                player.getName().getString());
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при очистке билетов квестов: {}", e.getMessage());
        }
    }
    
    /**
     * Получает максимальное количество активных квестов
     */
    public int getMaxActiveQuests() {
        return MAX_ACTIVE_QUESTS;
    }
}