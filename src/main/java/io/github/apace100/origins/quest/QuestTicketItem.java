package io.github.apace100.origins.quest;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

/**
 * Предмет билета квеста с соответствующей текстурой
 */
public class QuestTicketItem extends Item {
    private final Quest.QuestRarity rarity;
    
    public QuestTicketItem(Settings settings, Quest.QuestRarity rarity) {
        super(settings);
        this.rarity = rarity;
    }
    
    @Override
    public Text getName(ItemStack stack) {
        // Получаем квест из NBT данных
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest != null) {
            return getQuestSpecificName(stack, quest);
        }
        
        // Fallback к стандартному названию
        return super.getName(stack);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        try {
            super.appendTooltip(stack, world, tooltip, context);
            
            // Принудительно обновляем время для активных квестов при каждом отображении тултипа
            updateTimeForActiveQuest(stack);
            
            // Получаем квест из NBT данных
            Quest quest = QuestItem.getQuestFromStack(stack);
            if (quest != null) {
                // Добавляем состояние квеста
                QuestTicketState state = getTicketState(stack);
                tooltip.add(Text.literal("Состояние: " + state.getDisplayName())
                    .formatted(getStateFormatting(state)));
                
                // Добавляем полную информацию о квесте
                tooltip.add(Text.literal(""));
                QuestItem.addQuestTooltip(stack, tooltip);
                
                // Убираем отображение прогресса - время теперь отображается в основном тултипе
                
                // Добавляем подсказку для готовых к сдаче квестов
                if (state == QuestTicketState.COMPLETED) {
                    tooltip.add(Text.literal(""));
                    tooltip.add(Text.literal("Shift+ПКМ по доске объявлений для сдачи")
                        .formatted(Formatting.GOLD));
                }
            } else {
                // Если квест не найден, показываем отладочную информацию
                tooltip.add(Text.literal(""));
                tooltip.add(Text.literal("⚠ Поврежденный билет квеста").formatted(Formatting.RED));
                tooltip.add(Text.literal("Обратитесь к администратору").formatted(Formatting.GRAY));
            }
        } catch (Exception e) {
            // Если произошла ошибка при создании tooltip, показываем безопасную информацию
            tooltip.clear();
            tooltip.add(Text.literal("Билет квеста").formatted(Formatting.WHITE));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.literal("⚠ Ошибка отображения: " + e.getMessage()).formatted(Formatting.RED));
            tooltip.add(Text.literal("Обратитесь к администратору").formatted(Formatting.GRAY));
        }
    }
    
    /**
     * Принудительно обновляет время для активных квестов
     */
    private static void updateTimeForActiveQuest(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return;
        }
        
        QuestTicketState state = getTicketState(stack);
        if (!state.isActive()) {
            return;
        }
        
        long acceptTime = getAcceptTime(stack);
        if (acceptTime <= 0) {
            return;
        }
        
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest == null || quest.getTimeLimit() <= 0) {
            return;
        }
        
        // Проверяем, не истекло ли время (используем секунды для точности)
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - acceptTime) / 1000;
        long totalLimitSeconds = quest.getTimeLimit() * 60; // конвертируем минуты в секунды
        
        if (elapsedSeconds >= totalLimitSeconds) {
            // Время истекло, помечаем квест как проваленный
            markAsFailed(stack);
            io.github.apace100.origins.Origins.LOGGER.info("Квест {} помечен как проваленный из-за истечения времени", quest.getId());
        } else {
            // Обновляем метку времени для принудительного обновления tooltip
            net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putLong("tooltip_update_time", currentTime);
        }
    }
    
    /**
     * Получает форматирование для состояния квеста
     */
    private static Formatting getStateFormatting(QuestTicketState state) {
        return switch (state) {
            case AVAILABLE -> Formatting.WHITE;
            case ACCEPTED -> Formatting.YELLOW;
            case IN_PROGRESS -> Formatting.AQUA;
            case COMPLETED -> Formatting.GREEN;
            case FINISHED -> Formatting.GRAY;
            case FAILED -> Formatting.RED;
        };
    }
    
    /**
     * Добавляет информацию о прогрессе в tooltip
     */
    private static void addProgressTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        try {
            net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
            if (nbt == null) {
                tooltip.add(Text.literal("  • Нет данных о прогрессе").formatted(Formatting.GRAY));
                return;
            }
            
            // Проверяем новую систему с одной целью
            if (nbt.contains("objective")) {
                net.minecraft.nbt.NbtCompound objNbt = nbt.getCompound("objective");
                
                String type = objNbt.getString("type");
                String target = objNbt.getString("target");
                int amount = objNbt.getInt("amount");
                int progress = objNbt.getInt("progress");
                boolean completed = objNbt.getBoolean("completed");
                
                String progressText = progress + "/" + amount;
                Formatting progressColor;
                String statusIcon;
                
                if (completed) {
                    progressColor = Formatting.GREEN;
                    statusIcon = "✓ ";
                } else if (progress > 0) {
                    progressColor = Formatting.YELLOW;
                    statusIcon = "◐ ";
                } else {
                    progressColor = Formatting.RED;
                    statusIcon = "○ ";
                }
                
                String actionText = switch (type) {
                    case "collect" -> "Собрать";
                    case "kill" -> "Убить";
                    case "craft" -> "Создать";
                    case "mine" -> "Добыть";
                    case "smelt" -> "Переплавить";
                    case "brew" -> "Сварить";
                    case "cook" -> "Приготовить";
                    default -> "Выполнить";
                };
                
                tooltip.add(Text.literal("  " + statusIcon + actionText + ": " + getItemDisplayName(target) + " (" + progressText + ")")
                    .formatted(progressColor));
                
                return; // Выходим, так как обработали новую систему
            }
            
            // Fallback к старой системе с множественными целями
            net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
            int objectivesCount = nbt.getInt("objectives_count");
            
            if (objectivesCount == 0) {
                tooltip.add(Text.literal("  • Нет целей для отображения").formatted(Formatting.GRAY));
                return;
            }
            
            int completedObjectives = 0;
        
            for (int i = 0; i < objectivesCount; i++) {
                net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
                
                String type = objNbt.getString("type");
                String target = objNbt.getString("target");
                int amount = objNbt.getInt("amount");
                int progress = objNbt.getInt("progress");
                boolean completed = objNbt.getBoolean("completed");
                
                if (completed) completedObjectives++;
                
                String progressText = progress + "/" + amount;
                Formatting progressColor;
                String statusIcon;
                
                if (completed) {
                    progressColor = Formatting.GREEN;
                    statusIcon = "✓ ";
                } else if (progress > 0) {
                    progressColor = Formatting.YELLOW;
                    statusIcon = "◐ ";
                } else {
                    progressColor = Formatting.RED;
                    statusIcon = "○ ";
                }
                
                String actionText = switch (type) {
                    case "collect" -> "Собрать";
                    case "kill" -> "Убить";
                    case "craft" -> "Создать";
                case "mine" -> "Добыть";
                case "smelt" -> "Переплавить";
                case "brew" -> "Сварить";
                case "cook" -> "Приготовить";
                default -> "Выполнить";
            };
            
            tooltip.add(Text.literal("  " + statusIcon + actionText + ": " + getItemDisplayName(target) + " (" + progressText + ")")
                .formatted(progressColor));
        }
        
            // Добавляем общий прогресс
            if (objectivesCount > 1) {
                tooltip.add(Text.literal(""));
                Formatting overallColor = completedObjectives == objectivesCount ? Formatting.GREEN :
                                        completedObjectives > 0 ? Formatting.YELLOW : Formatting.RED;
                tooltip.add(Text.literal("Общий прогресс: " + completedObjectives + "/" + objectivesCount + " целей")
                    .formatted(overallColor));
            }
        } catch (Exception e) {
            tooltip.add(Text.literal("  • Ошибка отображения прогресса: " + e.getMessage()).formatted(Formatting.RED));
        }
    }
    
    /**
     * Добавляет информацию о времени в tooltip
     */
    private static void addTimeTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        try {
            if (quest == null) {
                tooltip.add(Text.literal("Нет информации о времени").formatted(Formatting.GRAY));
                return;
            }
            
            long acceptTime = getAcceptTime(stack);
            QuestTicketState state = getTicketState(stack);
            
            // Если квест принят и есть лимит времени
            if (state.isActive() && acceptTime > 0 && quest.getTimeLimit() > 0) {
                long currentTime = System.currentTimeMillis();
                long elapsedSeconds = (currentTime - acceptTime) / 1000;
                long totalLimitSeconds = quest.getTimeLimit() * 60; // конвертируем минуты в секунды
                long remainingSeconds = Math.max(0, totalLimitSeconds - elapsedSeconds);
                
                // Улучшенное отображение времени с секундами
                String timeText;
                Formatting timeColor;
                
                if (remainingSeconds <= 0) {
                    timeText = "ВРЕМЯ ИСТЕКЛО!";
                    timeColor = Formatting.DARK_RED;
                } else {
                    // Вычисляем часы, минуты и секунды
                    long hours = remainingSeconds / 3600;
                    long minutes = (remainingSeconds % 3600) / 60;
                    long seconds = remainingSeconds % 60;
                    
                    // Форматируем время в зависимости от оставшегося времени
                    if (remainingSeconds <= 300) { // Меньше 5 минут - показываем секунды
                        if (minutes > 0) {
                            timeText = String.format("Осталось времени: %dм %02dс (СРОЧНО!)", minutes, seconds);
                        } else {
                            timeText = String.format("Осталось времени: %dс (СРОЧНО!)", seconds);
                        }
                        timeColor = Formatting.RED;
                    } else if (remainingSeconds <= 900) { // Меньше 15 минут - показываем минуты и секунды
                        timeText = String.format("Осталось времени: %dм %02dс", minutes, seconds);
                        timeColor = Formatting.YELLOW;
                    } else if (remainingSeconds < 3600) { // Меньше часа - показываем минуты и секунды
                        timeText = String.format("Осталось времени: %dм %02dс", minutes, seconds);
                        timeColor = Formatting.GREEN;
                    } else { // Больше часа - показываем часы, минуты и секунды
                        timeText = String.format("Осталось времени: %dч %02dм %02dс", hours, minutes, seconds);
                        timeColor = Formatting.GREEN;
                    }
                }
                
                tooltip.add(Text.literal(timeText).formatted(timeColor));
            } else if (quest.getTimeLimit() > 0) {
                // Показываем лимит времени для неактивных квестов
                if (quest.getTimeLimit() >= 60) {
                    long hours = quest.getTimeLimit() / 60;
                    long minutes = quest.getTimeLimit() % 60;
                    tooltip.add(Text.literal("Лимит времени: " + hours + "ч " + minutes + "м")
                        .formatted(Formatting.WHITE));
                } else {
                    tooltip.add(Text.literal("Лимит времени: " + quest.getTimeLimit() + " мин")
                        .formatted(Formatting.WHITE));
                }
            } else {
                tooltip.add(Text.literal("Без ограничения по времени")
                    .formatted(Formatting.AQUA));
            }
        } catch (Exception e) {
            tooltip.add(Text.literal("Ошибка отображения времени: " + e.getMessage()).formatted(Formatting.RED));
        }
    }
    
    /**
     * Получает отображаемое название предмета
     */
    private static String getItemDisplayName(String itemId) {
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
    
    public Quest.QuestRarity getRarity() {
        return rarity;
    }
    
    /**
     * Создает ItemStack билета квеста
     */
    public static ItemStack createQuestTicket(Quest quest) {
        if (quest == null) return ItemStack.EMPTY;
        
        io.github.apace100.origins.Origins.LOGGER.info("Создаем билет для квеста: {} (редкость: {})", quest.getId(), quest.getRarity());
        
        // Выбираем подходящий предмет в зависимости от редкости
        Item ticketItem;
        switch (quest.getRarity()) {
            case COMMON:
                ticketItem = QuestRegistry.QUEST_TICKET_COMMON;
                break;
            case UNCOMMON:
                ticketItem = QuestRegistry.QUEST_TICKET_UNCOMMON;
                break;
            case RARE:
                ticketItem = QuestRegistry.QUEST_TICKET_RARE;
                break;
            case EPIC:
                ticketItem = QuestRegistry.QUEST_TICKET_EPIC;
                break;
            default:
                ticketItem = QuestRegistry.QUEST_TICKET_COMMON;
                break;
        }
        
        io.github.apace100.origins.Origins.LOGGER.info("Выбран предмет билета: {} (instanceof QuestTicketItem: {})", 
            ticketItem, ticketItem instanceof QuestTicketItem);
        
        ItemStack stack = new ItemStack(ticketItem);
        
        io.github.apace100.origins.Origins.LOGGER.info("Создан ItemStack: {} (isEmpty: {}, item: {})", 
            stack, stack.isEmpty(), stack.getItem());
        
        // Сохраняем данные квеста в NBT
        saveQuestToStack(stack, quest);
        
        io.github.apace100.origins.Origins.LOGGER.info("Билет создан с NBT: {}", stack.getNbt());
        
        return stack;
    }
    
    /**
     * Проверяет, является ли ItemStack билетом квеста
     */
    public static boolean isQuestTicket(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof QuestTicketItem;
    }
    
    /**
     * Сохраняет данные квеста в ItemStack
     */
    public static void saveQuestToStack(ItemStack stack, Quest quest) {
        if (quest == null || stack.isEmpty()) return;
        
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString("quest_id", quest.getId());
        nbt.putString("quest_title", quest.getTitle());
        nbt.putString("quest_class", quest.getPlayerClass());
        nbt.putString("quest_rarity", quest.getRarity().name());
        
        // Устанавливаем начальное состояние
        nbt.putString("quest_state", QuestTicketState.AVAILABLE.getName());
        nbt.putLong("accept_time", 0L);
        nbt.putBoolean("completion_ready", false);
        
        // Сохраняем отображаемое название
        updateTicketDisplayName(stack, quest);
        
        // Сохраняем цели (новая система с одной целью)
        QuestObjective objective = quest.getObjective();
        if (objective != null) {
            net.minecraft.nbt.NbtCompound objNbt = new net.minecraft.nbt.NbtCompound();
            objNbt.putString("type", objective.getType().getName());
            objNbt.putString("target", objective.getTarget());
            objNbt.putInt("amount", objective.getAmount());
            objNbt.putInt("progress", objective.getProgress());
            objNbt.putBoolean("completed", objective.isCompleted());
            nbt.put("objective", objNbt);
            
            io.github.apace100.origins.Origins.LOGGER.info("Сохранена цель квеста: type={}, target={}, amount={}", 
                objective.getType().getName(), objective.getTarget(), objective.getAmount());
        } else {
            // Fallback к старой системе с множественными целями
            net.minecraft.nbt.NbtCompound objectivesNbt = new net.minecraft.nbt.NbtCompound();
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                net.minecraft.nbt.NbtCompound objNbt = new net.minecraft.nbt.NbtCompound();
                objNbt.putString("type", obj.getType().getName());
                objNbt.putString("target", obj.getTarget());
                objNbt.putInt("amount", obj.getAmount());
                objNbt.putInt("progress", obj.getProgress());
                objNbt.putBoolean("completed", obj.isCompleted());
                objectivesNbt.put("objective_" + i, objNbt);
            }
            nbt.put("objectives", objectivesNbt);
            nbt.putInt("objectives_count", quest.getObjectives().size());
        }
        
        // Сохраняем награды (и в новом, и в старом формате для совместимости)
        net.minecraft.nbt.NbtCompound rewardsNbt = new net.minecraft.nbt.NbtCompound();
        for (int i = 0; i < quest.getRewards().size(); i++) {
            QuestReward reward = quest.getRewards().get(i);
            net.minecraft.nbt.NbtCompound rewNbt = new net.minecraft.nbt.NbtCompound();
            rewNbt.putString("type", reward.getType().getName());
            rewNbt.putInt("tier", reward.getTier());
            rewNbt.putInt("experience", reward.getExperience());
            rewardsNbt.put("reward_" + i, rewNbt);
        }
        nbt.put("rewards", rewardsNbt);
        nbt.putInt("rewards_count", quest.getRewards().size());
        
        // Также сохраняем первую награду в старом формате для совместимости
        if (!quest.getRewards().isEmpty()) {
            QuestReward firstReward = quest.getRewards().get(0);
            net.minecraft.nbt.NbtCompound rewardNbt = new net.minecraft.nbt.NbtCompound();
            rewardNbt.putString("type", firstReward.getType().getName());
            rewardNbt.putInt("tier", firstReward.getTier());
            rewardNbt.putInt("experience", firstReward.getExperience());
            nbt.put("reward", rewardNbt);
        }
    }
    
    /**
     * Получает специфичное для квеста название билета
     */
    public static Text getQuestSpecificName(ItemStack stack, Quest quest) {
        if (quest == null) {
            return Text.literal("Билет квеста");
        }
        
        // Получаем состояние квеста
        QuestTicketState state = getTicketState(stack);
        
        // Формируем базовое название
        String baseName = "Билет квеста: " + quest.getTitle();
        
        // Добавляем информацию о редкости (пока не используем)
        // String rarityText = getRarityDisplayName(quest.getRarity());
        
        // Добавляем информацию о классе, если не "any"
        String classInfo = "";
        if (!"any".equals(quest.getPlayerClass()) && !"human".equals(quest.getPlayerClass())) {
            String playerClass = quest.getPlayerClass();
            // Убираем префикс "origins:" если есть
            if (playerClass.startsWith("origins:")) {
                playerClass = playerClass.substring(8);
            }
            classInfo = " (" + QuestItem.getClassDisplayName(playerClass) + ")";
        }
        
        // Формируем итоговое название с учетом состояния
        String finalName = baseName + classInfo;
        
        // Применяем форматирование в зависимости от состояния
        Formatting nameFormatting = getNameFormatting(state, quest.getRarity());
        
        return Text.literal(finalName).formatted(nameFormatting);
    }
    
    /**
     * Обновляет отображаемое название билета квеста
     */
    public static void updateTicketDisplayName(ItemStack stack, Quest quest) {
        if (stack.isEmpty() || quest == null) return;
        
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        
        // Сохраняем кастомное название в NBT
        Text customName = getQuestSpecificName(stack, quest);
        nbt.putString("display_name", Text.Serializer.toJson(customName));
        
        // Устанавливаем кастомное название для отображения
        net.minecraft.nbt.NbtCompound displayNbt = nbt.getCompound("display");
        displayNbt.putString("Name", Text.Serializer.toJson(customName));
        nbt.put("display", displayNbt);
    }
    
    /**
     * Получает отображаемое название редкости
     */
    private static String getRarityDisplayName(Quest.QuestRarity rarity) {
        if (rarity == null) return "Неизвестный";
        
        return switch (rarity) {
            case COMMON -> "Обычный";
            case UNCOMMON -> "Необычный";
            case RARE -> "Редкий";
            case EPIC -> "Эпический";
        };
    }
    
    /**
     * Получает форматирование названия в зависимости от состояния и редкости
     */
    private static Formatting getNameFormatting(QuestTicketState state, Quest.QuestRarity rarity) {
        if (state == null) state = QuestTicketState.AVAILABLE;
        if (rarity == null) rarity = Quest.QuestRarity.COMMON;
        
        // Приоритет состояния над редкостью
        switch (state) {
            case COMPLETED:
                return Formatting.GOLD;
            case IN_PROGRESS:
                return Formatting.YELLOW;
            case ACCEPTED:
                return Formatting.GREEN;
            default:
                // Используем цвет редкости для доступных квестов
                return switch (rarity) {
                    case COMMON -> Formatting.WHITE;
                    case UNCOMMON -> Formatting.GREEN;
                    case RARE -> Formatting.BLUE;
                    case EPIC -> Formatting.LIGHT_PURPLE;
                };
        }
    }
    
    /**
     * Добавляет базовую информацию о целях для неактивных квестов
     */
    private static void addBasicObjectivesTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        if (quest == null) {
            tooltip.add(Text.literal("  • Ошибка: квест не найден").formatted(Formatting.RED));
            return;
        }
        
        try {
            // Для новой системы квестов используем getObjective() для одной цели
            QuestObjective objective = quest.getObjective();
            if (objective != null) {
                String actionText = switch (objective.getType()) {
                    case COLLECT -> "Собрать";
                    case KILL -> "Убить";
                    case CRAFT -> "Создать";
                    default -> "Выполнить";
                };
                
                tooltip.add(Text.literal("  • " + actionText + ": " + getItemDisplayName(objective.getTarget()) + " x" + objective.getAmount())
                    .formatted(Formatting.WHITE));
            } else {
                // Fallback к старой системе с множественными целями
                if (quest.getObjectives() != null && !quest.getObjectives().isEmpty()) {
                    for (QuestObjective obj : quest.getObjectives()) {
                        if (obj != null) {
                            String actionText = switch (obj.getType()) {
                                case COLLECT -> "Собрать";
                                case KILL -> "Убить";
                                case CRAFT -> "Создать";
                                default -> "Выполнить";
                            };
                            
                            tooltip.add(Text.literal("  • " + actionText + ": " + getItemDisplayName(obj.getTarget()) + " x" + obj.getAmount())
                                .formatted(Formatting.WHITE));
                        }
                    }
                } else {
                    tooltip.add(Text.literal("  • Нет доступных целей").formatted(Formatting.GRAY));
                }
            }
        } catch (Exception e) {
            tooltip.add(Text.literal("  • Ошибка отображения целей: " + e.getMessage()).formatted(Formatting.RED));
        }
    }
    
    /**
     * Добавляет информацию о наградах
     */
    private static void addRewardTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        for (QuestReward reward : quest.getRewards()) {
            switch (reward.getType()) {
                case EXPERIENCE:
                    tooltip.add(Text.literal("  • " + reward.getExperience() + " опыта профессии")
                        .formatted(Formatting.AQUA));
                    break;
                case SKILL_POINT_TOKEN:
                    tooltip.add(Text.literal("  • Токен очков навыков (уровень " + reward.getTier() + ")")
                        .formatted(Formatting.LIGHT_PURPLE));
                    break;
                case ITEM:
                    tooltip.add(Text.literal("  • Предметы")
                        .formatted(Formatting.WHITE));
                    break;
            }
        }
    }
    
    /**
     * Добавляет базовую информацию о времени
     */
    private static void addBasicTimeTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        if (quest.getTimeLimit() > 0) {
            if (quest.getTimeLimit() >= 60) {
                long hours = quest.getTimeLimit() / 60;
                long minutes = quest.getTimeLimit() % 60;
                tooltip.add(Text.literal("Лимит времени: " + hours + "ч " + minutes + "м")
                    .formatted(Formatting.WHITE));
            } else {
                tooltip.add(Text.literal("Лимит времени: " + quest.getTimeLimit() + " мин")
                    .formatted(Formatting.WHITE));
            }
        } else {
            tooltip.add(Text.literal("Без ограничения по времени")
                .formatted(Formatting.AQUA));
        }
    }
    

    
    /**
     * Проверяет, принят ли квест
     */
    public static boolean isAccepted(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return false;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return false;
        }
        
        QuestTicketState state = QuestTicketState.fromName(nbt.getString("quest_state"));
        return state.isActive();
    }
    
    /**
     * Отмечает квест как принятый
     */
    public static void markAsAccepted(ItemStack stack, long acceptTime) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString("quest_state", QuestTicketState.ACCEPTED.getName());
        nbt.putLong("accept_time", acceptTime);
    }
    
    /**
     * Обновляет прогресс квеста в билете
     */
    public static void updateProgress(ItemStack stack, QuestObjective objective) {
        if (stack.isEmpty() || !isQuestTicket(stack) || objective == null) {
            io.github.apace100.origins.Origins.LOGGER.warn("Попытка обновить прогресс с недопустимыми параметрами");
            return;
        }
        
        try {
            net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
            
            // Обновляем состояние на "в процессе"
            QuestTicketState currentState = QuestTicketState.fromName(nbt.getString("quest_state"));
            if (currentState == QuestTicketState.ACCEPTED) {
                nbt.putString("quest_state", QuestTicketState.IN_PROGRESS.getName());
            }
            
            // Обновляем прогресс цели (новая система)
            // Сначала пробуем новую систему с одной целью
            if (nbt.contains("objective")) {
                net.minecraft.nbt.NbtCompound objNbt = nbt.getCompound("objective");
                objNbt.putInt("progress", objective.getProgress());
                objNbt.putBoolean("completed", objective.isCompleted());
                io.github.apace100.origins.Origins.LOGGER.info("Обновлен прогресс цели (новая система): {} {}/{}", 
                    objective.getTarget(), objective.getProgress(), objective.getAmount());
            } else {
                // Fallback к старой системе с множественными целями
                net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
                int objectivesCount = nbt.getInt("objectives_count");
                
                boolean objectiveFound = false;
                for (int i = 0; i < objectivesCount; i++) {
                    net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
                    String objType = objNbt.getString("type");
                    String objTarget = objNbt.getString("target");
                    
                    if (objType.equals(objective.getType().getName()) && objTarget.equals(objective.getTarget())) {
                        objNbt.putInt("progress", objective.getProgress());
                        objNbt.putBoolean("completed", objective.isCompleted());
                        objectiveFound = true;
                        io.github.apace100.origins.Origins.LOGGER.info("Обновлен прогресс цели (старая система): {} {}/{}", 
                            objTarget, objective.getProgress(), objective.getAmount());
                        break;
                    }
                }
                
                if (!objectiveFound) {
                    io.github.apace100.origins.Origins.LOGGER.warn("Цель квеста не найдена для обновления: {} {}", 
                        objective.getType().getName(), objective.getTarget());
                }
            }
            
            // Проверяем, готов ли квест к завершению
            checkAndUpdateCompletionStatus(stack);
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при обновлении прогресса билета: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет, завершен ли квест в билете
     */
    public static boolean isQuestCompleted(ItemStack stack) {
        if (!isQuestTicket(stack) || !stack.hasNbt()) {
            return false;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        return nbt.getBoolean("completion_ready");
    }
    
    /**
     * Обновляет прогресс квеста в билете
     */
    public static boolean updateQuestProgress(ItemStack stack, String action, String target, int amount) {
        if (!isQuestTicket(stack) || !stack.hasNbt()) {
            io.github.apace100.origins.Origins.LOGGER.warn("Попытка обновить прогресс не-билета или билета без NBT");
            return false;
        }
        
        try {
            net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
            String questId = nbt.getString("quest_id");
            io.github.apace100.origins.Origins.LOGGER.info("Обновляем прогресс квеста {} для действия {} с целью {}", 
                questId, action, target);
            
            boolean progressUpdated = false;
            
            // Проверяем новую систему с одной целью
            if (nbt.contains("objective")) {
                net.minecraft.nbt.NbtCompound objective = nbt.getCompound("objective");
                
                String objectiveType = objective.getString("type");
                String objectiveTarget = objective.getString("target");
                int requiredAmount = objective.getInt("amount");
                int currentProgress = objective.getInt("progress");
                boolean completed = objective.getBoolean("completed");
                
                io.github.apace100.origins.Origins.LOGGER.info("Проверяем цель: type={}, target={}, required={}, current={}, completed={}", 
                    objectiveType, objectiveTarget, requiredAmount, currentProgress, completed);
                
                // Проверяем, подходит ли это действие для данной цели
                if (!completed && objectiveType.equals(action) && objectiveTarget.equals(target)) {
                    io.github.apace100.origins.Origins.LOGGER.info("Цель подходит! Обновляем прогресс с {} на {}", 
                        currentProgress, currentProgress + amount);
                    
                    int newProgress = Math.min(currentProgress + amount, requiredAmount);
                    objective.putInt("progress", newProgress);
                    
                    // Проверяем, завершена ли цель
                    if (newProgress >= requiredAmount) {
                        objective.putBoolean("completed", true);
                        io.github.apace100.origins.Origins.LOGGER.info("Цель завершена! ({}/{})", newProgress, requiredAmount);
                    }
                    
                    progressUpdated = true;
                    io.github.apace100.origins.Origins.LOGGER.info("Прогресс обновлен: {}/{}", newProgress, requiredAmount);
                }
            } else {
                // Fallback к старой системе с множественными целями
                net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
                int objectivesCount = nbt.getInt("objectives_count");
                
                // Проверяем каждую цель
                for (int i = 0; i < objectivesCount; i++) {
                    String objectiveKey = "objective_" + i;
                    if (objectivesNbt.contains(objectiveKey)) {
                        net.minecraft.nbt.NbtCompound objective = objectivesNbt.getCompound(objectiveKey);
                        
                        String objectiveType = objective.getString("type");
                        String objectiveTarget = objective.getString("target");
                        int requiredAmount = objective.getInt("amount");
                        int currentProgress = objective.getInt("progress");
                        boolean completed = objective.getBoolean("completed");
                        
                        io.github.apace100.origins.Origins.LOGGER.info("Проверяем цель {}: type={}, target={}, required={}, current={}, completed={}", 
                            i, objectiveType, objectiveTarget, requiredAmount, currentProgress, completed);
                        
                        // Проверяем, подходит ли это действие для данной цели
                        if (!completed && objectiveType.equals(action) && objectiveTarget.equals(target)) {
                            io.github.apace100.origins.Origins.LOGGER.info("Цель подходит! Обновляем прогресс с {} на {}", 
                                currentProgress, currentProgress + amount);
                            
                            int newProgress = Math.min(currentProgress + amount, requiredAmount);
                            objective.putInt("progress", newProgress);
                            
                            // Проверяем, завершена ли цель
                            if (newProgress >= requiredAmount) {
                                objective.putBoolean("completed", true);
                                io.github.apace100.origins.Origins.LOGGER.info("Цель {} завершена! ({}/{})", i, newProgress, requiredAmount);
                            }
                            
                            progressUpdated = true;
                            io.github.apace100.origins.Origins.LOGGER.info("Прогресс обновлен: {}/{}", newProgress, requiredAmount);
                        }
                    }
                }
            }
            
            if (progressUpdated) {
                // Проверяем, завершены ли все цели
                checkAndUpdateCompletionStatus(stack);
                
                // Обновляем отображаемое имя билета
                Quest quest = QuestItem.getQuestFromStack(stack);
                if (quest != null) {
                    updateTicketDisplayName(stack, quest);
                }
            }
            
            return progressUpdated;
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при обновлении прогресса квеста: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Проверяет и обновляет статус готовности к завершению
     */
    private static void checkAndUpdateCompletionStatus(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return;
        }
        
        try {
            net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
            
            boolean allCompleted = false;
            
            // Проверяем новую систему с одной целью
            if (nbt.contains("objective")) {
                net.minecraft.nbt.NbtCompound objNbt = nbt.getCompound("objective");
                allCompleted = objNbt.getBoolean("completed");
            } else {
                // Fallback к старой системе с множественными целями
                net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
                int objectivesCount = nbt.getInt("objectives_count");
                
                allCompleted = true;
                for (int i = 0; i < objectivesCount; i++) {
                    net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
                    if (!objNbt.getBoolean("completed")) {
                        allCompleted = false;
                        break;
                    }
                }
            }
            
            if (allCompleted) {
                nbt.putString("quest_state", QuestTicketState.COMPLETED.getName());
                nbt.putBoolean("completion_ready", true);
                io.github.apace100.origins.Origins.LOGGER.info("Квест готов к сдаче!");
            }
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при проверке статуса завершения: " + e.getMessage());
        }
    }
    
    /**
     * Проверяет, готов ли квест к завершению
     */
    public static boolean isReadyForCompletion(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return false;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return false;
        }
        
        return nbt.getBoolean("completion_ready");
    }
    
    /**
     * Получает состояние билета квеста
     */
    public static QuestTicketState getTicketState(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return QuestTicketState.AVAILABLE;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return QuestTicketState.AVAILABLE;
        }
        
        return QuestTicketState.fromName(nbt.getString("quest_state"));
    }
    
    /**
     * Получает время принятия квеста
     */
    public static long getAcceptTime(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return 0L;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return 0L;
        }
        
        return nbt.getLong("accept_time");
    }
    
    /**
     * Отмечает квест как проваленный
     */
    public static void markAsFailed(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString("quest_state", QuestTicketState.FAILED.getName());
        nbt.putBoolean("completion_ready", false);
        
        // Обновляем отображаемое название
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest != null) {
            updateTicketDisplayName(stack, quest);
        }
    }
    
    /**
     * Добавляет визуальные эффекты для готового к сдаче квеста
     */
    public static void addVisualCompletionEffect(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        net.minecraft.nbt.NbtCompound visualEffects = nbt.getCompound("visual_effects");
        
        visualEffects.putBoolean("glowing", true);
        visualEffects.putString("particle_effect", "completion");
        
        nbt.put("visual_effects", visualEffects);
    }
    
    /**
     * Проверяет, имеет ли билет визуальные эффекты
     */
    public static boolean hasVisualEffects(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return false;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return false;
        }
        
        net.minecraft.nbt.NbtCompound visualEffects = nbt.getCompound("visual_effects");
        return visualEffects.getBoolean("glowing");
    }
    
    /**
     * Обработка использования билета на блоке (для завершения квеста на доске)
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        BlockPos pos = context.getBlockPos();
        
        if (player == null || player.getWorld().isClient) {
            return ActionResult.PASS;
        }
        
        // Проверяем, что это доска объявлений
        BlockEntity blockEntity = player.getWorld().getBlockEntity(pos);
        if (!(blockEntity instanceof BountyBoardBlockEntity board)) {
            return ActionResult.PASS;
        }
        
        ItemStack stack = context.getStack();
        if (!isQuestTicket(stack)) {
            return ActionResult.PASS;
        }
        
        // Пытаемся завершить квест
        QuestTicketAcceptanceHandler handler = QuestTicketAcceptanceHandler.getInstance();
        boolean completed = handler.completeQuestAtBoard(player, stack, board);
        
        return completed ? ActionResult.SUCCESS : ActionResult.FAIL;
    }
}