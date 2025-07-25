package io.github.apace100.origins.quest.gui;

import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestObjective;
import io.github.apace100.origins.quest.QuestReward;
import io.github.apace100.origins.quest.ActiveQuest;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для отрисовки подсказок квестов в интерфейсе доски объявлений.
 * Заменяет функциональность подсказок из оригинального мода Bountiful.
 */
public class QuestTooltipRenderer {
    
    /**
     * Отрисовывает подсказку для квеста
     */
    public static void renderQuestTooltip(DrawContext context, Quest quest, int mouseX, int mouseY) {
        if (quest == null) return;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<Text> tooltip = buildQuestTooltip(quest);
        
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }
    
    /**
     * Отрисовывает подсказку для активного квеста
     */
    public static void renderActiveQuestTooltip(DrawContext context, ActiveQuest activeQuest, int mouseX, int mouseY) {
        if (activeQuest == null) return;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<Text> tooltip = buildActiveQuestTooltip(activeQuest);
        
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }
    
    /**
     * Строит список текста для подсказки квеста
     */
    private static List<Text> buildQuestTooltip(Quest quest) {
        List<Text> tooltip = new ArrayList<>();
        
        // Заголовок квеста
        tooltip.add(Text.literal(quest.getTitle()).formatted(Formatting.YELLOW, Formatting.BOLD));
        
        // Описание квеста
        if (!quest.getDescription().isEmpty()) {
            tooltip.add(Text.literal(quest.getDescription()).formatted(Formatting.GRAY));
            tooltip.add(Text.empty()); // Пустая строка для разделения
        }
        
        // Класс квеста
        tooltip.add(Text.literal("Класс: " + getLocalizedClassName(quest.getPlayerClass()))
                .formatted(Formatting.GOLD));
        
        // Уровень квеста
        tooltip.add(Text.literal("Уровень: " + quest.getLevel())
                .formatted(getLevelFormatting(quest.getLevel())));
        
        // Время выполнения
        if (quest.getTimeLimit() > 0) {
            tooltip.add(Text.literal("Время: " + quest.getTimeLimit() + " мин")
                    .formatted(Formatting.RED));
        }
        
        tooltip.add(Text.empty()); // Разделитель
        
        // Цель квеста
        tooltip.add(Text.literal("Цель:").formatted(Formatting.AQUA, Formatting.UNDERLINE));
        QuestObjective objective = quest.getObjective();
        if (objective != null) {
            tooltip.add(buildObjectiveText(objective));
        }
        
        tooltip.add(Text.empty()); // Разделитель
        
        // Награда квеста
        tooltip.add(Text.literal("Награда:").formatted(Formatting.GREEN, Formatting.UNDERLINE));
        QuestReward reward = quest.getReward();
        if (reward != null) {
            tooltip.add(buildRewardText(reward));
        }
        
        return tooltip;
    }
    
    /**
     * Строит список текста для подсказки активного квеста
     */
    private static List<Text> buildActiveQuestTooltip(ActiveQuest activeQuest) {
        List<Text> tooltip = buildQuestTooltip(activeQuest.getQuest());
        
        // Добавляем информацию о прогрессе
        tooltip.add(Text.empty()); // Разделитель
        tooltip.add(Text.literal("Прогресс:").formatted(Formatting.LIGHT_PURPLE, Formatting.UNDERLINE));
        
        float progressPercent = activeQuest.getProgressPercentage();
        Formatting progressColor = progressPercent >= 100 ? Formatting.GREEN : 
                                  progressPercent >= 50 ? Formatting.YELLOW : Formatting.RED;
        
        tooltip.add(Text.literal(String.format("%.1f%% выполнено", progressPercent))
                .formatted(progressColor));
        
        // Оставшееся время
        long remainingTime = activeQuest.getRemainingTimeMinutes();
        if (remainingTime >= 0) {
            Formatting timeColor = remainingTime > 10 ? Formatting.GREEN :
                                  remainingTime > 5 ? Formatting.YELLOW : Formatting.RED;
            
            tooltip.add(Text.literal("Осталось: " + remainingTime + " мин")
                    .formatted(timeColor));
        } else {
            tooltip.add(Text.literal("Без ограничения времени").formatted(Formatting.GRAY));
        }
        
        // Детальный прогресс по целям
        Quest quest = activeQuest.getQuest();
        QuestObjective objective = quest.getObjective();
        if (objective != null) {
            tooltip.add(Text.literal("Цель: " + objective.getProgress() + "/" + objective.getAmount())
                    .formatted(objective.isCompleted() ? Formatting.GREEN : Formatting.YELLOW));
        }
        
        return tooltip;
    }
    
