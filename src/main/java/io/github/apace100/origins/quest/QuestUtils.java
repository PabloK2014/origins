package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;

/**
 * Утилиты для работы с квестами
 */
public class QuestUtils {
    
    /**
     * Проверяет совместимость классов игрока и квеста
     */
    public static boolean isClassCompatible(String playerClass, String questClass) {
        Origins.LOGGER.info("QuestUtils.isClassCompatible: игрок='{}', квест='{}'", playerClass, questClass);
        
        // Квесты для любого класса всегда доступны
        if (questClass == null || questClass.equals("any") || questClass.equals("human")) {
            Origins.LOGGER.info("Квест для любого класса или человека - РАЗРЕШЕНО");
            return true;
        }
        
        // Нормализуем названия классов
        String normalizedPlayerClass = normalizeClassName(playerClass);
        String normalizedQuestClass = normalizeClassName(questClass);
        
        Origins.LOGGER.info("Нормализованные классы: игрок='{}', квест='{}'", normalizedPlayerClass, normalizedQuestClass);
        
        // Проверяем точное совпадение
        boolean compatible = normalizedPlayerClass.equals(normalizedQuestClass);
        
        // Дополнительная проверка: если игрок "human", он может брать квесты для "human"
        if (!compatible && normalizedPlayerClass.equals("human") && normalizedQuestClass.equals("human")) {
            compatible = true;
        }
        
        Origins.LOGGER.info("Результат проверки совместимости: {}", compatible ? "РАЗРЕШЕНО" : "ЗАПРЕЩЕНО");
        return compatible;
    }
    
    /**
     * Нормализует название класса, убирая префиксы
     */
    public static String normalizeClassName(String className) {
        if (className == null) {
            return "human";
        }
        
        // Убираем префикс "origins:" если есть
        if (className.startsWith("origins:")) {
            className = className.substring(8);
        }
        
        // Приводим к нижнему регистру для единообразия
        return className.toLowerCase();
    }
    
    /**
     * Получает локализованное название класса
     */
    public static String getLocalizedClassName(String playerClass) {
        return switch (normalizeClassName(playerClass)) {
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
     * Получает отображаемое название предмета
     */
    public static String getItemDisplayName(String itemId) {
        if (itemId == null) {
            return "Неизвестно";
        }
        
        // Убираем префикс minecraft:
        String cleanId = itemId.replace("minecraft:", "");
        
        // Заменяем подчеркивания на пробелы и делаем первую букву заглавной
        String[] parts = cleanId.split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase())
                      .append(parts[i].substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
}