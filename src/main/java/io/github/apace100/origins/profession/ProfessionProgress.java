package io.github.apace100.origins.profession;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * Класс для хранения прогресса профессии
 */
public class ProfessionProgress {
    private final Identifier professionId;
    private int level;
    private int experience;
    private int totalExperience;
    
    public ProfessionProgress(Identifier professionId) {
        this.professionId = professionId;
        this.level = 1;
        this.experience = 0;
        this.totalExperience = 0;
    }
    
    /**
     * Добавляет опыт и проверяет повышение уровня
     * @return true если уровень повысился
     */
    public boolean addExperience(int exp) {
        if (exp <= 0) return false;
        
        this.experience += exp;
        this.totalExperience += exp;
        
        boolean leveledUp = false;
        
        // Проверяем повышение уровня
        int expForNextLevel = getExperienceForNextLevel();
        while (this.experience >= expForNextLevel) {
            this.experience -= expForNextLevel;
            this.level++;
            leveledUp = true;
            expForNextLevel = getExperienceForNextLevel();
        }
        
        return leveledUp;
    }
    
    /**
     * Получает текущий уровень
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Устанавливает уровень
     */
    public void setLevel(int level) {
        this.level = Math.max(1, level);
        this.experience = 0; // Сбрасываем опыт на текущем уровне
        // totalExperience оставляем как есть
    }
    
    /**
     * Получает текущий опыт на этом уровне
     */
    public int getExperience() {
        return experience;
    }
    
    /**
     * Получает общий накопленный опыт
     */
    public int getTotalExperience() {
        return totalExperience;
    }
    
    /**
     * Получает идентификатор профессии
     */
    public Identifier getProfessionId() {
        return professionId;
    }
    
    /**
     * Получает опыт, необходимый для следующего уровня
     */
    public int getExperienceForNextLevel() {
        return Profession.getExperienceForNextLevel(level);
    }
    
    /**
     * Получает прогресс до следующего уровня (от 0.0 до 1.0)
     */
    public float getLevelProgress() {
        int expForNextLevel = getExperienceForNextLevel();
        if (expForNextLevel <= 0) return 1.0f;
        return (float) experience / expForNextLevel;
    }
    
    /**
     * Получает прогресс до следующего уровня (от 0.0 до 1.0) - double версия
     */
    public double getProgressToNextLevel() {
        int expForNextLevel = getExperienceForNextLevel();
        if (expForNextLevel <= 0) return 1.0;
        return (double) experience / expForNextLevel;
    }
    
    /**
     * Получает статистику по ключу
     */
    public int getStatistic(String key, int defaultValue) {
        // Пока что возвращаем значение по умолчанию
        // В будущем можно добавить Map для хранения статистики
        return defaultValue;
    }
    
    /**
     * Сохраняет прогресс в NBT
     */
    public void writeToNbt(NbtCompound tag) {
        tag.putInt("Level", level);
        tag.putInt("Experience", experience);
        tag.putInt("TotalExperience", totalExperience);
    }
    
    /**
     * Загружает прогресс из NBT
     */
    public void readFromNbt(NbtCompound tag) {
        this.level = tag.getInt("Level");
        this.experience = tag.getInt("Experience");
        this.totalExperience = tag.getInt("TotalExperience");
    }
}