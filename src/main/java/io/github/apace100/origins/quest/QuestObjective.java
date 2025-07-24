package io.github.apace100.origins.quest;

import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Представляет цель квеста (что нужно сделать)
 */
public class QuestObjective {
    private final ObjectiveType type;
    private final String target;
    private final int amount;
    private int progress;
    private boolean completed;
    
    public QuestObjective(ObjectiveType type, String target, int amount) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.progress = 0;
        this.completed = false;
    }
    
    // Геттеры
    public ObjectiveType getType() { return type; }
    public String getTarget() { return target; }
    public int getAmount() { return amount; }
    public int getProgress() { return progress; }
    public boolean isCompleted() { return completed; }
    
    /**
     * Обновляет прогресс выполнения цели
     */
    public void updateProgress(int amount) {
        this.progress = Math.min(this.progress + amount, this.amount);
        this.completed = this.progress >= this.amount;
    }
    
    /**
     * Устанавливает прогресс напрямую
     */
    public void setProgress(int progress) {
        this.progress = Math.min(progress, this.amount);
        this.completed = this.progress >= this.amount;
    }
    
    /**
     * Сбрасывает прогресс
     */
    public void resetProgress() {
        this.progress = 0;
        this.completed = false;
    }
    
    /**
     * Получает процент выполнения (0.0 - 1.0)
     */
    public float getProgressPercentage() {
        return amount > 0 ? (float) progress / amount : 0.0f;
    }
    
    /**
     * Проверяет, подходит ли предмет для этой цели
     */
    public boolean matchesItem(ItemStack stack) {
        if (type != ObjectiveType.COLLECT && type != ObjectiveType.CRAFT) {
            return false;
        }
        
        Item targetItem = Registries.ITEM.get(new Identifier(target));
        return stack.getItem() == targetItem;
    }
    
    /**
     * Проверяет, подходит ли тип сущности для этой цели
     */
    public boolean matchesEntity(EntityType<?> entityType) {
        if (type != ObjectiveType.KILL) {
            return false;
        }
        
        return target.equals(Registries.ENTITY_TYPE.getId(entityType).toString());
    }
    
    /**
     * Получает отображаемое описание цели
     */
    public Text getDisplayText() {
        String translationKey = "quest.origins.objective." + type.name().toLowerCase();
        
        switch (type) {
            case COLLECT:
                Item collectItem = Registries.ITEM.get(new Identifier(target));
                return Text.translatable(translationKey, progress, amount, collectItem.getName());
                
            case KILL:
                EntityType<?> entityType = Registries.ENTITY_TYPE.get(new Identifier(target));
                return Text.translatable(translationKey, progress, amount, entityType.getName());
                
            case CRAFT:
                Item craftItem = Registries.ITEM.get(new Identifier(target));
                return Text.translatable(translationKey, progress, amount, craftItem.getName());
                
            default:
                return Text.literal(progress + "/" + amount);
        }
    }
    
    /**
     * Создает цель из JSON объекта
     */
    public static QuestObjective fromJson(JsonObject json) {
        ObjectiveType type = ObjectiveType.valueOf(json.get("type").getAsString().toUpperCase());
        String target = json.get("target").getAsString();
        int amount = json.get("amount").getAsInt();
        
        return new QuestObjective(type, target, amount);
    }
    
    /**
     * Типы целей квестов
     */
    public enum ObjectiveType {
        COLLECT("collect"),    // Собрать предметы
        KILL("kill"),         // Убить мобов
        CRAFT("craft");       // Скрафтить предметы
        
        private final String name;
        
        ObjectiveType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public Text getDisplayName() {
            return Text.translatable("quest.origins.objective.type." + name);
        }
    }
}