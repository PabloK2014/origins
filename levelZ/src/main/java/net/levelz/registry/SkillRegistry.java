package net.levelz.registry;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.Lifecycle;
import net.minecraft.util.Identifier;
import net.levelz.stats.Skill;

public class SkillRegistry {
    public static final RegistryKey<Registry<Skill>> SKILL_KEY = RegistryKey.ofRegistry(new Identifier("levelz", "skill"));
    public static final Registry<Skill> SKILL = new SimpleRegistry<>(SKILL_KEY, Lifecycle.stable(), false);
} 