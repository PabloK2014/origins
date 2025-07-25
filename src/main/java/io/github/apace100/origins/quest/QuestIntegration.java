package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.origin.OriginLayers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс для интеграции системы квестов с системой классов Origins.
 * Обеспечивает получение квестов для конкретного класса игрока и проверку возможности взятия квестов.
 */
public class QuestIntegration {
    
    /**
     * Получает класс игрока на основе его происхождения в Origins
     */
    public static String getPlayerClass(PlayerEntity player) {
        try {
            var originComponent = ModComponents.ORIGIN.get(player);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(
                        OriginLayers.getLayer(Origins.identifier("origin")));
                if (origin != null) {
                    String originId = origin.getIdentifier().toString();
                    return switch (originId) {
                        case "origins:warrior" -> "warrior";
                        case "origins:miner" -> "miner";
                        case "origins:blacksmith" -> "blacksmith";
                        case "origins:courier" -> "courier";
                        case "origins:brewer" -> "brewer";
                        case "origins:cook" -> "cook";
                        default -> "human";
                    };
                }
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при получении класса игрока: " + e.getMessage());
        }
        return "human";
    }
    
    /**
     * Получает доступные квесты для конкретного класса игрока
     */
    public static List<Quest> getQuestsForPlayerClass(String playerClass, int count) {
        return QuestManager.getInstance().getRandomQuestsForClass(playerClass, count);
    }
    
    /**
     * Проверяет, может ли игрок взять конкретный квест
     */
    public static boolean canTakeQuest(PlayerEntity player, Quest quest) {
        if (quest == null) return false;
        
        String playerClass = getPlayerClass(player);
        
        // Проверяем, соответствует ли квест классу игрока
        if (!quest.getPlayerClass().equals(playerClass)) {
            return false;
        }
        
        // Проверяем, не взят ли уже этот квест
        if (hasActiveQuest(player, quest)) {
            return false;
        }
        
        // Проверяем уровень игрока (опционально)
        if (!hasRequiredLevel(player, quest)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Проверяет, есть ли у игрока активный квест
     */
    public static boolean hasActiveQuest(PlayerEntity player, Quest quest) {
        // Пока что простая проверка, можно расширить для системы активных квестов
        return false;
    }
    
    /**
     * Проверяет, достаточен ли уровень игрока для квеста
     */
    public static boolean hasRequiredLevel(PlayerEntity player, Quest quest) {
        // Пока что все квесты доступны всем уровням
        // Можно добавить проверку уровня игрока через PlayerSkillComponent
        return true;
    }
    
    /**
     * Получает отфильтрованный список квестов для игрока
     */
    public static List<Quest> getAvailableQuestsForPlayer(PlayerEntity player, List<Quest> allQuests) {
        String playerClass = getPlayerClass(player);
        
        return allQuests.stream()
                .filter(quest -> quest.getPlayerClass().equals(playerClass))
                .filter(quest -> canTakeQuest(player, quest))
                .collect(Collectors.toList());
    }
    
    /**
     * Проверяет, является ли игрок человеком (без класса)
     */
    public static boolean isHuman(PlayerEntity player) {
        return "human".equals(getPlayerClass(player));
    }
    
    /**
     * Получает локализованное название класса игрока
     */
    public static String getLocalizedClassName(String playerClass) {
        return switch (playerClass) {
            case "warrior" -> "Воин";
            case "miner" -> "Шахтер";
            case "blacksmith" -> "Кузнец";
            case "courier" -> "Курьер";
            case "brewer" -> "Пивовар";
            case "cook" -> "Повар";
            case "human" -> "Человек";
            default -> "Неизвестный";
        };
    }
    
    /**
     * Получает максимальное количество активных квестов для класса
     */
    public static int getMaxActiveQuests(String playerClass) {
        return switch (playerClass) {
            case "human" -> 0; // Люди не могут брать квесты
            default -> 1; // Пока что один активный квест на класс
        };
    }
    
    /**
     * Проверяет, может ли игрок взять еще один квест
     */
    public static boolean canTakeMoreQuests(PlayerEntity player) {
        String playerClass = getPlayerClass(player);
        int maxQuests = getMaxActiveQuests(playerClass);
        
        if (maxQuests == 0) return false;
        
        // Здесь должна быть проверка количества активных квестов
        // Пока что возвращаем true
        return true;
    }
}