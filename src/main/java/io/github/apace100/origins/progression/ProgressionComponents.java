package io.github.apace100.origins.progression;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Регистрация компонентов прогрессии
 */
public class ProgressionComponents implements EntityComponentInitializer {
    
    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Регистрируем компонент прогрессии для игроков
        registry.registerForPlayers(OriginProgressionComponent.KEY, 
            OriginProgressionComponent::new, 
            RespawnCopyStrategy.ALWAYS_COPY);
    }
}