package io.github.apace100.origins.quest;

/**
 * Enum representing different types of quest acceptance errors
 */
public enum QuestAcceptanceError {
    PROFESSION_MISMATCH("Player profession does not match quest requirements"),
    ALREADY_HAS_QUEST("Player already has an active quest"),
    QUEST_UNAVAILABLE("Quest is no longer available"),
    NETWORK_ERROR("Network communication failed"),
    INVENTORY_FULL("Player inventory is full"),
    QUEST_INVALID("Quest data is invalid or corrupted"),
    PLAYER_LEVEL_TOO_LOW("Player level is too low for this quest"),
    QUEST_LIMIT_REACHED("Maximum number of active quests reached"),
    UNKNOWN_ERROR("Unknown error occurred");
    
    private final String message;
    
    QuestAcceptanceError(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets a localized user-friendly message for the error
     */
    public String getLocalizedMessage() {
        return switch (this) {
            case PROFESSION_MISMATCH -> "Этот квест не подходит для вашей профессии";
            case ALREADY_HAS_QUEST -> "У вас уже есть активный квест";
            case QUEST_UNAVAILABLE -> "Квест больше недоступен";
            case NETWORK_ERROR -> "Ошибка сети, попробуйте еще раз";
            case INVENTORY_FULL -> "Нет места в инвентаре для билета квеста";
            case QUEST_INVALID -> "Данные квеста повреждены";
            case PLAYER_LEVEL_TOO_LOW -> "Недостаточный уровень для этого квеста";
            case QUEST_LIMIT_REACHED -> "Достигнут лимит активных квестов";
            case UNKNOWN_ERROR -> "Произошла неизвестная ошибка";
        };
    }
}