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
    private int skillPoints = 0; // Новое поле для очков навыков
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
        updateLevelWithSkillPoints();
    }
    
    public void addExperience(int amount) {
        if (amount <= 0) return;
        int oldLevel = this.level;
        this.experience += amount;
        updateLevelWithSkillPoints();
        if (this.level > oldLevel) {
            skillPoints += (this.level - oldLevel); // +1 очко за каждый новый уровень
        }
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        int oldLevel = this.level;
        this.level = Math.max(1, level);
        if (this.level > oldLevel) {
            skillPoints += (this.level - oldLevel); // +1 очко за каждый новый уровень
        } else if (this.level < oldLevel) {
            // Если уровень понижен, можно уменьшить очки (по желанию)
            skillPoints = Math.max(0, skillPoints - (oldLevel - this.level));
        }
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
    private void updateLevelWithSkillPoints() {
        int newLevel = 1;
        for (int i = 2; i <= getMaxLevel(); i++) {
            if (experience >= getExperienceForLevel(i)) {
                newLevel = i;
            } else {
                break;
            }
        }
        if (newLevel > this.level) {
            skillPoints += (newLevel - this.level); // +1 очко за каждый новый уровень
        } else if (newLevel < this.level) {
            skillPoints = Math.max(0, skillPoints - (this.level - newLevel));
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
        progression.skillPoints = nbt.contains("skillPoints") ? nbt.getInt("skillPoints") : (progression.level - 1); // если нет, то по уровню
        
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
        nbt.putInt("skillPoints", skillPoints);
        
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

    // Методы для работы с очками навыков
    public int getSkillPoints() { return skillPoints; }
    public void spendSkillPoint() { if (skillPoints > 0) skillPoints--; }
    public void addSkillPoints(int amount) { skillPoints += amount; }
}