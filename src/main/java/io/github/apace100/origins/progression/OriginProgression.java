package io.github.apace100.origins.progression;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс для хранения прогрессии конкретного происхождения
 */
public class OriginProgression {
    private final String originId;
    private int experience;
    private int level;
    private final Map<String, Integer> statistics = new HashMap<>();
    
    public OriginProgression(String originId) {
        this.originId = originId;
        this.experience = 0;
        this.level = 1;
    }
    
    public String getOriginId() {
        return originId;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
        updateLevel();
    }
    
    public void addExperience(int amount) {
        if (amount <= 0) return;
        
        this.experience += amount;
        updateLevel();
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }
    
    /**
     * Получает количество опыта, необходимое для следующего уровня
     */
    public int getExperienceForNextLevel() {
        return getExperienceForLevel(level + 1);
    }
    
    /**
     * Получает количество опыта, необходимое для достижения определенного уровня
     */
    public int getExperienceForLevel(int targetLevel) {
        if (targetLevel <= 1) return 0;
        
        // Экспоненциальная формула: 100 * (уровень^1.5)
        return (int) (100 * Math.pow(targetLevel, 1.5));
    }
    
    /**
     * Получает прогресс до следующего уровня (0.0 - 1.0)
     */
    public double getProgressToNextLevel() {
        if (level >= getMaxLevel()) return 1.0;
        
        int currentLevelExp = getExperienceForLevel(level);
        int nextLevelExp = getExperienceForLevel(level + 1);
        
        if (nextLevelExp <= currentLevelExp) return 1.0;
        
        return (double) (experience - currentLevelExp) / (nextLevelExp - currentLevelExp);
    }
    
    /**
     * Получает максимальный уровень для данного происхождения
     */
    public int getMaxLevel() {
        return 50; // Можно настроить для каждого происхождения отдельно
    }
    
    /**
     * Обновляет уровень на основе текущего опыта
     */
    private void updateLevel() {
        int newLevel = 1;
        
        for (int i = 2; i <= getMaxLevel(); i++) {
            if (experience >= getExperienceForLevel(i)) {
                newLevel = i;
            } else {
                break;
            }
        }
        
        this.level = newLevel;
    }
    
    /**
     * Получает статистику по ключу
     */
    public int getStatistic(String key, int defaultValue) {
        return statistics.getOrDefault(key, defaultValue);
    }
    
    /**
     * Устанавливает статистику
     */
    public void setStatistic(String key, int value) {
        statistics.put(key, Math.max(0, value));
    }
    
    /**
     * Увеличивает статистику на указанное количество
     */
    public void incrementStatistic(String key, int amount) {
        if (amount <= 0) return;
        
        int currentValue = statistics.getOrDefault(key, 0);
        statistics.put(key, currentValue + amount);
    }
    
    /**
     * Получает все статистики
     */
    public Map<String, Integer> getAllStatistics() {
        return new HashMap<>(statistics);
    }
    
    /**
     * Очищает всю статистику
     */
    public void clearStatistics() {
        statistics.clear();
    }
    
    /**
     * Сбрасывает прогрессию к начальному состоянию
     */
    public void reset() {
        this.experience = 0;
        this.level = 1;
        this.statistics.clear();
    }
    
    /**
     * Копирует данные из другой прогрессии
     */
    public void copyFrom(OriginProgression other) {
        if (!this.originId.equals(other.originId)) {
            throw new IllegalArgumentException("Cannot copy progression from different origin type");
        }
        
        this.experience = other.experience;
        this.level = other.level;
        this.statistics.clear();
        this.statistics.putAll(other.statistics);
    }
    
    /**
     * Создает OriginProgression из NBT данных
     */
    public static OriginProgression fromNbt(String originId, net.minecraft.nbt.NbtCompound nbt) {
        OriginProgression progression = new OriginProgression(originId);
        progression.experience = nbt.getInt("experience");
        progression.level = nbt.getInt("level");
        
        // Загружаем статистику
        net.minecraft.nbt.NbtCompound statsNbt = nbt.getCompound("statistics");
        for (String key : statsNbt.getKeys()) {
            progression.statistics.put(key, statsNbt.getInt(key));
        }
        
        return progression;
    }
    
    /**
     * Сохраняет OriginProgression в NBT
     */
    public net.minecraft.nbt.NbtCompound writeToNbt() {
        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
        nbt.putInt("experience", experience);
        nbt.putInt("level", level);
        
        // Сохраняем статистику
        net.minecraft.nbt.NbtCompound statsNbt = new net.minecraft.nbt.NbtCompound();
        for (Map.Entry<String, Integer> entry : statistics.entrySet()) {
            statsNbt.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("statistics", statsNbt);
        
        return nbt;
    }
    
    @Override
    public String toString() {
        return "OriginProgression{" +
                "originId='" + originId + '\'' +
                ", experience=" + experience +
                ", level=" + level +
                ", statistics=" + statistics.size() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        OriginProgression that = (OriginProgression) obj;
        return originId.equals(that.originId);
    }
    
    @Override
    public int hashCode() {
        return originId.hashCode();
    }
}