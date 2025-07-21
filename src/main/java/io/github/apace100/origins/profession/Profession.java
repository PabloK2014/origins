package io.github.apace100.origins.profession;

import java.util.List;

/**
 * Класс, представляющий профессию игрока
 */
public class Profession {
    private final String id;
    private final String name;
    private final String description;
    private final String iconPath;
    private final List<String> experienceSources;
    private final int maxLevel;

    public Profession(String id, String name, String description, String iconPath, 
                     List<String> experienceSources, int maxLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconPath = iconPath;
        this.experienceSources = experienceSources;
        this.maxLevel = maxLevel;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconPath() {
        return iconPath;
    }

    public List<String> getExperienceSources() {
        return experienceSources;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Проверяет, дает ли данное действие опыт для этой профессии
     */
    public boolean givesExperience(String action) {
        return experienceSources.contains(action);
    }

    /**
     * Рассчитывает количество опыта, необходимое для достижения уровня
     */
    public int getExperienceForLevel(int level) {
        if (level <= 1) return 0;
        int exp = 100;
        for (int i = 2; i <= level; i++) {
            exp = (int)Math.round(exp * 1.2);
        }
        return exp;
    }

    /**
     * Рассчитывает уровень по количеству опыта
     */
    public int getLevelFromExperience(int experience) {
        if (experience <= 0) return 1;
        
        for (int level = 1; level <= maxLevel; level++) {
            if (getExperienceForLevel(level + 1) > experience) {
                return level;
            }
        }
        
        return maxLevel;
    }

    /**
     * Рассчитывает прогресс до следующего уровня (0.0 - 1.0)
     */
    public double getProgressToNextLevel(int experience) {
        int currentLevel = getLevelFromExperience(experience);
        if (currentLevel >= maxLevel) return 1.0;
        
        int currentLevelExp = getExperienceForLevel(currentLevel);
        int nextLevelExp = getExperienceForLevel(currentLevel + 1);
        
        if (nextLevelExp <= currentLevelExp) return 1.0;
        
        return (double) (experience - currentLevelExp) / (nextLevelExp - currentLevelExp);
    }
    
    /**
     * Статический метод для получения опыта для следующего уровня
     */
    public static int getExperienceForNextLevel(int currentLevel) {
        if (currentLevel < 1) return 100;
        int exp = 100;
        for (int i = 2; i <= currentLevel + 1; i++) {
            exp = (int)Math.round(exp * 1.2);
        }
        return exp;
    }

    @Override
    public String toString() {
        return "Profession{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", maxLevel=" + maxLevel +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Profession that = (Profession) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}