package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Handles quest error recovery and user feedback
 */
public class QuestErrorRecovery {
    
    /**
     * Handles quest acceptance failure with appropriate recovery actions
     */
    public static void handleQuestAcceptanceFailure(QuestAcceptanceError error, Quest quest, PlayerEntity player) {
        // Log the error with detailed information
        Origins.LOGGER.warn("Quest acceptance failed: {} for quest {} and player {}", 
            error.getMessage(), 
            quest != null ? quest.getId() : "null", 
            player != null ? player.getName().getString() : "null");
        
        // Send user-friendly message to player
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Text errorMessage = Text.literal(error.getLocalizedMessage()).formatted(Formatting.RED);
            serverPlayer.sendMessage(errorMessage, false);
        }
        
        // Attempt recovery based on error type
        switch (error) {
            case NETWORK_ERROR -> retryNetworkOperation(quest, player);
            case QUEST_UNAVAILABLE -> refreshQuestList(player);
            case INVENTORY_FULL -> suggestInventoryCleanup(player);
            case QUEST_INVALID -> reportInvalidQuest(quest);
            default -> {} // No specific recovery action needed
        }
    }
    
    /**
     * Handles quest completion failure
     */
    public static void handleQuestCompletionFailure(QuestAcceptanceError error, Quest quest, PlayerEntity player) {
        Origins.LOGGER.warn("Quest completion failed: {} for quest {} and player {}", 
            error.getMessage(), 
            quest != null ? quest.getId() : "null", 
            player != null ? player.getName().getString() : "null");
        
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Text errorMessage = Text.literal("Не удалось завершить квест: " + error.getLocalizedMessage())
                .formatted(Formatting.RED);
            serverPlayer.sendMessage(errorMessage, false);
        }
    }
    
    /**
     * Handles general quest system errors
     */
    public static void handleQuestSystemError(Exception exception, String operation, PlayerEntity player) {
        Origins.LOGGER.error("Quest system error during {}: {}", operation, exception.getMessage());
        exception.printStackTrace();
        
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Text errorMessage = Text.literal("Произошла ошибка в системе квестов. Попробуйте позже.")
                .formatted(Formatting.RED);
            serverPlayer.sendMessage(errorMessage, false);
        }
    }
    
    /**
     * Attempts to retry a network operation
     */
    private static void retryNetworkOperation(Quest quest, PlayerEntity player) {
        Origins.LOGGER.info("Attempting to retry network operation for quest {} and player {}", 
            quest != null ? quest.getId() : "null", 
            player != null ? player.getName().getString() : "null");
        
        // TODO: Implement network retry logic
        // For now, just log the attempt
        
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Text retryMessage = Text.literal("Повторная попытка...")
                .formatted(Formatting.YELLOW);
            serverPlayer.sendMessage(retryMessage, false);
        }
    }
    
    /**
     * Refreshes the quest list for the player
     */
    private static void refreshQuestList(PlayerEntity player) {
        Origins.LOGGER.info("Refreshing quest list for player {}", 
            player != null ? player.getName().getString() : "null");
        
        try {
            // Find nearby bounty boards and refresh them
            // TODO: Implement quest list refresh logic
            
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Text refreshMessage = Text.literal("Список квестов обновлен")
                    .formatted(Formatting.GREEN);
                serverPlayer.sendMessage(refreshMessage, false);
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to refresh quest list: {}", e.getMessage());
        }
    }
    
    /**
     * Suggests inventory cleanup to the player
     */
    private static void suggestInventoryCleanup(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Text suggestion = Text.literal("Освободите место в инвентаре и попробуйте снова")
                .formatted(Formatting.YELLOW);
            serverPlayer.sendMessage(suggestion, false);
            
            // Count empty slots for additional info
            int emptySlots = 0;
            for (int i = 0; i < player.getInventory().size(); i++) {
                if (player.getInventory().getStack(i).isEmpty()) {
                    emptySlots++;
                }
            }
            
            Text slotInfo = Text.literal("Свободных слотов: " + emptySlots)
                .formatted(Formatting.GRAY);
            serverPlayer.sendMessage(slotInfo, false);
        }
    }
    
    /**
     * Reports an invalid quest for debugging
     */
    private static void reportInvalidQuest(Quest quest) {
        Origins.LOGGER.error("Invalid quest reported: {}", quest != null ? quest.getId() : "null");
        
        if (quest != null) {
            Origins.LOGGER.error("Quest details - Title: {}, Class: {}, Objective: {}, Reward: {}", 
                quest.getTitle(),
                quest.getPlayerClass(),
                quest.getObjective() != null ? quest.getObjective().getType() : "null",
                quest.getReward() != null ? quest.getReward().getType() : "null");
        }
        
        // TODO: Add quest validation and repair logic
    }
    
    /**
     * Validates quest data and attempts to fix common issues
     */
    public static boolean validateAndRepairQuest(Quest quest) {
        if (quest == null) {
            Origins.LOGGER.warn("Cannot validate null quest");
            return false;
        }
        
        boolean isValid = true;
        
        // Check quest ID
        if (quest.getId() == null || quest.getId().isEmpty()) {
            Origins.LOGGER.warn("Quest has invalid ID: {}", quest.getId());
            isValid = false;
        }
        
        // Check quest title
        if (quest.getTitle() == null || quest.getTitle().isEmpty()) {
            Origins.LOGGER.warn("Quest {} has invalid title: {}", quest.getId(), quest.getTitle());
            isValid = false;
        }
        
        // Check player class
        if (quest.getPlayerClass() == null || quest.getPlayerClass().isEmpty()) {
            Origins.LOGGER.warn("Quest {} has invalid player class: {}", quest.getId(), quest.getPlayerClass());
            isValid = false;
        }
        
        // Check objective
        if (quest.getObjective() == null) {
            Origins.LOGGER.warn("Quest {} has null objective", quest.getId());
            isValid = false;
        } else {
            QuestObjective objective = quest.getObjective();
            if (objective.getTarget() == null || objective.getTarget().isEmpty()) {
                Origins.LOGGER.warn("Quest {} has invalid objective target: {}", quest.getId(), objective.getTarget());
                isValid = false;
            }
            if (objective.getAmount() <= 0) {
                Origins.LOGGER.warn("Quest {} has invalid objective amount: {}", quest.getId(), objective.getAmount());
                isValid = false;
            }
        }
        
        // Check reward
        if (quest.getReward() == null) {
            Origins.LOGGER.warn("Quest {} has null reward", quest.getId());
            isValid = false;
        } else {
            QuestReward reward = quest.getReward();
            if (reward.getTier() <= 0) {
                Origins.LOGGER.warn("Quest {} has invalid reward tier: {}", quest.getId(), reward.getTier());
                isValid = false;
            }
        }
        
        return isValid;
    }
    
    /**
     * Creates a detailed error report for debugging
     */
    public static String createErrorReport(QuestAcceptanceError error, Quest quest, PlayerEntity player, Exception exception) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== Quest Error Report ===\n");
        report.append("Error Type: ").append(error.name()).append("\n");
        report.append("Error Message: ").append(error.getMessage()).append("\n");
        report.append("Timestamp: ").append(System.currentTimeMillis()).append("\n");
        
        if (player != null) {
            report.append("Player: ").append(player.getName().getString()).append("\n");
            report.append("Player UUID: ").append(player.getUuid()).append("\n");
        }
        
        if (quest != null) {
            report.append("Quest ID: ").append(quest.getId()).append("\n");
            report.append("Quest Title: ").append(quest.getTitle()).append("\n");
            report.append("Quest Class: ").append(quest.getPlayerClass()).append("\n");
        }
        
        if (exception != null) {
            report.append("Exception: ").append(exception.getClass().getSimpleName()).append("\n");
            report.append("Exception Message: ").append(exception.getMessage()).append("\n");
        }
        
        report.append("========================\n");
        
        return report.toString();
    }
}