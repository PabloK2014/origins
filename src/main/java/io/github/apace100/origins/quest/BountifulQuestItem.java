package io.github.apace100.origins.quest;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

/**
 * Предмет квеста, аналогичный BountyItem из Bountiful
 */
public class BountifulQuestItem extends Item {
    
    public BountifulQuestItem() {
        super(new Settings().maxCount(1).fireproof());
    }
    
    /**
     * Регистрирует предикаты модели для разных редкостей
     */
    @Environment(EnvType.CLIENT)
    public static void registerModelPredicates() {
        ModelPredicateProviderRegistry.register(
            QuestRegistry.BOUNTIFUL_QUEST_ITEM,
            new Identifier("origins", "rarity"),
            (stack, world, entity, seed) -> {
                BountifulQuestInfo info = BountifulQuestInfo.get(stack);
                return info.getRarity().ordinal() * 0.1f;
            }
        );
    }
    
    @Override
    public Text getName(ItemStack stack) {
        BountifulQuestInfo info = BountifulQuestInfo.get(stack);
        Quest.QuestRarity rarity = info.getRarity();
        
        // Создаем название квеста
        String rarityName = rarity.getName();
        String capitalizedRarity = rarityName.substring(0, 1).toUpperCase(Locale.ROOT) + 
                                 rarityName.substring(1);
        
        net.minecraft.text.MutableText questName = Text.translatable("origins.quest.name", capitalizedRarity)
            .formatted(rarity.getColor());
        
        // Добавляем жирность для эпических квестов
        if (rarity == Quest.QuestRarity.EPIC) {
            questName = questName.formatted(Formatting.BOLD);
        }
        
        // Добавляем профессию если она указана
        if (!info.getProfession().equals("any")) {
            questName = questName.append(" (")
                .append(Text.translatable("origins.profession." + info.getProfession()))
                .append(")");
        }
        
        return questName;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if (world != null && world.isClient) {
            BountifulQuestInfo info = BountifulQuestInfo.get(stack);
            BountifulQuestData data = BountifulQuestData.get(stack);
            
            // Добавляем кастомный tooltip вместо стандартного
            addCustomQuestTooltip(stack, tooltip, info, data);
        }
        
        // НЕ вызываем super.appendTooltip чтобы избежать стандартного "доступен"
    }
    
