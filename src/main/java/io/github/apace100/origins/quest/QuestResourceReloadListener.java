package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/**
 * Обработчик перезагрузки ресурсов для квестов.
 * Загружает квесты из JSON файлов при старте сервера и перезагрузке ресурсов.
 */
public class QuestResourceReloadListener implements SimpleSynchronousResourceReloadListener {
    
    @Override
    public Identifier getFabricId() {
        return Origins.identifier("quests");
    }
    
    @Override
    public void reload(ResourceManager resourceManager) {
        Origins.LOGGER.info("Начинаем загрузку квестов из JSON файлов...");
        
        try {
            // Загружаем квесты из JSON файлов
            QuestGenerator.loadQuestsFromResources(resourceManager);
            Origins.LOGGER.info("Квесты успешно загружены из JSON файлов");
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при загрузке квестов: " + e.getMessage(), e);
        }
    }
}