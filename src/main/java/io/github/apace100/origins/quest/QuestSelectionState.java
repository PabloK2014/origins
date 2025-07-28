package io.github.apace100.origins.quest;

/**
 * Manages quest selection state for the bounty board UI
 */
public class QuestSelectionState {
    private int selectedIndex = -1;
    private String selectedQuestId = null;
    private long selectionTimestamp = 0;
    
    /**
     * Selects a quest by index and quest object
     */
    public void selectQuest(int index, Quest quest) {
        this.selectedIndex = index;
        this.selectedQuestId = quest != null ? quest.getId() : null;
        this.selectionTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Checks if the given quest is currently selected
     */
    public boolean isSelected(int index, Quest quest) {
        return selectedIndex == index && 
               quest != null && 
               quest.getId().equals(selectedQuestId);
    }
    
    /**
     * Gets the currently selected quest index
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Gets the currently selected quest ID
     */
    public String getSelectedQuestId() {
        return selectedQuestId;
    }
    
    /**
     * Gets the timestamp when the selection was made
     */
    public long getSelectionTimestamp() {
        return selectionTimestamp;
    }
    
    /**
     * Clears the current selection
     */
    public void clearSelection() {
        this.selectedIndex = -1;
        this.selectedQuestId = null;
        this.selectionTimestamp = 0;
    }
    
    /**
     * Checks if any quest is currently selected
     */
    public boolean hasSelection() {
        return selectedIndex >= 0 && selectedQuestId != null;
    }
}