package io.github.apace100.origins.quest;

/**
 * Состояния билета квеста
 */
public enum QuestTicketState {
    /**
     * Квест доступен на доске объявлений
     */
    AVAILABLE("available", "Доступен"),
    
    /**
     * Квест принят игроком
     */
    ACCEPTED("accepted", "Принят"),
    
    /**
     * Квест в процессе выполнения
     */
    IN_PROGRESS("in_progress", "В процессе"),
    
    /**
     * Квест готов к сдаче
     */
    COMPLETED("completed", "Готов к сдаче"),
    
    /**
     * Квест завершен и награда получена
     */
    FINISHED("finished", "Завершен"),
    
    /**
     * Квест провален (время истекло или другие причины)
     */
    FAILED("failed", "Провален");
    
    private final String name;
    private final String displayName;
    
    QuestTicketState(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Получает состояние по названию
     */
    public static QuestTicketState fromName(String name) {
        if (name == null) {
            return AVAILABLE;
        }
        
        for (QuestTicketState state : values()) {
            if (state.name.equals(name)) {
                return state;
            }
        }
        
        return AVAILABLE;
    }
    
    /**
     * Проверяет, может ли билет быть использован для завершения квеста
     */
    public boolean canComplete() {
        return this == COMPLETED;
    }
    
    /**
     * Проверяет, активен ли квест
     */
    public boolean isActive() {
        return this == ACCEPTED || this == IN_PROGRESS || this == COMPLETED;
    }
}