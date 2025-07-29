package io.github.apace100.origins.quest;

/**
 * Представляет активный квест игрока с информацией о времени начала и прогрессе.
 */
public class ActiveQuest {
    private final Quest quest;
    private final long startTime;
    private int progress;
    
    public ActiveQuest(Quest quest, long startTime) {
        this.quest = quest;
        this.startTime = startTime;
        this.progress = 0;
    }
    
    public Quest getQuest() {
        return quest;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    /**
     * Получает оставшееся время в минутах
     */
    public long getRemainingTimeMinutes() {
        if (quest.getTimeLimit() <= 0) {
            return -1; // Без ограничения времени
        }
        
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60);
        return Math.max(0, quest.getTimeLimit() - elapsedMinutes);
    }
    
    /**
     * Проверяет, истекло ли время квеста
     */
    public boolean isExpired() {
        if (quest.getTimeLimit() <= 0) {
            return false; // Без ограничения времени
        }
        
        return getRemainingTimeMinutes() <= 0;
    }
    
    /**
     * Получает прогресс в процентах (0-100)
     */
    public float getProgressPercentage() {
        if (quest.getObjectives().isEmpty()) {
            return 0.0f;
        }
        
        int totalObjectives = quest.getObjectives().size();
        int completedObjectives = 0;
        
        for (QuestObjective objective : quest.getObjectives()) {
            if (objective != null && objective.isCompleted()) {
                completedObjectives++;
            }
        }
        
        return (float) completedObjectives / totalObjectives * 100.0f;
    }
    
    /**
     * Проверяет, завершен ли квест
     */
    public boolean isCompleted() {
        return quest.getObjectives().stream()
            .filter(obj -> obj != null)
            .allMatch(QuestObjective::isCompleted);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ActiveQuest that = (ActiveQuest) obj;
        return quest.equals(that.quest) && startTime == that.startTime;
    }
    
    @Override
    public int hashCode() {
        return quest.hashCode() * 31 + Long.hashCode(startTime);
    }
    
    @Override
    public String toString() {
        return "ActiveQuest{" +
                "quest=" + quest.getTitle() +
                ", startTime=" + startTime +
                ", progress=" + progress +
                ", remainingTime=" + getRemainingTimeMinutes() + "min" +
                '}';
    }
}