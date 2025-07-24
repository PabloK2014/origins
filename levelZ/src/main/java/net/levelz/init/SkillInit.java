package net.levelz.init;

import net.levelz.stats.Skill;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.levelz.registry.SkillRegistry;

public class SkillInit {
    public static void init() {
        // Регистрируем скиллы
        for (Skill skill : Skill.values()) {
            Registry.register(SkillRegistry.SKILL, 
                new Identifier("levelz", skill.getNbt().toLowerCase()), 
                skill);
        }
    }
} 