    /**
     * Строит текст для цели квеста
     */
    private static Text buildObjectiveText(QuestObjective objective) {
        String prefix = objective.isCompleted() ? "✓ " : "• ";
        Formatting color = objective.isCompleted() ? Formatting.GREEN : Formatting.WHITE;
        
        String objectiveText;
        switch (objective.getType()) {
            case COLLECT:
                objectiveText = "Собрать " + getItemName(objective.getTarget()) + " x" + objective.getAmount();
                break;
            case KILL:
                objectiveText = "Убить " + getEntityName(objective.getTarget()) + " x" + objective.getAmount();
                break;
            case CRAFT:
                objectiveText = "Создать " + getItemName(objective.getTarget()) + " x" + objective.getAmount();
                break;
            default:
                objectiveText = objective.getTarget() + " x" + objective.getAmount();
                break;
        }
        
        return Text.literal(prefix + objectiveText).formatted(color);
    }
    
    /**
     * Строит текст для награды квеста
     */
    private static Text buildRewardText(QuestReward reward) {
        String rewardText;
        if (reward.getType() == QuestReward.RewardType.SKILL_POINT_TOKEN) {
            rewardText = "Токен опыта (" + reward.getExperience() + " опыта)";
        } else {
            rewardText = "Неизвестная награда";
        }
        
        Formatting color;
        switch (reward.getTier()) {
            case 1:
                color = Formatting.WHITE;
                break;
            case 2:
                color = Formatting.YELLOW;
                break;
            case 3:
                color = Formatting.AQUA;
                break;
            default:
                color = Formatting.GRAY;
                break;
        }
        
        return Text.literal("• " + rewardText).formatted(color);
    }
    
    /**
     * Получает локализованное название класса
     */
    private static String getLocalizedClassName(String playerClass) {
        switch (playerClass) {
            case "warrior":
                return "Воин";
            case "miner":
                return "Шахтер";
            case "blacksmith":
                return "Кузнец";
            case "courier":
                return "Курьер";
            case "brewer":
                return "Пивовар";
            case "cook":
                return "Повар";
            case "human":
                return "Человек";
            default:
                return "Неизвестный";
        }
    }
    
    /**
     * Получает форматирование для уровня квеста
     */
    private static Formatting getLevelFormatting(int level) {
        switch (level) {
            case 1:
                return Formatting.GREEN;
            case 2:
                return Formatting.YELLOW;
            case 3:
                return Formatting.RED;
            default:
                return Formatting.WHITE;
        }
    }
    
