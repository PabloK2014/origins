package io.github.apace100.origins.quest;

/**
 * Перечисление ошибок при принятии квестов
 */
public enum QuestAcceptanceError {
    ALREADY_HAS_QUEST("У вас уже есть этот квест"),
    PROFESSION_MISMATCH("Этот квест не подходит для вашего класса"),
    QUEST_LIMIT_REACHED("Достигнут лимит активных квестов"),
    INVENTORY_FULL("Недостаточно места в инвентаре"),
    PLAYER_LEVEL_TOO_LOW("Ваш уровень слишком низкий для этого квеста"),
    QUEST_INVALID("Квест поврежден или недействителен"),
    NETWORK_ERROR("Ошибка сети"),
    QUEST_UNAVAILABLE("Квест недоступен"),
    UNKNOWN_ERROR("Неизвестная ошибка");
    
    private final String message;
    
    QuestAcceptanceError(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getLocalizedMessage() {
        return message;
    }
}