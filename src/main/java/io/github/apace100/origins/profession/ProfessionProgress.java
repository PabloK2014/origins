package io.github.apace100.origins.profession;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public class ProfessionProgress {
    private final Identifier professionId;
    private int level;
    private int experience;
    private int totalExperience;
    private int skillPoints = 0;
    private final ProfessionSkills skills;
    private final Map<String, Integer> statistics = new HashMap<>();

    public ProfessionProgress(Identifier professionId) {
        this.professionId = professionId;
        this.level = 1;
        this.experience = 0;
        this.totalExperience = 0;
        this.skills = new ProfessionSkills();
    }

    public boolean addExperience(int exp) {
        if (exp <= 0) return false;
        
        this.experience += exp;
        this.totalExperience += exp;
        
        boolean leveledUp = false;
        
        int expForNextLevel = getExperienceForNextLevel();
        while (this.experience >= expForNextLevel) {
            this.experience -= expForNextLevel;
            this.level++;
            this.skillPoints++; // +1 очко за каждый новый уровень
            leveledUp = true;
            expForNextLevel = getExperienceForNextLevel();
        }
        
        return leveledUp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        int oldLevel = this.level;
        this.level = Math.max(1, level);
        this.experience = 0;
        if (this.level > oldLevel) {
            this.skillPoints += (this.level - oldLevel);
        } else if (this.level < oldLevel) {
            this.skillPoints = Math.max(0, this.skillPoints - (oldLevel - this.level));
        }
    }

    public int getExperience() {
        return experience;
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public Identifier getProfessionId() {
        return professionId;
    }

    public int getExperienceForNextLevel() {
        return Profession.getExperienceForNextLevel(level);
    }

    public float getLevelProgress() {
        int expForNextLevel = getExperienceForNextLevel();
        if (expForNextLevel <= 0) return 1.0f;
        return (float) experience / expForNextLevel;
    }

    public double getProgressToNextLevel() {
        int expForNextLevel = getExperienceForNextLevel();
        if (expForNextLevel <= 0) return 1.0;
        return (double) experience / expForNextLevel;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void spendSkillPoint() {
        if (skillPoints > 0) skillPoints--;
    }

    public void addSkillPoints(int amount) {
        skillPoints += amount;
    }

    public ProfessionSkills getSkills() {
        return skills;
    }

    public int getStatistic(String key, int defaultValue) {
        return statistics.getOrDefault(key, defaultValue);
    }

    public void setStatistic(String key, int value) {
        statistics.put(key, value);
    }

    public void incrementStatistic(String key) {
        statistics.put(key, getStatistic(key, 0) + 1);
    }

    public void incrementStatistic(String key, int amount) {
        statistics.put(key, getStatistic(key, 0) + amount);
    }

    public void writeToNbt(NbtCompound tag) {
        tag.putInt("Level", level);
        tag.putInt("Experience", experience);
        tag.putInt("TotalExperience", totalExperience);
        tag.putInt("SkillPoints", skillPoints);
        
        // Сохраняем навыки
        NbtCompound skillsTag = new NbtCompound();
        skills.writeToNbt(skillsTag);
        tag.put("Skills", skillsTag);

        // Сохраняем статистику
        NbtCompound statsTag = new NbtCompound();
        statistics.forEach(statsTag::putInt);
        tag.put("Statistics", statsTag);
    }

    public void readFromNbt(NbtCompound tag) {
        this.level = tag.getInt("Level");
        this.experience = tag.getInt("Experience");
        this.totalExperience = tag.getInt("TotalExperience");
        this.skillPoints = tag.contains("SkillPoints") ? tag.getInt("SkillPoints") : (level - 1);
        
        // Загружаем навыки
        if (tag.contains("Skills")) {
            skills.readFromNbt(tag.getCompound("Skills"));
        }

        // Загружаем статистику
        statistics.clear();
        if (tag.contains("Statistics")) {
            NbtCompound statsTag = tag.getCompound("Statistics");
            for (String key : statsTag.getKeys()) {
                statistics.put(key, statsTag.getInt(key));
            }
        }
    }
}