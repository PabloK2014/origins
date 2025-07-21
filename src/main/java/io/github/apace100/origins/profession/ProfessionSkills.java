package io.github.apace100.origins.profession;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ProfessionSkills {
    private final Map<String, Integer> skillLevels = new HashMap<>();
    public static final int MAX_SKILL_LEVEL = 10;

    public ProfessionSkills() {
        // Инициализируем базовые навыки
        skillLevels.put("health", 0);
        skillLevels.put("strength", 0);
        skillLevels.put("agility", 0);
        skillLevels.put("defense", 0);
    }

    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }

    public boolean canIncreaseSkill(String skillId) {
        return getSkillLevel(skillId) < MAX_SKILL_LEVEL;
    }

    public void increaseSkill(String skillId) {
        if (canIncreaseSkill(skillId)) {
            skillLevels.put(skillId, getSkillLevel(skillId) + 1);
        }
    }

    public List<Text> getSkillTooltip(String skillId) {
        List<Text> tooltip = new ArrayList<>();
        int level = getSkillLevel(skillId);
        
        switch (skillId) {
            case "health":
                tooltip.add(Text.translatable("skill.origins.health.desc"));
                tooltip.add(Text.translatable("skill.origins.health.current", level * 2));
                tooltip.add(Text.translatable("skill.origins.health.next", (level + 1) * 2));
                break;
            case "strength":
                tooltip.add(Text.translatable("skill.origins.strength.desc"));
                tooltip.add(Text.translatable("skill.origins.strength.current", level * 0.5f));
                tooltip.add(Text.translatable("skill.origins.strength.next", (level + 1) * 0.5f));
                break;
            case "agility":
                tooltip.add(Text.translatable("skill.origins.agility.desc"));
                tooltip.add(Text.translatable("skill.origins.agility.current", level * 2));
                tooltip.add(Text.translatable("skill.origins.agility.next", (level + 1) * 2));
                break;
            case "defense":
                tooltip.add(Text.translatable("skill.origins.defense.desc"));
                tooltip.add(Text.translatable("skill.origins.defense.current", level));
                tooltip.add(Text.translatable("skill.origins.defense.next", level + 1));
                break;
        }
        
        if (level >= MAX_SKILL_LEVEL) {
            tooltip.add(Text.translatable("skill.origins.max_level"));
        }
        
        return tooltip;
    }

    public void writeToNbt(NbtCompound tag) {
        NbtCompound skillsTag = new NbtCompound();
        skillLevels.forEach(skillsTag::putInt);
        tag.put("Skills", skillsTag);
    }

    public void readFromNbt(NbtCompound tag) {
        skillLevels.clear();
        NbtCompound skillsTag = tag.getCompound("Skills");
        for (String key : skillsTag.getKeys()) {
            skillLevels.put(key, skillsTag.getInt(key));
        }
    }

    public float getHealthBonus() {
        return getSkillLevel("health") * 2.0f; // +2 здоровья за уровень
    }

    public float getStrengthBonus() {
        return getSkillLevel("strength") * 0.5f; // +0.5 урона за уровень
    }

    public float getAgilityBonus() {
        return getSkillLevel("agility") * 0.02f; // +2% скорости за уровень
    }

    public float getDefenseBonus() {
        return getSkillLevel("defense") * 1.0f; // +1 броня за уровень
    }
} 