    /**
     * Получает читаемое название предмета
     */
    private static String getItemName(String itemId) {
        try {
            net.minecraft.util.Identifier identifier = new net.minecraft.util.Identifier(itemId);
            net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(identifier);
            if (item != null) {
                return item.getName().getString();
            }
        } catch (Exception e) {
            // Fallback к простому преобразованию
        }
        
        // Простое преобразование ID предмета в читаемое название
        String[] parts = itemId.replace("minecraft:", "").split("_");
        StringBuilder name = new StringBuilder();
        
        for (String part : parts) {
            if (name.length() > 0) name.append(" ");
            name.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        
        return name.toString();
    }
    
    /**
     * Получает читаемое название сущности
     */
    private static String getEntityName(String entityId) {
        try {
            net.minecraft.util.Identifier identifier = new net.minecraft.util.Identifier(entityId);
            net.minecraft.entity.EntityType<?> entityType = net.minecraft.registry.Registries.ENTITY_TYPE.get(identifier);
            if (entityType != null) {
                return entityType.getName().getString();
            }
        } catch (Exception e) {
            // Fallback к простому преобразованию
        }
        
        // Простое преобразование ID сущности в читаемое название
        return getItemName(entityId); // Используем ту же логику
    }
    
    /**
     * Отрисовывает компактную подсказку для цели квеста
     */
    public static void renderObjectiveTooltip(DrawContext context, QuestObjective objective, int mouseX, int mouseY) {
        if (objective == null) return;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<Text> tooltip = new ArrayList<>();
        
        // Тип цели
        String typeText;
        switch (objective.getType()) {
            case COLLECT:
                typeText = "Собрать";
                break;
            case KILL:
                typeText = "Убить";
                break;
            case CRAFT:
                typeText = "Создать";
                break;
            default:
                typeText = "Выполнить";
                break;
        }
        
        tooltip.add(Text.literal(typeText).formatted(Formatting.YELLOW));
        tooltip.add(Text.literal(getItemName(objective.getTarget())).formatted(Formatting.WHITE));
        tooltip.add(Text.literal("Количество: " + objective.getAmount()).formatted(Formatting.GRAY));
        
        if (objective.isCompleted()) {
            tooltip.add(Text.literal("✓ Выполнено").formatted(Formatting.GREEN));
        } else {
            tooltip.add(Text.literal("Прогресс: " + objective.getProgress() + "/" + objective.getAmount())
                    .formatted(Formatting.YELLOW));
        }
        
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }
    
    /**
     * Отрисовывает компактную подсказку для награды квеста
     */
    public static void renderRewardTooltip(DrawContext context, QuestReward reward, int mouseX, int mouseY) {
        if (reward == null) return;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<Text> tooltip = new ArrayList<>();
        
        tooltip.add(Text.literal("Награда").formatted(Formatting.GREEN, Formatting.BOLD));
        
        String rewardText;
        if (reward.getType() == QuestReward.RewardType.SKILL_POINT_TOKEN) {
            rewardText = "Токен опыта";
        } else {
            rewardText = "Неизвестная награда";
        }
        
        tooltip.add(Text.literal(rewardText).formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("Опыт: " + reward.getExperience()).formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Уровень: " + reward.getTier()).formatted(getLevelFormatting(reward.getTier())));
        
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }
    
    /**
     * Отрисовывает подсказку для полосы прогресса квеста
     */
    public static void renderProgressTooltip(DrawContext context, ActiveQuest activeQuest, int mouseX, int mouseY) {
        if (activeQuest == null) return;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<Text> tooltip = new ArrayList<>();
        
        Quest quest = activeQuest.getQuest();
        QuestObjective objective = quest.getObjective();
        
        tooltip.add(Text.literal("Прогресс квеста").formatted(Formatting.YELLOW, Formatting.BOLD));
        tooltip.add(Text.literal(quest.getTitle()).formatted(Formatting.WHITE));
        tooltip.add(Text.empty());
        
        if (objective != null) {
            float progressPercent = objective.getProgressPercentage() * 100;
            Formatting color = progressPercent >= 100 ? Formatting.GREEN :
                              progressPercent >= 50 ? Formatting.YELLOW : Formatting.RED;
            
            tooltip.add(Text.literal(String.format("%.1f%% выполнено", progressPercent)).formatted(color));
            tooltip.add(Text.literal(objective.getProgress() + "/" + objective.getAmount() + " " + 
                       getObjectiveTypeName(objective.getType())).formatted(Formatting.GRAY));
        }
        
        long remainingTime = activeQuest.getRemainingTimeMinutes();
        if (remainingTime >= 0) {
            Formatting timeColor = remainingTime > 10 ? Formatting.GREEN :
                                  remainingTime > 5 ? Formatting.YELLOW : Formatting.RED;
            tooltip.add(Text.literal("Осталось: " + remainingTime + " мин").formatted(timeColor));
        }
        
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }
    
    /**
     * Получает название типа цели
     */
    private static String getObjectiveTypeName(QuestObjective.ObjectiveType type) {
        return switch (type) {
            case COLLECT -> "собрано";
            case KILL -> "убито";
            case CRAFT -> "создано";
        };
    }
    
    /**
     * Отрисовывает краткую подсказку с основной информацией о квесте
     */
    public static void renderShortQuestTooltip(DrawContext context, Quest quest, int mouseX, int mouseY) {
        if (quest == null) return;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<Text> tooltip = new ArrayList<>();
        
        // Заголовок
        tooltip.add(Text.literal(quest.getTitle()).formatted(Formatting.YELLOW, Formatting.BOLD));
        
        // Класс и уровень
        tooltip.add(Text.literal("Класс: " + getLocalizedClassName(quest.getPlayerClass()) + 
                   " | Уровень: " + quest.getLevel()).formatted(Formatting.GRAY));
        
        // Время
        if (quest.getTimeLimit() > 0) {
            tooltip.add(Text.literal("Время: " + quest.getTimeLimit() + " мин").formatted(Formatting.RED));
        }
        
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }
    
    /**
     * Проверяет, нужно ли отображать подсказку (например, если текст не помещается)
     */
    public static boolean shouldShowTooltip(String text, int maxWidth) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        return textRenderer.getWidth(text) > maxWidth;
    }
}