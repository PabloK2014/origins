package io.github.apace100.origins.quest;

/**
 * Состояние выбора квеста в интерфейсе
 */
public class QuestSelectionState {
    private int selectedIndex = -1;
    private Quest selectedQuest = null;
    
    public QuestSelectionState() {
    }
    
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }
    
    public Quest getSelectedQuest() {
        return selectedQuest;
    }
    
    public void setSelectedQuest(Quest quest) {
        this.selectedQuest = quest;
    }
    
    public boolean hasSelection() {
        return selectedIndex >= 0 && selectedQuest != null;
    }
    
    public void clearSelection() {
        selectedIndex = -1;
        selectedQuest = null;
    }
    
    public boolean isSelected(int index, Quest quest) {
        return selectedIndex == index && selectedQuest == quest;
    }
    
    public void selectQuest(int index, Quest quest) {
        this.selectedIndex = index;
        this.selectedQuest = quest;
    }
}