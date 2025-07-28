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
        super.appendTooltip(stack, world, tooltip, context);
        
        // Получаем квест из NBT данных
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest != null) {
            // Добавляем состояние квеста
            QuestTicketState state = getTicketState(stack);
            tooltip.add(Text.literal("Состояние: " + state.getDisplayName())
                .formatted(getStateFormatting(state)));
            
            // Используем упрощенный метод из QuestItem для базовой информации
            QuestItem.addQuestTooltip(stack, tooltip);
            
            // Добавляем специфичную для билета информацию
            if (state.isActive() && getAcceptTime(stack) > 0) {
                // Показываем прогресс для активных квестов
                tooltip.add(Text.literal(""));
                tooltip.add(Text.literal("Прогресс:").formatted(Formatting.YELLOW));
                addProgressTooltip(stack, tooltip, quest);
                
                // Показываем оставшееся время
                addTimeTooltip(stack, tooltip, quest);
            }
            
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
     * Получает форматирование для состояния квеста
     */
    private static Formatting getStateFormatting(QuestTicketState state) {
        return switch (state) {
            case AVAILABLE -> Formatting.WHITE;
            case ACCEPTED -> Formatting.YELLOW;
            case IN_PROGRESS -> Formatting.AQUA;
            case COMPLETED -> Formatting.GREEN;
            case FINISHED -> Formatting.GRAY;
        };
    }
    
    /**
     * Добавляет информацию о прогрессе в tooltip
     */
    private static void addProgressTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        net.minecraft.nbt.NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return;
        }
        
        net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
        int objectivesCount = nbt.getInt("objectives_count");
        
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
    }
    
    /**
     * Добавляет информацию о времени в tooltip
     */
    private static void addTimeTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        long acceptTime = getAcceptTime(stack);
        if (acceptTime > 0 && quest.getTimeLimit() > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsedMinutes = (currentTime - acceptTime) / (1000 * 60);
            long remainingMinutes = Math.max(0, quest.getTimeLimit() - elapsedMinutes);
            
            // Улучшенное отображение времени
            String timeText;
            Formatting timeColor;
            
            if (remainingMinutes <= 0) {
                timeText = "ВРЕМЯ ИСТЕКЛО!";
                timeColor = Formatting.DARK_RED;
            } else if (remainingMinutes <= 5) {
                timeText = "Осталось времени: " + remainingMinutes + " мин (СРОЧНО!)";
                timeColor = Formatting.RED;
            } else if (remainingMinutes <= 15) {
                timeText = "Осталось времени: " + remainingMinutes + " мин";
                timeColor = Formatting.YELLOW;
            } else if (remainingMinutes <= 60) {
                timeText = "Осталось времени: " + remainingMinutes + " мин";
                timeColor = Formatting.GREEN;
            } else {
                long hours = remainingMinutes / 60;
                long minutes = remainingMinutes % 60;
                timeText = "Осталось времени: " + hours + "ч " + minutes + "м";
                timeColor = Formatting.GREEN;
            }
            
            tooltip.add(Text.literal(timeText).formatted(timeColor));
        } else if (quest.getTimeLimit() > 0) {
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
        
        ItemStack stack = new ItemStack(ticketItem);
        
        // Сохраняем данные квеста в NBT
        saveQuestToStack(stack, quest);
        
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
        
        // Сохраняем цели
        net.minecraft.nbt.NbtCompound objectivesNbt = new net.minecraft.nbt.NbtCompound();
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective objective = quest.getObjectives().get(i);
            net.minecraft.nbt.NbtCompound objNbt = new net.minecraft.nbt.NbtCompound();
            objNbt.putString("type", objective.getType().getName());
            objNbt.putString("target", objective.getTarget());
            objNbt.putInt("amount", objective.getAmount());
            objNbt.putInt("progress", objective.getProgress());
            objNbt.putBoolean("completed", objective.isCompleted());
            objectivesNbt.put("objective_" + i, objNbt);
        }
        nbt.put("objectives", objectivesNbt);
        nbt.putInt("objectives_count", quest.getObjectives().size());
        
        // Сохраняем награды
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
        
        // Добавляем информацию о редкости
        String rarityText = getRarityDisplayName(quest.getRarity());
        
        // Добавляем информацию о классе, если не "any"
        String classInfo = "";
        if (!"any".equals(quest.getPlayerClass()) && !"human".equals(quest.getPlayerClass())) {
            classInfo = " (" + getClassDisplayName(quest.getPlayerClass()) + ")";
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
        return switch (rarity) {
            case COMMON -> "Обычный";
            case UNCOMMON -> "Необычный";
            case RARE -> "Редкий";
            case EPIC -> "Эпический";
            default -> "Неизвестный";
        };
    }
    
    /**
     * Получает форматирование названия в зависимости от состояния и редкости
     */
    private static Formatting getNameFormatting(QuestTicketState state, Quest.QuestRarity rarity) {
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
                    default -> Formatting.GRAY;
                };
        }
    }
    
    /**
     * Добавляет базовую информацию о целях для неактивных квестов
     */
    private static void addBasicObjectivesTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        for (QuestObjective objective : quest.getObjectives()) {
            String actionText = switch (objective.getType()) {
                case COLLECT -> "Собрать";
                case KILL -> "Убить";
                case CRAFT -> "Создать";
                default -> "Выполнить";
            };
            
            tooltip.add(Text.literal("  • " + actionText + ": " + getItemDisplayName(objective.getTarget()) + " x" + objective.getAmount())
                .formatted(Formatting.WHITE));
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
     * Добавляет информацию о прогрессе для активных квестов
     */
    private static void addProgressTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        if (quest.getObjective() != null) {
            QuestObjective objective = quest.getObjective();
            int progress = objective.getProgress();
            int total = objective.getAmount();
            
            String progressText = String.format("  • %s: %d/%d", 
                getItemDisplayName(objective.getTarget()), progress, total);
            
            Formatting color = progress >= total ? Formatting.GREEN : 
                              progress > 0 ? Formatting.YELLOW : Formatting.WHITE;
            
            tooltip.add(Text.literal(progressText).formatted(color));
        }
    }
    
    /**
     * Добавляет информацию о времени для активных квестов
     */
    private static void addTimeTooltip(ItemStack stack, List<Text> tooltip, Quest quest) {
        long acceptTime = getAcceptTime(stack);
        if (acceptTime > 0 && quest.getTimeLimit() > 0) {
            int remainingTime = quest.getRemainingTime(acceptTime);
            
            if (remainingTime <= 0) {
                tooltip.add(Text.literal("Время истекло!").formatted(Formatting.RED));
            } else if (remainingTime < 5) {
                tooltip.add(Text.literal("Осталось: " + remainingTime + " мин").formatted(Formatting.RED));
            } else if (remainingTime < 15) {
                tooltip.add(Text.literal("Осталось: " + remainingTime + " мин").formatted(Formatting.YELLOW));
            } else {
                tooltip.add(Text.literal("Осталось: " + remainingTime + " мин").formatted(Formatting.GREEN));
            }
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
            return;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        
        // Обновляем состояние на "в процессе"
        if (QuestTicketState.fromName(nbt.getString("quest_state")) == QuestTicketState.ACCEPTED) {
            nbt.putString("quest_state", QuestTicketState.IN_PROGRESS.getName());
        }
        
        // Обновляем прогресс цели
        net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
        int objectivesCount = nbt.getInt("objectives_count");
        
        for (int i = 0; i < objectivesCount; i++) {
            net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
            if (objNbt.getString("type").equals(objective.getType().getName()) &&
                objNbt.getString("target").equals(objective.getTarget())) {
                
                objNbt.putInt("progress", objective.getProgress());
                objNbt.putBoolean("completed", objective.isCompleted());
                break;
            }
        }
        
        // Проверяем, готов ли квест к завершению
        checkAndUpdateCompletionStatus(stack);
    }
    
    /**
     * Проверяет и обновляет статус готовности к завершению
     */
    private static void checkAndUpdateCompletionStatus(ItemStack stack) {
        if (stack.isEmpty() || !isQuestTicket(stack)) {
            return;
        }
        
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
        int objectivesCount = nbt.getInt("objectives_count");
        
        boolean allCompleted = true;
        for (int i = 0; i < objectivesCount; i++) {
            net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
            if (!objNbt.getBoolean("completed")) {
                allCompleted = false;
                break;
            }
        }
        
        if (allCompleted) {
            nbt.putString("quest_state", QuestTicketState.COMPLETED.getName());
            nbt.putBoolean("completion_ready", true);
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