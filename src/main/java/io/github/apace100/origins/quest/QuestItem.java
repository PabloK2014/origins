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
            return null;
        }
        
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return null;
        }
        
        // Проверяем оба формата: новый (с QuestData) и старый (прямо в NBT)
        NbtCompound questNbt;
        if (nbt.contains(QUEST_NBT_KEY)) {
            questNbt = nbt.getCompound(QUEST_NBT_KEY);
        } else if (nbt.contains("quest_id")) {
            // Используем данные напрямую из NBT (формат QuestTicketItem)
            questNbt = nbt;
        } else {
            return null;
        }
        
        try {
            // Поддерживаем оба формата данных
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
                    objective = new QuestObjective(type, target, amount);
                    
                    // Восстанавливаем прогресс если есть
                    if (objectiveNbt.contains("progress")) {
                        objective.setProgress(objectiveNbt.getInt("progress"));
                    }
                    if (objectiveNbt.getBoolean("completed")) {
                        objective.setCompleted(true);
                    }
                } catch (Exception e) {
                    io.github.apace100.origins.Origins.LOGGER.warn("Ошибка при восстановлении цели: {}", e.getMessage());
                }
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
                    io.github.apace100.origins.Origins.LOGGER.warn("Ошибка при восстановлении награды: {}", e.getMessage());
                }
            }
            
            // Создаем квест
            return new Quest(id, playerClass, level, title, description, 
                           objective, timeLimit, reward);
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error(
                "Ошибка при восстановлении квеста из ItemStack: " + e.getMessage());
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
            
            // Используем локализованные строки
            String translationKey = switch (objective.getType()) {
                case COLLECT -> "quest.origins.objective.collect";
                case KILL -> "quest.origins.objective.kill";
                case CRAFT -> "quest.origins.objective.craft";
                default -> "quest.origins.objective.unknown";
            };
            
            // Получаем локализованное название предмета/сущности
            String itemName = getLocalizedItemName(objective.getTarget());
            
            return Text.translatable(translationKey, 0, objective.getAmount(), itemName).formatted(Formatting.YELLOW);
        }
        return Text.translatable("quest.origins.objective.unknown").formatted(Formatting.DARK_GRAY);
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
        tooltip.add(getQuestTimeLimit(stack));
        
        if (!quest.getPlayerClass().equals("any")) {
            tooltip.add(Text.literal("Класс: " + getClassDisplayName(quest.getPlayerClass())).formatted(Formatting.AQUA));
        }
        
        // Добавляем редкость
        tooltip.add(Text.literal("Редкость: ").formatted(Formatting.WHITE)
            .append(quest.getRarity().getDisplayName()));
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
    private static String getClassDisplayName(String playerClass) {
        return switch (playerClass) {
            case "warrior" -> "Воин";
            case "miner" -> "Шахтер";
            case "blacksmith" -> "Кузнец";
            case "courier" -> "Курьер";
            case "brewer" -> "Пивовар";
            case "cook" -> "Повар";
            default -> playerClass;
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