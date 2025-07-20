package io.github.apace100.origins.progression;

import net.minecraft.nbt.NbtCompound;

/**
 * Класс для хранения прогрессии происхождения игрока
 */
public class OriginProgression {
    
    private String originId;
    private int level;
    private int experience;
    private int totalExperience;
    
    // Константы для расчета уровней
    private static final int BASE_EXP_PER_LEVEL = 100;
    private static final float EXP_MULTIPLIER = 1.2f;
    
    public OriginProgression(String originId) {
        this.originId = originId;
        this.level = 1;
        this.experience = 0;
        this.totalExperience = 0;
    }
    
    /**
     * Добавляет опыт и проверяет повышение уровня
     */
    public boolean addExperience(int exp) {
        if (exp <= 0) return false;
        
        this.experience += exp;
        this.totalExperience += exp;
        
        boolean leveledUp = false;
        
        // Проверяем повышение уровня
        while (this.experience >= getExperienceForNextLevel()) {
            this.experience -= getExperienceForNextLevel();
            this.level++;
            leveledUp = true;
        }
        
        return leveledUp;
    }
    
    /**
     * Получить опыт, необходимый для следующего уровня
     */
    public int getExperienceForNextLevel() {
        return (int) (BASE_EXP_PER_LEVEL * Math.pow(EXP_MULTIPLIER, level - 1));
    }
    
    /**
     * Получить прогресс до следующего уровня (0.0 - 1.0)
     */
    public float getProgressToNextLevel() {
        int expForNext = getExperienceForNextLevel();
        return expForNext > 0 ? (float) experience / expForNext : 1.0f;
    }
    
    /**
     * Получить процент прогресса (0-100)
     */
    public int getProgressPercent() {
        return (int) (getProgressToNextLevel() * 100);
    }
    
    // Геттеры и сеттеры
    public String getOriginId() { return originId; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getTotalExperience() { return totalExperience; }
    
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    
    /**
     * Сохранение в NBT
     */
    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("originId", originId);
        nbt.putInt("level", level);
        nbt.putInt("experience", experience);
        nbt.putInt("totalExperience", totalExperience);
        return nbt;
    }
    
    /**
     * Загрузка из NBT
     */
    public void readFromNbt(NbtCompound nbt) {
        this.originId = nbt.getString("originId");
        this.level = Math.max(1, nbt.getInt("level"));
        this.experience = Math.max(0, nbt.getInt("experience"));
        this.totalExperience = Math.max(0, nbt.getInt("totalExperience"));
    }
    
    /**
     * Создание из NBT
     */
    public static OriginProgression fromNbt(NbtCompound nbt) {
        OriginProgression progression = new OriginProgression(nbt.getString("originId"));
        progression.readFromNbt(nbt);
        return progression;
    }
    
    @Override
    public String toString() {
        return String.format("OriginProgression{origin='%s', level=%d, exp=%d/%d (%d%%)}", 
            originId, level, experience, getExperienceForNextLevel(), getProgressPercent());
    }
}