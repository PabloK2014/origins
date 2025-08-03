package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;

import java.util.List;

/**
 * Отслеживает прогресс выполнения квестов игроков.
 * Автоматически обновляет прогресс в билетах квестов при выполнении действий.
 */
public class QuestProgressTracker {
    private static QuestProgressTracker instance;
    
    private QuestProgressTracker() {}
    
    public static QuestProgressTracker getInstance() {
        if (instance == null) {
            instance = new QuestProgressTracker();
        }
        return instance;
    }
    
    /**
     * Отслеживает действие игрока и обновляет прогресс квестов
     */
    public void trackPlayerAction(PlayerEntity player, String action, String target, int amount) {
        Origins.LOGGER.info("Tracking action: player={}, action={}, target={}, amount={}", 
            player.getName().getString(), action, target, amount);
        
        try {
            // Получаем все билеты квестов в инвентаре игрока
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            List<ItemStack> questTickets = inventoryManager.findQuestTickets(player);
            
            Origins.LOGGER.info("Найдено билетов квестов: {}", questTickets.size());
            inventoryManager.debugInventory(player);
            
            if (questTickets.isEmpty()) {
                Origins.LOGGER.info("Нет активных квестов у игрока {}", player.getName().getString());
                return; // Нет активных квестов
            }
            
            // Обновляем прогресс для каждого билета
            for (ItemStack ticket : questTickets) {
                Origins.LOGGER.info("Обрабатываем билет квеста: {}", ticket);
                
                // Получаем квест из билета
                Quest quest = QuestItem.getQuestFromStack(ticket);
                if (quest != null) {
                    Origins.LOGGER.info("Найден квест в билете: {} (ID: {})", quest.getTitle(), quest.getId());
                    updateTicketProgress(player, ticket, action, target, amount);
                } else {
                    Origins.LOGGER.warn("Не удалось получить квест из билета: {}", ticket);
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при отслеживании действия игрока {}: {}", 
                player.getName().getString(), e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Обновляет прогресс в конкретном билете квеста
     */
    public void updateTicketProgress(PlayerEntity player, ItemStack ticketStack) {
        if (ticketStack.isEmpty() || !QuestTicketItem.isQuestTicket(ticketStack)) {
            return;
        }
        
        try {
            Quest quest = QuestItem.getQuestFromStack(ticketStack);
            if (quest == null) {
                return;
            }
            
            // Проверяем цель квеста (новая система с одной целью)
            QuestObjective objective = quest.getObjective();
            if (objective != null && !objective.isCompleted()) {
                boolean updated = checkObjectiveCompletion(objective, player);
                if (updated) {
                    QuestTicketItem.updateProgress(ticketStack, objective);
                    
                    // Синхронизируем с клиентом
                    syncProgressToClient(player);
                    
                    // Уведомляем игрока о прогрессе
                    notifyPlayerProgress(player, objective, quest);
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обновлении прогресса билета: {}", e.getMessage());
        }
    }
    
    /**
     * Обновляет прогресс в билете для конкретного действия
     */
    private void updateTicketProgress(PlayerEntity player, ItemStack ticket, String action, String target, int amount) {
        if (ticket.isEmpty() || !QuestTicketItem.isQuestTicket(ticket)) {
            return;
        }
        
        try {
            Quest quest = QuestItem.getQuestFromStack(ticket);
            if (quest == null) {
                Origins.LOGGER.error("Невалидный квест для билета: {}", ticket);
                return;
            }
            
            boolean progressUpdated = false;
            
            // Проверяем цель квеста (новая система с одной целью)
            QuestObjective objective = quest.getObjective();
            if (objective == null) {
                Origins.LOGGER.warn("Цель квеста не найдена: {}", quest.getId());
                return;
            }
            
            if (objective.isCompleted()) {
                return; // Цель уже выполнена
            }
            
            // Проверяем, соответствует ли действие цели
            Origins.LOGGER.info("Проверяем соответствие действия цели для квеста {}", quest.getId());
            if (isActionMatchingObjective(action, target, objective)) {
                Origins.LOGGER.info("Действие соответствует цели! Обновляем прогресс.");
                // Обновляем прогресс
                int newProgress = Math.min(objective.getProgress() + amount, objective.getAmount());
                objective.setProgress(newProgress);
                
                Origins.LOGGER.info("Обновлен прогресс квеста {}: {}/{}", quest.getId(), newProgress, objective.getAmount());
                
                if (newProgress >= objective.getAmount()) {
                    objective.setCompleted(true);
                    Origins.LOGGER.info("Цель квеста {} выполнена!", quest.getId());
                }
                
                // Обновляем билет с новым методом
                progressUpdated = QuestTicketItem.updateQuestProgress(ticket, action, target, amount);
                
                // Уведомляем игрока
                notifyPlayerProgress(player, objective, quest);
            } else {
                Origins.LOGGER.info("Действие НЕ соответствует цели квеста {}", quest.getId());
            }
            
            if (progressUpdated) {
                // Синхронизируем с клиентом
                syncProgressToClient(player);
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обновлении прогресса билета для действия {}: {}", action, e.getMessage());
        }
    }
    
    /**
     * Проверяет, соответствует ли действие цели квеста
     */
    private boolean isActionMatchingObjective(String action, String target, QuestObjective objective) {
        if (objective == null || action == null || target == null) {
            Origins.LOGGER.warn("Null параметры в isActionMatchingObjective: action={}, target={}, objective={}", action, target, objective);
            return false;
        }
        
        Origins.LOGGER.info("Проверяем соответствие: action={}, target={}, objectiveType={}, objectiveTarget={}", 
            action, target, objective.getType().getName(), objective.getTarget());
        
        // Проверяем тип действия
        String objectiveType = objective.getType().getName().toLowerCase();
        String actionLower = action.toLowerCase();
        
        boolean typeMatches;
        switch (objectiveType) {
            case "collect":
                typeMatches = actionLower.equals("collect") || actionLower.equals("pickup");
                break;
            case "kill":
                typeMatches = actionLower.equals("kill") || actionLower.equals("slay");
                break;
            case "craft":
                typeMatches = actionLower.equals("craft") || actionLower.equals("create");
                break;
            default:
                typeMatches = false;
                break;
        }
        
        if (!typeMatches) {
            Origins.LOGGER.info("Тип действия не совпадает: {} != {}", actionLower, objectiveType);
            return false;
        }
        
        Origins.LOGGER.info("Тип действия совпадает: {} == {}", actionLower, objectiveType);
        
        // Проверяем цель (предмет/моб)
        String objectiveTarget = objective.getTarget();
        if (objectiveTarget == null) {
            return false;
        }
        
        // Прямое сравнение ID
        if (objectiveTarget.equals(target)) {
            Origins.LOGGER.info("Точное совпадение цели: {} == {}", objectiveTarget, target);
            return true;
        }
        
        // Сравнение без префикса minecraft:
        String cleanObjective = objectiveTarget.replace("minecraft:", "");
        String cleanTarget = target.replace("minecraft:", "");
        
        if (cleanObjective.equals(cleanTarget)) {
            Origins.LOGGER.info("Совпадение без префикса: {} == {}", cleanObjective, cleanTarget);
            return true;
        }
        
        Origins.LOGGER.debug("Цели не совпадают: {} != {}", objectiveTarget, target);
        return false;
    }
    
    /**
     * Нормализует название цели для сравнения
     */
    private String normalizeTarget(String target) {
        if (target == null) {
            return "";
        }
        
        return target.toLowerCase()
                    .replace("minecraft:", "")
                    .replace("_", " ")
                    .trim();
    }
    
    /**
     * Проверяет выполнение цели квеста
     */
    public boolean checkObjectiveCompletion(QuestObjective objective, PlayerEntity player) {
        if (objective == null || player == null || objective.isCompleted()) {
            return false;
        }
        
        try {
            switch (objective.getType()) {
                case COLLECT:
                    return checkCollectObjective(objective, player);
                case KILL:
                    // Для убийства мобов используем отдельную систему отслеживания
                    return false; // Обновляется через события
                case CRAFT:
                    // Для крафта также используем события
                    return false; // Обновляется через события
                default:
                    return false;
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке выполнения цели: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Проверяет выполнение цели сбора предметов
     */
    private boolean checkCollectObjective(QuestObjective objective, PlayerEntity player) {
        try {
            String targetItem = objective.getTarget();
            int requiredAmount = objective.getAmount();
            
            // Подсчитываем количество предметов в инвентаре
            int currentAmount = 0;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                    if (itemId.contains(targetItem.replace("minecraft:", ""))) {
                        currentAmount += stack.getCount();
                    }
                }
            }
            
            // Обновляем прогресс
            int newProgress = Math.min(currentAmount, requiredAmount);
            if (newProgress != objective.getProgress()) {
                objective.setProgress(newProgress);
                
                if (newProgress >= requiredAmount) {
                    objective.setCompleted(true);
                }
                
                return true; // Прогресс обновлен
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке цели сбора: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Синхронизирует прогресс с клиентом
     */
    public void syncProgressToClient(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        try {
            // TODO: Реализовать синхронизацию через пакеты
            // Пока что просто обновляем инвентарь
            serverPlayer.playerScreenHandler.syncState();
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при синхронизации прогресса с клиентом: {}", e.getMessage());
        }
    }
    
    /**
     * Уведомляет игрока о прогрессе квеста
     */
    private void notifyPlayerProgress(PlayerEntity player, QuestObjective objective, Quest quest) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        try {
            if (objective.isCompleted()) {
                // Цель выполнена
                serverPlayer.sendMessage(
                    net.minecraft.text.Text.literal("✓ Цель выполнена: " + getObjectiveDescription(objective))
                        .formatted(net.minecraft.util.Formatting.GREEN),
                    true // Отображаем в action bar
                );
                
                // Проверяем, выполнен ли квест (новая система с одной целью)
                QuestObjective questObjective = quest.getObjective();
                if (questObjective != null && questObjective.isCompleted()) {
                    serverPlayer.sendMessage(
                        net.minecraft.text.Text.literal("🎉 Квест \"" + quest.getTitle() + "\" готов к сдаче!")
                            .formatted(net.minecraft.util.Formatting.GOLD),
                        false
                    );
                }
            } else {
                // Прогресс обновлен
                String progressText = objective.getProgress() + "/" + objective.getAmount();
                serverPlayer.sendMessage(
                    net.minecraft.text.Text.literal("📈 " + getObjectiveDescription(objective) + " (" + progressText + ")")
                        .formatted(net.minecraft.util.Formatting.YELLOW),
                    true // Отображаем в action bar
                );
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при уведомлении игрока о прогрессе: {}", e.getMessage());
        }
    }
    
    /**
     * Получает описание цели квеста
     */
    private String getObjectiveDescription(QuestObjective objective) {
        if (objective == null) {
            return "Неизвестная цель";
        }
        
        String action;
        switch (objective.getType()) {
            case COLLECT:
                action = "Собрать";
                break;
            case KILL:
                action = "Убить";
                break;
            case CRAFT:
                action = "Создать";
                break;
            default:
                action = "Выполнить";
                break;
        }
        
        String target = QuestUtils.getItemDisplayName(objective.getTarget());
        
        return action + " " + target;
    }
    

}