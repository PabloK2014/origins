package io.github.apace100.origins.skill;

import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;

/**
 * Регистрация компонентов навыков
 */
public class SkillComponents implements EntityComponentInitializer {
    
    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Регистрируем компонент навыков для игроков
        registry.registerForPlayers(PlayerSkillComponent.KEY, 
            PlayerSkillComponent::new, 
            RespawnCopyStrategy.ALWAYS_COPY);
    }
}