    /**
     * Добавляет кастомный tooltip для квеста
     */
    private void addCustomQuestTooltip(ItemStack stack, List<Text> tooltip, BountifulQuestInfo info, BountifulQuestData data) {
        // Проверяем совместимость класса игрока с квестом
        String playerClass = getCurrentPlayerClass();
        String questClass = info.getProfession();
        boolean isCompatible = isClassCompatible(playerClass, questClass);
        
        // Добавляем статус квеста
        if (isCompatible) {
            tooltip.add(Text.literal("Статус: Доступен для принятия").formatted(net.minecraft.util.Formatting.GREEN));
        } else {
            tooltip.add(Text.literal("Статус: Недоступен (класс: " + getClassDisplayName(questClass) + ")").formatted(net.minecraft.util.Formatting.RED));
        }
        
        // Добавляем информацию о профессии
        if (!questClass.equals("any")) {
            tooltip.add(Text.literal("Требуемый класс: " + getClassDisplayName(questClass))
                .formatted(net.minecraft.util.Formatting.GOLD));
        }
        
        // Добавляем цели квеста
        if (data != null && !data.getObjectives().isEmpty()) {
            tooltip.add(Text.literal("Цели:").formatted(net.minecraft.util.Formatting.YELLOW));
            for (BountifulQuestEntry objective : data.getObjectives()) {
                tooltip.add(Text.literal("• " + objective.getName() + " x" + objective.getAmount())
                    .formatted(net.minecraft.util.Formatting.WHITE));
            }
        }
        
        // Добавляем награды
        if (data != null && !data.getRewards().isEmpty()) {
            tooltip.add(Text.literal("Награды:").formatted(net.minecraft.util.Formatting.GREEN));
            for (BountifulQuestEntry reward : data.getRewards()) {
                tooltip.add(Text.literal("• " + reward.getName() + " x" + reward.getAmount())
                    .formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            }
        }
        
        // Добавляем инструкцию
        if (isCompatible) {
            tooltip.add(Text.literal("Кликните ПКМ для принятия квеста").formatted(net.minecraft.util.Formatting.AQUA));
        } else {
            tooltip.add(Text.literal("Недоступно для вашего класса").formatted(net.minecraft.util.Formatting.DARK_RED));
        }
    }
    
    /**
     * Получает текущий класс игрока
     */
    private String getCurrentPlayerClass() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            return io.github.apace100.origins.quest.QuestIntegration.getPlayerClass(client.player);
        }
        return "human";
    }
    
    /**
     * Проверяет совместимость классов
     */
    private boolean isClassCompatible(String playerClass, String questClass) {
        if (questClass == null || questClass.equals("any")) {
            return true;
        }
        
        // Нормализуем названия классов
        String normalizedPlayerClass = normalizeClassName(playerClass);
        String normalizedQuestClass = normalizeClassName(questClass);
        
        return normalizedPlayerClass.equals(normalizedQuestClass);
    }
    
    /**
     * Нормализует название класса
     */
    private String normalizeClassName(String className) {
        if (className == null) {
            return "human";
        }
        
        // Убираем префикс "origins:" если есть
        if (className.startsWith("origins:")) {
            className = className.substring(8);
        }
        
        return className.toLowerCase();
    }
    
    /**
     * Получает отображаемое название класса
     */
    private String getClassDisplayName(String className) {
        String normalized = normalizeClassName(className);
        return switch (normalized) {
            case "warrior" -> "Воин";
            case "miner" -> "Шахтер";
            case "blacksmith" -> "Кузнец";
            case "courier" -> "Курьер";
            case "brewer" -> "Пивовар";
            case "cook" -> "Повар";
            case "any" -> "Любой";
            default -> normalized;
        };
    }
    
    @Override
    public net.minecraft.util.TypedActionResult<ItemStack> use(net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity user, net.minecraft.util.Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (world.isClient) {
            return net.minecraft.util.TypedActionResult.pass(stack);
        }
        
        // Проверяем совместимость класса
        BountifulQuestInfo info = BountifulQuestInfo.get(stack);
        String playerClass = io.github.apace100.origins.quest.QuestIntegration.getPlayerClass(user);
        String questClass = info.getProfession();
        
        if (!isClassCompatible(playerClass, questClass)) {
            user.sendMessage(Text.literal("Этот квест недоступен для вашего класса!").formatted(net.minecraft.util.Formatting.RED), false);
            return net.minecraft.util.TypedActionResult.fail(stack);
        }
        
        // Пытаемся принять квест
        if (acceptBountifulQuest(user, stack)) {
            user.sendMessage(Text.literal("Квест принят!").formatted(net.minecraft.util.Formatting.GREEN), false);
            return net.minecraft.util.TypedActionResult.success(stack);
        } else {
            user.sendMessage(Text.literal("Не удалось принять квест!").formatted(net.minecraft.util.Formatting.RED), false);
            return net.minecraft.util.TypedActionResult.fail(stack);
        }
    }
    
    /**
     * Принимает Bountiful квест
     */
    private boolean acceptBountifulQuest(net.minecraft.entity.player.PlayerEntity player, ItemStack questStack) {
        try {
            BountifulQuestInfo info = BountifulQuestInfo.get(questStack);
            BountifulQuestData data = BountifulQuestData.get(questStack);
            
            // Создаем Quest объект из Bountiful данных
            Quest quest = convertBountifulToQuest(data, info);
            if (quest == null) {
                return false;
            }
            
            // Используем QuestTicketAcceptanceHandler для принятия квеста
            QuestTicketAcceptanceHandler acceptanceHandler = QuestTicketAcceptanceHandler.getInstance();
            return acceptanceHandler.acceptQuestFromBoard(player, quest, null);
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при принятии Bountiful квеста: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Конвертирует Bountiful квест в обычный Quest
     */
    private Quest convertBountifulToQuest(BountifulQuestData data, BountifulQuestInfo info) {
        try {
            // Генерируем уникальный ID
            String questId = "bountiful_" + System.currentTimeMillis();
            
            // Получаем первую цель (упрощенно)
            if (data.getObjectives().isEmpty()) {
                return null;
            }
            
            BountifulQuestEntry firstObjective = data.getObjectives().get(0);
            QuestObjective.ObjectiveType objectiveType = convertObjectiveType(firstObjective.getObjectiveType());
            QuestObjective objective = new QuestObjective(objectiveType, firstObjective.getContent(), firstObjective.getAmount());
            
            // Получаем первую награду (упрощенно)
            QuestReward reward;
            if (!data.getRewards().isEmpty()) {
                BountifulQuestEntry firstReward = data.getRewards().get(0);
                reward = new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, info.getRarity().ordinal() + 1, firstReward.getAmount() * 100);
            } else {
                reward = new QuestReward(QuestReward.RewardType.SKILL_POINT_TOKEN, 1, 500);
            }
            
            // Создаем квест
            String title = "Bountiful Quest (" + info.getRarity().getName() + ")";
            String description = "Квест из системы Bountiful";
            int timeLimit = 60; // 60 минут по умолчанию
            
            return new Quest(questId, info.getProfession(), info.getRarity().ordinal() + 1, title, description, objective, timeLimit, reward);
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при конвертации Bountiful квеста: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Конвертирует тип цели Bountiful в обычный тип
     */
    private QuestObjective.ObjectiveType convertObjectiveType(BountifulQuestObjectiveType bountifulType) {
        if (bountifulType == null) {
            return QuestObjective.ObjectiveType.COLLECT;
        }
        
        return switch (bountifulType.getName()) {
            case "collect" -> QuestObjective.ObjectiveType.COLLECT;
            case "kill" -> QuestObjective.ObjectiveType.KILL;
            case "craft" -> QuestObjective.ObjectiveType.CRAFT;
            default -> QuestObjective.ObjectiveType.COLLECT;
        };
    }
    
    /**
     * Создает новый квест-предмет
     */
    public static ItemStack createQuestItem(BountifulQuestData data, BountifulQuestInfo info) {
        ItemStack stack = new ItemStack(QuestRegistry.BOUNTIFUL_QUEST_ITEM);
        
        BountifulQuestData.set(stack, data);
        BountifulQuestInfo.set(stack, info);
        
        return stack;
    }
}