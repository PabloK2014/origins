package io.github.apace100.origins.quest;

import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Представляет квест для системы досок объявлений
 */
public class Quest {
    private final String id;
    private final String playerClass;
    private final int level;
    private final String title;
    private final String description;
    private QuestObjective objective;
    private final int timeLimit; // в минутах
    private QuestReward reward;
    
    public Quest(String id, String playerClass, int level, String title, String description,
                QuestObjective objective, int timeLimit, QuestReward reward) {
        this.id = id;
        this.playerClass = playerClass;
        this.level = level;
        this.title = title;
        this.description = description;
        this.objective = objective;
        this.timeLimit = timeLimit;
        this.reward = reward;
    }
    
    // Геттеры
    public String getId() { return id; }
    public String getPlayerClass() { return playerClass; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public QuestObjective getObjective() { return objective; }
    public int getTimeLimit() { return timeLimit; }
    public QuestReward getReward() { return reward; }
    
    // Методы для совместимости с интерфейсом (возвращают списки с одним элементом)
    public java.util.List<QuestObjective> getObjectives() { 
        return java.util.Collections.singletonList(objective); 
    }
    
    public java.util.List<QuestReward> getRewards() { 
        return java.util.Collections.singletonList(reward); 
    }
    
    public Text getDisplayName() {
        return Text.translatable("quest.origins." + id + ".title").formatted(getRarity().getColor());
    }
    
    public Text getDisplayDescription() {
        return Text.translatable("quest.origins." + id + ".description");
    }
    
    /**
     * Получает отформатированное название с учетом редкости
     */
    public Text getFormattedTitle() {
        return Text.literal(title).formatted(getRarity().getColor());
    }
    
    /**
     * Получает краткую информацию о квесте для отображения в списке
     */
    public Text getQuestInfo() {
        return Text.literal(String.format("[%s] %s (%d мин)", 
            getRarity().getDisplayName().getString(), 
            title, 
            timeLimit))
            .formatted(getRarity().getColor());
    }
    
    public QuestRarity getRarity() {
        return QuestRarity.fromLevel(level);
    }
    
    /**
     * Создает квест из JSON объекта
     */
    public static Quest fromJson(JsonObject json) {
        try {
            String id = json.get("id").getAsString();
            String playerClass = json.has("playerClass") ? json.get("playerClass").getAsString() : "any";
            int level = json.has("level") ? json.get("level").getAsInt() : 1;
            String title = json.has("title") ? json.get("title").getAsString() : "Безымянный квест";
            String description = json.has("description") ? json.get("description").getAsString() : "Описание отсутствует";
            int timeLimit = json.has("timeLimit") ? json.get("timeLimit").getAsInt() : 60; // 1 час по умолчанию
            
            QuestObjective objective = json.has("objective") ? 
                QuestObjective.fromJson(json.getAsJsonObject("objective")) : 
                new QuestObjective(QuestObjective.ObjectiveType.COLLECT, "minecraft:dirt", 1);
                
            QuestReward reward = json.has("reward") ? 
                QuestReward.fromJson(json.getAsJsonObject("reward")) : 
                new QuestReward(QuestReward.RewardType.EXPERIENCE, 1, 100);
            
            return new Quest(id, playerClass, level, title, description, objective, timeLimit, reward);
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при загрузке квеста из JSON: " + e.getMessage());
            // Возвращаем дефолтный квест в случае ошибки
            return createDefaultQuest();
        }
    }
    
    /**
     * Создает квест по умолчанию в случае ошибки загрузки
     */
    private static Quest createDefaultQuest() {
        QuestObjective defaultObjective = new QuestObjective(
            QuestObjective.ObjectiveType.COLLECT, 
            "minecraft:dirt", 
            1
        );
        QuestReward defaultReward = new QuestReward(
            QuestReward.RewardType.EXPERIENCE, 
            1, 
            100
        );
        
        return new Quest(
            "default_quest",
            "any",
            1,
            "Тестовый квест",
            "Соберите 1 блок земли",
            defaultObjective,
            60,
            defaultReward
        );
    }
    
    /**
     * Проверяет, может ли игрок взять этот квест
     */
    public boolean canPlayerTakeQuest(String playerClass) {
        return this.playerClass.equals(playerClass) || this.playerClass.equals("any");
    }
    
    /**
     * Проверяет, может ли игрок взять квест с учетом уровня
     */
    public boolean canPlayerTakeQuest(String playerClass, int playerLevel) {
        return canPlayerTakeQuest(playerClass) && playerLevel >= this.level;
    }
    
    /**
     * Проверяет, истек ли квест
     */
    public boolean isExpired(long startTime) {
        long currentTime = System.currentTimeMillis();
        long timeLimitMs = timeLimit * 60 * 1000L; // конвертируем минуты в миллисекунды
        return (currentTime - startTime) > timeLimitMs;
    }
    
    /**
     * Получает оставшееся время в минутах
     */
    public int getRemainingTime(long startTime) {
        long currentTime = System.currentTimeMillis();
        long timeLimitMs = timeLimit * 60 * 1000L;
        long remainingMs = timeLimitMs - (currentTime - startTime);
        return Math.max(0, (int)(remainingMs / (60 * 1000L)));
    }
    
    /**
     * Получает отформатированное время для отображения
     */
    public Text getFormattedTimeRemaining(long startTime) {
        int remainingMinutes = getRemainingTime(startTime);
        if (remainingMinutes <= 0) {
            return Text.literal("Истекло").formatted(Formatting.RED);
        } else if (remainingMinutes < 5) {
            return Text.literal(remainingMinutes + " мин").formatted(Formatting.YELLOW);
        } else {
            return Text.literal(remainingMinutes + " мин").formatted(Formatting.GREEN);
        }
    }
    
    /**
     * Добавляет или заменяет цель квеста
     */
    public void addObjective(QuestObjective newObjective) {
        this.objective = newObjective;
    }
    
    /**
     * Добавляет или заменяет награду квеста
     */
    public void addReward(QuestReward newReward) {
        this.reward = newReward;
    }
    
    /**
     * Создает копию квеста с сброшенным прогрессом
     */
    public Quest createFreshCopy() {
        QuestObjective freshObjective = new QuestObjective(
            objective.getType(),
            objective.getTarget(),
            objective.getAmount()
        );
        
        return new Quest(id, playerClass, level, title, description, 
                        freshObjective, timeLimit, reward);
    }
    
    /**
     * Проверяет валидность квеста
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() &&
               title != null && !title.isEmpty() &&
               objective != null &&
               reward != null &&
               timeLimit > 0 &&
               level > 0;
    }
    
    public enum QuestRarity {
        COMMON(1, "common", Formatting.WHITE),
        UNCOMMON(2, "uncommon", Formatting.GREEN), 
        RARE(3, "rare", Formatting.BLUE),
        EPIC(4, "epic", Formatting.LIGHT_PURPLE);
        
        private final int level;
        private final String name;
        private final Formatting color;
        
        QuestRarity(int level, String name, Formatting color) {
            this.level = level;
            this.name = name;
            this.color = color;
        }
        
        public int getLevel() { return level; }
        public String getName() { return name; }
        public Formatting getColor() { return color; }
        
        public static QuestRarity fromLevel(int level) {
            for (QuestRarity rarity : values()) {
                if (rarity.level == level) {
                    return rarity;
                }
            }
            return COMMON;
        }
        
        public Text getDisplayName() {
            return Text.translatable("quest.origins.rarity." + name).formatted(color);
        }
        
        /**
         * Получает редкость по названию
         */
        public static QuestRarity fromName(String name) {
            for (QuestRarity rarity : values()) {
                if (rarity.name.equalsIgnoreCase(name)) {
                    return rarity;
                }
            }
            return COMMON;
        }
    }
}