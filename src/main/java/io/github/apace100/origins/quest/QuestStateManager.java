package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages quest UI state for players
 */
public class QuestStateManager {
    private static QuestStateManager instance;
    
    private final Map<UUID, QuestSelectionState> playerSelections = new HashMap<>();
    private final Map<UUID, Long> lastSyncTimes = new HashMap<>();
    
    private QuestStateManager() {}
    
    public static QuestStateManager getInstance() {
        if (instance == null) {
            instance = new QuestStateManager();
        }
        return instance;
    }
    
    /**
     * Gets the selection state for a player
     */
    public QuestSelectionState getSelectionState(UUID playerId) {
        return playerSelections.computeIfAbsent(playerId, k -> new QuestSelectionState());
    }
    
    /**
     * Updates the selection state for a player
     */
    public void updateSelectionState(UUID playerId, int index, Quest quest) {
        QuestSelectionState state = getSelectionState(playerId);
        state.selectQuest(index, quest);
        
        Origins.LOGGER.info("Updated selection state for player {}: index={}, quest={}", 
            playerId, index, quest != null ? quest.getId() : "null");
    }
    
    /**
     * Clears the selection state for a player
     */
    public void clearSelectionState(UUID playerId) {
        QuestSelectionState state = getSelectionState(playerId);
        state.clearSelection();
        
        Origins.LOGGER.info("Cleared selection state for player {}", playerId);
    }
    
    /**
     * Checks if a quest is selected by a player
     */
    public boolean isQuestSelected(UUID playerId, int index, Quest quest) {
        QuestSelectionState state = playerSelections.get(playerId);
        return state != null && state.isSelected(index, quest);
    }
    
    /**
     * Gets the last sync time for a player
     */
    public long getLastSyncTime(UUID playerId) {
        return lastSyncTimes.getOrDefault(playerId, 0L);
    }
    
    /**
     * Updates the last sync time for a player
     */
    public void updateSyncTime(UUID playerId) {
        lastSyncTimes.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Checks if a player needs state synchronization
     */
    public boolean needsSync(UUID playerId, long threshold) {
        long lastSync = getLastSyncTime(playerId);
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastSync) > threshold;
    }
    
    /**
     * Removes all state data for a player (cleanup on disconnect)
     */
    public void removePlayerState(UUID playerId) {
        playerSelections.remove(playerId);
        lastSyncTimes.remove(playerId);
        
        Origins.LOGGER.info("Removed state data for player {}", playerId);
    }
    
    /**
     * Gets the number of players with active state
     */
    public int getActivePlayerCount() {
        return playerSelections.size();
    }
    
    /**
     * Performs cleanup of old state data
     */
    public void cleanup(long maxAge) {
        long currentTime = System.currentTimeMillis();
        
        lastSyncTimes.entrySet().removeIf(entry -> {
            boolean shouldRemove = (currentTime - entry.getValue()) > maxAge;
            if (shouldRemove) {
                UUID playerId = entry.getKey();
                playerSelections.remove(playerId);
                Origins.LOGGER.info("Cleaned up old state data for player {}", playerId);
            }
            return shouldRemove;
        });
    }
}