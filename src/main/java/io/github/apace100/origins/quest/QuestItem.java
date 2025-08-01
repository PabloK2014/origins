package io.github.apace100.origins.quest;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Представление квеста как предмета для drag-and-drop операций
 */
public class QuestItem {
    private static final String QUEST_NBT_KEY = "QuestData";
    private static final String QUEST_ID_KEY = "QuestId";
    private static final String QUEST_TITLE_KEY = "QuestTitle";
    private static final String QUEST_DESCRIPTION_KEY = "QuestDescription";
    private static final String QUEST_LEVEL_KEY = "QuestLevel";
    private static final String QUEST_CLASS_KEY = "QuestClass";
    private static final String QUEST_TIME_LIMIT_KEY = "QuestTimeLimit";
    
    /**
     * Создает ItemStack представляющий квест
     */
    public static ItemStack createQuestStack(Quest quest) {
        if (quest == null) {
            return ItemStack.EMPTY;
        }
        
        // Используем соответствующий билет квеста в зависимости от редкости
        Item questTicket = getQuestTicketForRarity(quest.getRarity());
        ItemStack stack = new ItemStack(questTicket);
        
        // Добавляем NBT данные квеста
        NbtCompound questNbt = new NbtCompound();
        questNbt.putString(QUEST_ID_KEY, quest.getId());
        questNbt.putString(QUEST_TITLE_KEY, quest.getTitle());
        questNbt.putString(QUEST_DESCRIPTION_KEY, quest.getDescription());
        questNbt.putInt(QUEST_LEVEL_KEY, quest.getLevel());
        questNbt.putString(QUEST_CLASS_KEY, quest.getPlayerClass());
        questNbt.putInt(QUEST_TIME_LIMIT_KEY, quest.getTimeLimit());
        
        // Сохраняем данные цели квеста
        if (quest.getObjective() != null) {
            NbtCompound objectiveNbt = new NbtCompound();
            objectiveNbt.putString("type", quest.getObjective().getType().name());
            objectiveNbt.putString("target", quest.getObjective().getTarget());
            objectiveNbt.putInt("amount", quest.getObjective().getAmount());
            questNbt.put("objective", objectiveNbt);
        }
        
        // Сохраняем данные награды
        if (quest.getReward() != null) {
            NbtCompound rewardNbt = new NbtCompound();
            rewardNbt.putString("type", quest.getReward().getType().name());
            rewardNbt.putInt("tier", quest.getReward().getTier());
            rewardNbt.putInt("experience", quest.getReward().getExperience());
            questNbt.put("reward", rewardNbt);
        }
        
        stack.getOrCreateNbt().put(QUEST_NBT_KEY, questNbt);
        
        // Устанавливаем кастомное название
        stack.setCustomName(quest.getFormattedTitle());
        
        return stack;
    }
    
