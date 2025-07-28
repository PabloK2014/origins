package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

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
        if (player == null || player.getWorld().isClient) {
            return;
        }
        
        try {
            // Получаем все билеты квестов в инвентаре игрока
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            List<ItemStack> questTickets = inventoryManager.findQuestTickets(player);
            
            if (questTickets.isEmpty()) {
                return; // Нет активных квестов
            }
            
            // Обновляем прогресс для каждого билета
            for (ItemStack ticket : questTickets) {
                updateTicketProgress(player, ticket, action, target, amount);
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при отслеживании действия игрока {}: {}", 
                player.getName().getString(), e.getMessage());
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
            
            // Проверяем каждую цель квеста
            for (QuestObjective objective : quest.getObjectives()) {
                if (!objective.isCompleted()) {
                    boolean updated = checkObjectiveCompletion(objective, player);
                    if (updated) {
                        QuestTicketItem.updateProgress(ticketStack, objective);
                        
                        // Синхронизируем с клиентом
                        syncProgressToClient(player);
                        
                        // Уведомляем игрока о прогрессе
                        notifyPlayerProgress(player, objective, quest);
                    }
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
                return;
            }
            
            boolean progressUpdated = false;
            
            // Проверяем каждую цель квеста
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.isCompleted()) {
                    continue; // Цель уже выполнена
                }
                
                // Проверяем, соответствует ли действие цели
                if (isActionMatchingObjective(action, target, objective)) {
                    // Обновляем прогресс
                    int newProgress = Math.min(objective.getProgress() + amount, objective.getAmount());
                    objective.setProgress(newProgress);
                    
                    if (newProgress >= objective.getAmount()) {
                        objective.setCompleted(true);
                    }
                    
                    // Обновляем билет
                    QuestTicketItem.updateProgress(ticket, objective);
                    progressUpdated = true;
                    
                    // Уведомляем игрока
                    notifyPlayerProgress(player, objective, quest);
                }
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
        if (objective == null || action == null) {
            return false;
        }
        
        // Проверяем тип действия
        String objectiveType = objective.getType().getName().toLowerCase();
        String actionLower = action.toLowerCase();
        
        boolean typeMatches = switch (objectiveType) {
            case "collect" -> actionLower.equals("collect") || actionLower.equals("pickup");
            case "kill" -> actionLower.equals("kill") || actionLower.equals("slay");
            case "craft" -> actionLower.equals("craft") || actionLower.equals("create");
            case "mine" -> actionLower.equals("mine") || actionLower.equals("break");
            case "cook" -> actionLower.equals("cook") || actionLower.equals("smelt");
            default -> false;
        };
        
        if (!typeMatches) {
            return false;
        }
        
        // Проверяем цель (предмет/моб)
        String objectiveTarget = objective.getTarget();
        if (objectiveTarget == null || target == null) {
            return false;
        }
        
        // Нормализуем названия для сравнения
        String normalizedObjectiveTarget = normalizeTarget(objectiveTarget);
        String normalizedTarget = normalizeTarget(target);
        
        return normalizedObjectiveTarget.equals(normalizedTarget);
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
                    String itemId = stack.getItem().toString();
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
                
                // Проверяем, выполнен ли весь квест
                boolean allCompleted = quest.getObjectives().stream().allMatch(QuestObjective::isCompleted);
                if (allCompleted) {
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
        
        String action = switch (objective.getType()) {
            case COLLECT -> "Собрать";
            case KILL -> "Убить";
            case CRAFT -> "Создать";
            default -> "Выполнить";
        };
        
        String target = getItemDisplayName(objective.getTarget());
        
        return action + " " + target;
    }
    
    /**
     * Получает отображаемое название предмета
     */
    private String getItemDisplayName(String itemId) {
        if (itemId == null) {
            return "неизвестно";
        }
        
        // Убираем префикс minecraft:
        String cleanId = itemId.replace("minecraft:", "");
        
        // Заменяем подчеркивания на пробелы и делаем первую букву заглавной
        String[] parts = cleanId.split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase())
                      .append(parts[i].substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
}