    /**
     * Извлекает квест из ItemStack
     */
    public static Quest getQuestFromStack(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof QuestTicketItem)) {
        io.github.apace100.origins.Origins.LOGGER.warn("Попытка получить квест из пустого или неверного ItemStack: {}", stack);
        return null;
    }
    
    NbtCompound nbt = stack.getNbt();
    if (nbt == null) {
        io.github.apace100.origins.Origins.LOGGER.warn("Билет квеста не содержит NBT: {}", stack);
        return null;
    }
    

    
    // Проверяем оба формата: новый (с QuestData) и старый (прямо в NBT)
    NbtCompound questNbt;
    if (nbt.contains(QUEST_NBT_KEY)) {
        questNbt = nbt.getCompound(QUEST_NBT_KEY);
    } else if (nbt.contains("quest_id")) {
        questNbt = nbt;
    } else {
        io.github.apace100.origins.Origins.LOGGER.warn("Билет квеста не содержит QuestData или quest_id: {}", nbt);
        return null;
    }
    
    try {
        String id = questNbt.contains(QUEST_ID_KEY) ? 
            questNbt.getString(QUEST_ID_KEY) : questNbt.getString("quest_id");
        String title = questNbt.contains(QUEST_TITLE_KEY) ? 
            questNbt.getString(QUEST_TITLE_KEY) : questNbt.getString("quest_title");
        String description = questNbt.contains(QUEST_DESCRIPTION_KEY) ? 
            questNbt.getString(QUEST_DESCRIPTION_KEY) : "";
        int level = questNbt.contains(QUEST_LEVEL_KEY) ? 
            questNbt.getInt(QUEST_LEVEL_KEY) : 1;
        String playerClass = questNbt.contains(QUEST_CLASS_KEY) ? 
            questNbt.getString(QUEST_CLASS_KEY) : questNbt.getString("quest_class");
        int timeLimit = questNbt.contains(QUEST_TIME_LIMIT_KEY) ? 
            questNbt.getInt(QUEST_TIME_LIMIT_KEY) : 60;
            

        
        // Восстанавливаем цель квеста
        QuestObjective objective = null;
        if (questNbt.contains("objective")) {
            NbtCompound objectiveNbt = questNbt.getCompound("objective");
            try {
                QuestObjective.ObjectiveType type = QuestObjective.ObjectiveType.valueOf(
                    objectiveNbt.getString("type").toUpperCase());
                String target = objectiveNbt.getString("target");
                int amount = objectiveNbt.getInt("amount");
                if (target.isEmpty() || amount <= 0) {
                    io.github.apace100.origins.Origins.LOGGER.error("Невалидная цель квеста: target={}, amount={}", target, amount);
                    return null;
                }
                objective = new QuestObjective(type, target, amount);
                
                if (objectiveNbt.contains("progress")) {
                    objective.setProgress(objectiveNbt.getInt("progress"));
                }
                if (objectiveNbt.getBoolean("completed")) {
                    objective.setCompleted(true);
                }
            } catch (Exception e) {
                io.github.apace100.origins.Origins.LOGGER.error("Ошибка при восстановлении цели для квеста {}: {}", id, e.getMessage());
                return null; // Не создаём квест без валидной цели
            }
        } else {
            io.github.apace100.origins.Origins.LOGGER.error("Билет квеста {} не содержит данных цели", id);
            return null;
        }
        
        // Восстанавливаем награду
        QuestReward reward = null;
        if (questNbt.contains("reward")) {
            NbtCompound rewardNbt = questNbt.getCompound("reward");
            try {
                QuestReward.RewardType type = QuestReward.RewardType.valueOf(
                    rewardNbt.getString("type").toUpperCase());
                int tier = rewardNbt.getInt("tier");
                int experience = rewardNbt.contains("experience") ? 
                    rewardNbt.getInt("experience") : 
                    (rewardNbt.contains("amount") ? rewardNbt.getInt("amount") : 500);
                reward = new QuestReward(type, tier, experience);
            } catch (Exception e) {
                io.github.apace100.origins.Origins.LOGGER.warn("Ошибка при восстановлении награды для квеста {}: {}", id, e.getMessage());
            }
        } else if (questNbt.contains("rewards")) {
            // Пытаемся загрузить из нового формата
            NbtCompound rewardsNbt = questNbt.getCompound("rewards");
            if (rewardsNbt.contains("reward_0")) {
                NbtCompound rewardNbt = rewardsNbt.getCompound("reward_0");
                try {
                    QuestReward.RewardType type = QuestReward.RewardType.valueOf(
                        rewardNbt.getString("type").toUpperCase());
                    int tier = rewardNbt.getInt("tier");
                    int experience = rewardNbt.getInt("experience");
                    reward = new QuestReward(type, tier, experience);
                } catch (Exception e) {
                    io.github.apace100.origins.Origins.LOGGER.warn("Ошибка при восстановлении награды из rewards для квеста {}: {}", id, e.getMessage());
                }
            }
        }
        
        // Если награда не найдена, создаем награду по умолчанию
        if (reward == null) {
            io.github.apace100.origins.Origins.LOGGER.warn("Награда не найдена для квеста {}, создаем награду по умолчанию", id);
            reward = new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 500);
        }
        
        if (objective == null) {
            io.github.apace100.origins.Origins.LOGGER.error("Квест {} не имеет валидной цели", id);
            return null;
        }
        
        // Предполагаем, что Quest принимает List<QuestObjective>
        java.util.List<QuestObjective> objectives = new java.util.ArrayList<>();
        objectives.add(objective);
        
        Quest quest = new Quest(id, playerClass, level, title, description, objective, timeLimit, reward);

        return quest;
    } catch (Exception e) {
        io.github.apace100.origins.Origins.LOGGER.error("Ошибка при восстановлении квеста из ItemStack: {}", e.getMessage());
        return null;
    }
}
    
    /**
     * Проверяет, является ли ItemStack квестом
     */
    public static boolean isQuestStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof QuestTicketItem)) {
            return false;
        }
        
        NbtCompound nbt = stack.getNbt();
        return nbt != null && (nbt.contains(QUEST_NBT_KEY) || nbt.contains("quest_id"));
    }
    
    /**
     * Получает отформатированное название квеста из ItemStack
     */
    public static Text getQuestTitle(ItemStack stack) {
        Quest quest = getQuestFromStack(stack);
        if (quest != null) {
            return quest.getFormattedTitle();
        }
        return Text.literal("Неизвестный квест").formatted(Formatting.GRAY);
    }
    
    /**
     * Получает описание квеста из ItemStack
     */
    public static Text getQuestDescription(ItemStack stack) {
        Quest quest = getQuestFromStack(stack);
        if (quest != null) {
            return Text.literal(quest.getDescription()).formatted(Formatting.GRAY);
        }
        return Text.literal("Описание недоступно").formatted(Formatting.DARK_GRAY);
    }
    
    /**
     * Получает информацию о цели квеста
     */
    public static Text getQuestObjective(ItemStack stack) {
        Quest quest = getQuestFromStack(stack);
        if (quest != null && quest.getObjective() != null) {
            QuestObjective objective = quest.getObjective();
            
            // Используем прямые строки вместо локализации
            String actionText = switch (objective.getType()) {
                case COLLECT -> "Собрать";
                case KILL -> "Убить";
                case CRAFT -> "Создать";
                default -> "Выполнить";
            };
            
            // Получаем читаемое название предмета/сущности
            String itemName = getReadableItemName(objective.getTarget());
            
            // Добавляем прогресс
            String progressText = "(" + objective.getProgress() + "/" + objective.getAmount() + ")";
            
            return Text.literal(actionText + ": " + itemName + " x" + objective.getAmount() + " " + progressText).formatted(Formatting.YELLOW);
        }
        return Text.literal("Цель не указана").formatted(Formatting.DARK_GRAY);
    }
    
    /**
     * Получает читаемое название предмета
     */
    private static String getReadableItemName(String itemId) {
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
     * Получает информацию о награде квеста
     */
    public static Text getQuestReward(ItemStack stack) {
        Quest quest = getQuestFromStack(stack);
        if (quest != null && quest.getReward() != null) {
            QuestReward reward = quest.getReward();
            String rewardText = switch (reward.getType()) {
                case EXPERIENCE -> "Опыт: " + reward.getExperience() + " XP";
                case SKILL_POINT_TOKEN -> "Очки навыков: " + reward.getExperience() + " (Уровень " + reward.getTier() + ")";
                case ITEM -> "Предмет (Уровень " + reward.getTier() + ")";
                default -> "Награда (Уровень " + reward.getTier() + ")";
            };
            return Text.literal(rewardText).formatted(Formatting.GREEN);
        }
        return Text.literal("Награда неизвестна").formatted(Formatting.DARK_GRAY);
    }
    
    /**
     * Получает информацию о времени выполнения квеста
     */
    public static Text getQuestTimeLimit(ItemStack stack) {
        Quest quest = getQuestFromStack(stack);
        if (quest != null) {
            int timeLimit = quest.getTimeLimit();
            String timeText = timeLimit >= 60 ? 
                (timeLimit / 60) + " ч " + (timeLimit % 60) + " мин" : 
                timeLimit + " мин";
            return Text.literal("Время: " + timeText).formatted(Formatting.AQUA);
        }
        return Text.literal("Время неизвестно").formatted(Formatting.DARK_GRAY);
    }
    
    /**
     * Получает актуальную информацию о времени квеста (лимит или оставшееся время)
     */
    public static Text getQuestTimeInfo(ItemStack stack) {
        Quest quest = getQuestFromStack(stack);
        if (quest == null) {
            return Text.literal("Время неизвестно").formatted(Formatting.DARK_GRAY);
        }
        
        // Если у квеста нет лимита времени, показываем это
        if (quest.getTimeLimit() <= 0) {
            return Text.literal("Без ограничения по времени").formatted(Formatting.AQUA);
        }
        
        // Проверяем, является ли это билетом квеста
        if (!(stack.getItem() instanceof QuestTicketItem)) {
            // Для обычных квестов показываем лимит времени
            return getQuestTimeLimit(stack);
        }
        
        // Для билетов квестов проверяем состояние
        QuestTicketState state = QuestTicketItem.getTicketState(stack);
        long acceptTime = QuestTicketItem.getAcceptTime(stack);
        
        // Если квест активен и есть время принятия, показываем оставшееся время
        if (state.isActive() && acceptTime > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - acceptTime) / 1000;
            long totalLimitSeconds = quest.getTimeLimit() * 60; // конвертируем минуты в секунды
            long remainingSeconds = Math.max(0, totalLimitSeconds - elapsedSeconds);
            
            // Форматируем оставшееся время с секундами
            String timeText;
            Formatting timeColor;
            
            if (remainingSeconds <= 0) {
                timeText = "Время: ИСТЕКЛО!";
                timeColor = Formatting.DARK_RED;
            } else {
                // Вычисляем часы, минуты и секунды
                long hours = remainingSeconds / 3600;
                long minutes = (remainingSeconds % 3600) / 60;
                long seconds = remainingSeconds % 60;
                
                // Форматируем время в зависимости от оставшегося времени
                if (remainingSeconds <= 300) { // Меньше 5 минут - показываем секунды и делаем красным
                    if (minutes > 0) {
                        timeText = String.format("Время: %dм %02dс (СРОЧНО!)", minutes, seconds);
                    } else {
                        timeText = String.format("Время: %dс (СРОЧНО!)", seconds);
                    }
                    timeColor = Formatting.RED;
                } else if (remainingSeconds <= 900) { // Меньше 15 минут - показываем минуты и секунды
                    timeText = String.format("Время: %dм %02dс", minutes, seconds);
                    timeColor = Formatting.YELLOW;
                } else if (remainingSeconds < 3600) { // Меньше часа - показываем минуты и секунды
                    timeText = String.format("Время: %dм %02dс", minutes, seconds);
                    timeColor = Formatting.AQUA;
                } else { // Больше часа - показываем часы, минуты и секунды
                    timeText = String.format("Время: %dч %02dм %02dс", hours, minutes, seconds);
                    timeColor = Formatting.AQUA;
                }
            }
            
            return Text.literal(timeText).formatted(timeColor);
        } else {
            // Для неактивных квестов показываем лимит времени
            return getQuestTimeLimit(stack);
        }
    }
    
    /**
     * Получает читаемое название предмета
     */
    private static String getItemName(String itemId) {
        String[] parts = itemId.replace("minecraft:", "").split("_");
        StringBuilder name = new StringBuilder();
        
        for (String part : parts) {
            if (name.length() > 0) name.append(" ");
            name.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        
        return name.toString();
    }
    
    /**
     * Получает локализованное название предмета
     */
    private static String getLocalizedItemName(String itemId) {
        try {
            // Пытаемся получить локализованное название через Minecraft
            net.minecraft.util.Identifier id = new net.minecraft.util.Identifier(itemId);
            net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(id);
            if (item != null && item != net.minecraft.item.Items.AIR) {
                return item.getName().getString();
            }
            
            // Если не удалось, пытаемся получить название блока
            net.minecraft.block.Block block = net.minecraft.registry.Registries.BLOCK.get(id);
            if (block != null && block != net.minecraft.block.Blocks.AIR) {
                return block.getName().getString();
            }
            
            // Если не удалось, пытаемся получить название сущности
            net.minecraft.entity.EntityType<?> entityType = net.minecraft.registry.Registries.ENTITY_TYPE.get(id);
            if (entityType != null) {
                return entityType.getName().getString();
            }
        } catch (Exception e) {
            // Игнорируем ошибки и используем fallback
        }
        
        // Fallback к читаемому названию
        return getItemName(itemId);
    }
    
    /**
     * Получает читаемое название сущности
     */
    private static String getEntityName(String entityId) {
        return getItemName(entityId);
    }
    
    /**
     * Создает копию ItemStack квеста
     */
    public static ItemStack copyQuestStack(ItemStack original) {
        if (!isQuestStack(original)) {
            return ItemStack.EMPTY;
        }
        
        return original.copy();
    }
    
    /**
     * Проверяет, одинаковые ли квесты в двух ItemStack
     */
    public static boolean areQuestsEqual(ItemStack stack1, ItemStack stack2) {
        if (!isQuestStack(stack1) || !isQuestStack(stack2)) {
            return false;
        }
        
        Quest quest1 = getQuestFromStack(stack1);
        Quest quest2 = getQuestFromStack(stack2);
        
        if (quest1 == null || quest2 == null) {
            return false;
        }
        
        return quest1.getId().equals(quest2.getId());
    }
    
    /**
     * Создает tooltip для квеста
     */
    public static void addQuestTooltip(ItemStack stack, List<Text> tooltip) {
        Quest quest = getQuestFromStack(stack);
        if (quest == null) {
            tooltip.add(Text.literal("Поврежденный квест").formatted(Formatting.RED));
            return;
        }
        
        // Добавляем описание
        if (!quest.getDescription().isEmpty()) {
            tooltip.add(Text.literal(quest.getDescription()).formatted(Formatting.GRAY));
            tooltip.add(Text.empty());
        }
        
        // Добавляем информацию о цели
        if (quest.getObjective() != null) {
            tooltip.add(Text.literal("Цель:").formatted(Formatting.YELLOW));
            tooltip.add(getQuestObjective(stack));
            tooltip.add(Text.empty());
        }
        
        // Добавляем информацию о награде
        if (quest.getReward() != null) {
            tooltip.add(Text.literal("Награда:").formatted(Formatting.GREEN));
            tooltip.add(getQuestReward(stack));
            tooltip.add(Text.empty());
        }
        
        // Добавляем информацию о времени и классе
        tooltip.add(getQuestTimeInfo(stack));
        
        if (!quest.getPlayerClass().equals("any")) {
            String playerClass = quest.getPlayerClass();
            // Убираем префикс "origins:" если есть
            if (playerClass.startsWith("origins:")) {
                playerClass = playerClass.substring(8);
            }
            tooltip.add(Text.literal("Класс: " + getClassDisplayName(playerClass)).formatted(Formatting.AQUA));
        }
        
        // Добавляем редкость
        tooltip.add(Text.literal("Редкость: " + getRarityDisplayName(quest.getRarity())).formatted(Formatting.WHITE));
    }
    
    /**
     * Обновляет NBT данные квеста в ItemStack
     */
    public static ItemStack updateQuestStack(ItemStack stack, Quest quest) {
        if (stack.isEmpty() || quest == null) {
            return stack;
        }
        
        // Обновляем NBT данные
        NbtCompound questNbt = new NbtCompound();
        questNbt.putString(QUEST_ID_KEY, quest.getId());
        questNbt.putString(QUEST_TITLE_KEY, quest.getTitle());
        questNbt.putString(QUEST_DESCRIPTION_KEY, quest.getDescription());
        questNbt.putInt(QUEST_LEVEL_KEY, quest.getLevel());
        questNbt.putString(QUEST_CLASS_KEY, quest.getPlayerClass());
        questNbt.putInt(QUEST_TIME_LIMIT_KEY, quest.getTimeLimit());
        
        // Обновляем данные цели
        if (quest.getObjective() != null) {
            NbtCompound objectiveNbt = new NbtCompound();
            objectiveNbt.putString("type", quest.getObjective().getType().name());
            objectiveNbt.putString("target", quest.getObjective().getTarget());
            objectiveNbt.putInt("amount", quest.getObjective().getAmount());
            objectiveNbt.putInt("progress", quest.getObjective().getProgress());
            objectiveNbt.putBoolean("completed", quest.getObjective().isCompleted());
            questNbt.put("objective", objectiveNbt);
        }
        
        // Обновляем данные награды
        if (quest.getReward() != null) {
            NbtCompound rewardNbt = new NbtCompound();
            rewardNbt.putString("type", quest.getReward().getType().name());
            rewardNbt.putInt("tier", quest.getReward().getTier());
            rewardNbt.putInt("experience", quest.getReward().getExperience());
            questNbt.put("reward", rewardNbt);
        }
        
        stack.getOrCreateNbt().put(QUEST_NBT_KEY, questNbt);
        stack.setCustomName(quest.getFormattedTitle());
        
        return stack;
    }
    
    /**
     * Получает отображаемое название класса
     */
    public static String getClassDisplayName(String playerClass) {
        // Убираем префикс "origins:" если есть
        String cleanClass = playerClass;
        if (cleanClass.startsWith("origins:")) {
            cleanClass = cleanClass.substring(8);
        }
        
        return switch (cleanClass) {
            case "warrior" -> "Воин";
            case "miner" -> "Шахтер";
            case "blacksmith" -> "Кузнец";
            case "courier" -> "Курьер";
            case "brewer" -> "Пивовар";
            case "cook" -> "Повар";
            default -> cleanClass;
        };
    }
    
    /**
     * Получает отображаемое название редкости
     */
    private static String getRarityDisplayName(Quest.QuestRarity rarity) {
        return switch (rarity) {
            case COMMON -> "Обычный";
            case UNCOMMON -> "Необычный";
            case RARE -> "Редкий";
            case EPIC -> "Эпический";
            default -> "Неизвестный";
        };
    }
    
    /**
     * Получает соответствующий билет квеста для редкости
     */
    private static Item getQuestTicketForRarity(Quest.QuestRarity rarity) {
        return switch (rarity) {
            case COMMON -> QuestRegistry.QUEST_TICKET_COMMON;
            case UNCOMMON -> QuestRegistry.QUEST_TICKET_UNCOMMON;
            case RARE -> QuestRegistry.QUEST_TICKET_RARE;
            case EPIC -> QuestRegistry.QUEST_TICKET_EPIC;
        };
    }
}