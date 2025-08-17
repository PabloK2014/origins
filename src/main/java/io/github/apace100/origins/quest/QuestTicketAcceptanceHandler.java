package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/**
 * Обработчик принятия квестов через билеты.
 * Управляет логикой принятия, завершения и отмены квестов.
 */
public class QuestTicketAcceptanceHandler {
    private static QuestTicketAcceptanceHandler instance;
    
    private QuestTicketAcceptanceHandler() {}
    
    public static QuestTicketAcceptanceHandler getInstance() {
        if (instance == null) {
            instance = new QuestTicketAcceptanceHandler();
        }
        return instance;
    }
    
    /**
     * Принимает квест через ЛКМ по билету на доске объявлений с улучшенной обработкой ошибок
     */
    public boolean acceptQuestFromBoard(PlayerEntity player, Quest quest, BountyBoardBlockEntity board) {

        
        // Валидация входных параметров
        if (player == null) {
            QuestErrorRecovery.handleQuestSystemError(
                new IllegalArgumentException("Player is null"), 
                "acceptQuestFromBoard", 
                null
            );
            return false;
        }
        
        if (quest == null) {
            QuestErrorRecovery.handleQuestAcceptanceFailure(
                QuestAcceptanceError.QUEST_INVALID, 
                null, 
                player
            );
            return false;
        }
        
        // Валидация квеста
        if (!QuestErrorRecovery.validateAndRepairQuest(quest)) {
            QuestErrorRecovery.handleQuestAcceptanceFailure(
                QuestAcceptanceError.QUEST_INVALID, 
                quest, 
                player
            );
            return false;
        }
        
        // Проверка возможности принятия квеста с улучшенной обработкой
        if (!canAcceptQuest(player, quest)) {
            Origins.LOGGER.warn("Игрок {} не может принять квест {}", player.getName().getString(), quest.getId());
            return false;
        }
        
        try {
                        // Создаем билет квеста
            ItemStack questTicket = QuestTicketItem.createQuestTicket(quest);
            if (questTicket.isEmpty()) {
                Origins.LOGGER.error("Не удалось создать билет для квеста: {}", quest.getId());
                QuestErrorRecovery.handleQuestAcceptanceFailure(
                    QuestAcceptanceError.UNKNOWN_ERROR, 
                    quest, 
                    player
                );
                return false;
            }
                        
                        // Добавляем билет в инвентарь игрока
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            if (!inventoryManager.addQuestTicketToInventory(player, quest)) {
                Origins.LOGGER.warn("Не удалось добавить билет в инвентарь");
                sendErrorMessage(player, "Не удалось добавить билет в инвентарь!");
                return false;
            }
            
                        inventoryManager.debugInventory(player);
            
            // Помечаем билет как принятый
            ItemStack addedTicket = inventoryManager.getActiveQuestTicket(player, quest.getId());
            if (!addedTicket.isEmpty()) {
                QuestTicketItem.markAsAccepted(addedTicket, System.currentTimeMillis());
                            } else {
                Origins.LOGGER.warn("Не удалось найти добавленный билет квеста {}", quest.getId());
            }
            
                        // Регистрируем квест как принятый в QuestManager
            QuestManager questManager = QuestManager.getInstance();
            questManager.startQuest(player, quest);
                        
                        // Удаляем квест с доски объявлений
            if (board != null) {
                board.removeQuest(quest);
                            }
            
            // Уведомляем игрока об успехе
            if (player instanceof ServerPlayerEntity serverPlayer) {
                int activeCount = questManager.getActiveQuestCount(player);
                int maxQuests = questManager.getMaxActiveQuests();
                
                sendSuccessMessage(player, "Квест \"" + quest.getTitle() + "\" принят!");
                sendInfoMessage(player, "Активных квестов: " + activeCount + "/" + maxQuests);
                            }
            
                        return true;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принятии квеста {}: {}", quest.getId(), e.getMessage());
            QuestErrorRecovery.handleQuestSystemError(e, "acceptQuestFromBoard", player);
            return false;
        }
    }
    
    /**
     * Определяет тип ошибки при попытке принятия квеста
     */
    private QuestAcceptanceError getAcceptanceError(PlayerEntity player, Quest quest) {
        try {
            // Проверяем, есть ли уже этот конкретный квест
            QuestManager questManager = QuestManager.getInstance();
            if (questManager.hasActiveQuest(player, quest.getId())) {
                return QuestAcceptanceError.ALREADY_HAS_QUEST;
            }
            
            // Проверяем соответствие класса игрока
            String playerClass = getPlayerOriginClass(player);
            String questClass = quest.getPlayerClass();
            
            if (!QuestUtils.isClassCompatible(playerClass, questClass)) {
                return QuestAcceptanceError.PROFESSION_MISMATCH;
            }
            
            // Проверяем лимит активных квестов
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            if (inventoryManager.hasReachedQuestLimit(player)) {
                return QuestAcceptanceError.QUEST_LIMIT_REACHED;
            }
            
            // Проверяем, есть ли место в инвентаре
            if (!hasInventorySpace(player)) {
                return QuestAcceptanceError.INVENTORY_FULL;
            }
            
            // Проверяем уровень игрока
            if (!checkPlayerLevel(player, quest)) {
                return QuestAcceptanceError.PLAYER_LEVEL_TOO_LOW;
            }
            
            return null; // Нет ошибок
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при определении типа ошибки принятия квеста: {}", e.getMessage());
            return QuestAcceptanceError.UNKNOWN_ERROR;
        }
    }
    
    /**
     * Завершает квест через Shift+ПКМ билетом по доске объявлений
     */
    public boolean completeQuestAtBoard(PlayerEntity player, ItemStack ticketStack, BountyBoardBlockEntity board) {
        Origins.LOGGER.info("Попытка завершения квеста через доску объявлений: player={}, ticket={}", 
            player != null ? player.getName().getString() : "null",
            !ticketStack.isEmpty() ? "present" : "empty");
        
        if (ticketStack.isEmpty() || !QuestTicketItem.isQuestTicket(ticketStack)) {
            Origins.LOGGER.warn("Неверный билет квеста");
            sendErrorMessage(player, "Неверный билет квеста!");
            return false;
        }
        
        try {
            // Получаем квест из билета
            Quest quest = QuestItem.getQuestFromStack(ticketStack);
            if (quest == null) {
                Origins.LOGGER.error("Не удалось получить квест из билета");
                sendErrorMessage(player, "Поврежденный билет квеста!");
                return false;
            }
            
                        
            // Проверяем готовность квеста к завершению через билет
            QuestTicketState ticketState = QuestTicketItem.getTicketState(ticketStack);
            boolean isReady = QuestTicketItem.isReadyForCompletion(ticketStack);
            
                        
            if (!isReady) {
                                sendErrorMessage(player, "Квест еще не выполнен! Проверьте все условия.");
                return false;
            }
            
            // Проверяем, не истек ли квест по времени
            if (isQuestExpired(ticketStack, quest)) {
                Origins.LOGGER.warn("Квест {} истек по времени", quest.getId());
                sendErrorMessage(player, "Время выполнения квеста истекло!");
                return false;
            }
            
                        
            // Выдаем награды
            giveQuestRewards(player, quest);
            
            // Завершаем квест в QuestManager
            QuestManager questManager = QuestManager.getInstance();
            questManager.completeQuest(player, quest);
            
            // Удаляем билет из инвентаря (уменьшаем стак на 1)
            ticketStack.decrement(1);
            
            // Уведомляем игрока об успешном завершении
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                    Text.literal("✓ Квест \"" + quest.getTitle() + "\" завершен!")
                        .formatted(Formatting.GOLD), 
                    false
                );
                
                // Показываем полученные награды
                showRewardDetails(serverPlayer, quest);
            }
            
                        return true;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при завершении квеста: {}", e.getMessage());
            e.printStackTrace();
            sendErrorMessage(player, "Произошла ошибка при завершении квеста");
            return false;
        }
    }
    
    /**
     * Отменяет квест через выбрасывание билета
     */
    public void cancelQuestByDroppingTicket(PlayerEntity player, ItemStack ticketStack) {
        if (ticketStack.isEmpty() || !QuestTicketItem.isQuestTicket(ticketStack)) {
            return;
        }
        
        try {
            // Получаем квест из билета
            Quest quest = QuestItem.getQuestFromStack(ticketStack);
            if (quest == null) {
                return;
            }
            
            // Отменяем квест в QuestManager
            QuestManager questManager = QuestManager.getInstance();
            questManager.cancelQuest(player, quest.getId());
            
            // Уведомляем игрока
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                    Text.literal("Квест \"" + quest.getTitle() + "\" отменен.")
                        .formatted(Formatting.YELLOW), 
                    false
                );
            }
            
                        
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при отмене квеста: {}", e.getMessage());
        }
    }
    
    /**
     * Проверяет возможность принятия квеста с улучшенной валидацией
     */
    public boolean canAcceptQuest(PlayerEntity player, Quest quest) {
        Origins.LOGGER.info("QuestTicketAcceptanceHandler.canAcceptQuest: player={}, quest={}", 
            player != null ? player.getName().getString() : "null",
            quest != null ? quest.getTitle() : "null");
        
        // Базовая проверка на null
        if (player == null) {
            Origins.LOGGER.warn("Player is null");
            return false;
        }
        
        if (quest == null) {
            Origins.LOGGER.warn("Quest is null");
            return false;
        }
        
        try {
            // Проверяем валидность квеста
            if (!isQuestValid(quest)) {
                Origins.LOGGER.warn("Quest {} is not valid", quest.getId());
                return false;
            }
            
            // Проверяем лимит активных квестов
                        QuestManager questManager = QuestManager.getInstance();
            
            if (!questManager.canAcceptAdditionalQuest(player)) {
                int currentCount = questManager.getActiveQuestCount(player);
                int maxQuests = questManager.getMaxActiveQuests();
                Origins.LOGGER.warn("Игрок достиг лимита квестов: {}/{}", currentCount, maxQuests);
                sendErrorMessage(player, String.format("Достигнут лимит активных квестов! (%d/%d)", currentCount, maxQuests));
                return false;
            }
            
            // Проверяем, нет ли уже этого конкретного квеста (проверяем и в QuestManager, и в инвентаре)
            boolean hasInManager = questManager.hasSpecificActiveQuest(player, quest.getId());
            
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            boolean hasInInventory = inventoryManager.hasQuestTicket(player, quest.getId());
            
            if (hasInManager || hasInInventory) {
                Origins.LOGGER.warn("У игрока уже есть этот квест: {} (в менеджере: {}, в инвентаре: {})", 
                    quest.getId(), hasInManager, hasInInventory);
                
                // Если есть рассинхронизация, попробуем исправить
                if (hasInManager != hasInInventory) {
                    Origins.LOGGER.warn("Обнаружена рассинхронизация квестов для игрока {}", player.getName().getString());
                    
                    if (hasInManager && !hasInInventory) {
                        // Есть в менеджере, но нет в инвентаре - удаляем из менеджера
                        questManager.cancelQuest(player, quest.getId());
                                                return canAcceptQuest(player, quest); // Повторная проверка
                    } else if (!hasInManager && hasInInventory) {
                        // Есть в инвентаре, но нет в менеджере - удаляем из инвентаря
                        inventoryManager.removeQuestTicketFromInventory(player, quest.getId());
                                                return canAcceptQuest(player, quest); // Повторная проверка
                    }
                }
                
                sendErrorMessage(player, "У вас уже есть этот квест!");
                return false;
            }
            
                        
            // Проверяем соответствие класса игрока с улучшенной логикой
                        String playerClass = getPlayerOriginClass(player);
            String questClass = quest.getPlayerClass();
            
                        
            if (!QuestUtils.isClassCompatible(playerClass, questClass)) {
                Origins.LOGGER.warn("Класс игрока {} не подходит для квеста класса {}", playerClass, questClass);
                sendErrorMessage(player, "Этот квест предназначен для класса: " + QuestUtils.getLocalizedClassName(questClass));
                return false;
            }
                        
            // Проверяем, есть ли место в инвентаре
                        if (!hasInventorySpace(player)) {
                Origins.LOGGER.warn("Нет места в инвентаре");
                sendErrorMessage(player, "Нет места в инвентаре для билета квеста! Освободите место и попробуйте снова.");
                return false;
            }
                        
            // Проверяем уровень игрока (если требуется)
            if (!checkPlayerLevel(player, quest)) {
                Origins.LOGGER.warn("Уровень игрока недостаточен для квеста");
                sendErrorMessage(player, "Недостаточный уровень для этого квеста!");
                return false;
            }
            
                        return true;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке возможности принятия квеста: {}", e.getMessage());
            e.printStackTrace();
            sendErrorMessage(player, "Произошла ошибка при проверке квеста");
            return false;
        }
    }
    
    /**
     * Проверяет валидность квеста
     */
    private boolean isQuestValid(Quest quest) {
        if (quest.getId() == null || quest.getId().isEmpty()) {
            Origins.LOGGER.warn("Quest has invalid ID");
            return false;
        }
        
        if (quest.getTitle() == null || quest.getTitle().isEmpty()) {
            Origins.LOGGER.warn("Quest has invalid title");
            return false;
        }
        
        if (quest.getPlayerClass() == null || quest.getPlayerClass().isEmpty()) {
            Origins.LOGGER.warn("Quest has invalid player class");
            return false;
        }
        
        if (quest.getObjective() == null) {
            Origins.LOGGER.warn("Quest has no objective");
            return false;
        }
        
        if (quest.getReward() == null) {
            Origins.LOGGER.warn("Quest has no reward");
            return false;
        }
        
        return true;
    }
    
    /**
     * Получает класс игрока с fallback поддержкой
     */
    private String getPlayerOriginClass(PlayerEntity player) {
        try {
            String playerClass = QuestIntegration.getPlayerClass(player);
            if (playerClass != null && !playerClass.isEmpty()) {
                return playerClass;
            }
        } catch (Exception e) {
            Origins.LOGGER.warn("Ошибка при получении класса игрока: {}", e.getMessage());
        }
        
        // Fallback к "human" если не удалось определить класс
        return "human";
    }
    

    

    
    /**
     * Проверяет наличие места в инвентаре
     */
    private boolean hasInventorySpace(PlayerEntity player) {
        try {
            int emptySlot = player.getInventory().getEmptySlot();
            return emptySlot != -1;
        } catch (Exception e) {
            Origins.LOGGER.warn("Ошибка при проверке места в инвентаре: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Проверяет уровень игрока для квеста
     */
    private boolean checkPlayerLevel(PlayerEntity player, Quest quest) {
        try {
            // Пока что всегда возвращаем true, можно добавить логику уровней позже
            return true;
        } catch (Exception e) {
            Origins.LOGGER.warn("Ошибка при проверке уровня игрока: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Отправляет сообщение об ошибке игроку
     */
    private void sendErrorMessage(PlayerEntity player, String message) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(
                Text.literal("❌ " + message).formatted(Formatting.RED), 
                false
            );
        }
    }
    
    /**
     * Отправляет информационное сообщение игроку
     */
    private void sendInfoMessage(PlayerEntity player, String message) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(
                Text.literal("ℹ " + message).formatted(Formatting.YELLOW), 
                false
            );
        }
    }
    
    /**
     * Отправляет сообщение об успехе игроку
     */
    private void sendSuccessMessage(PlayerEntity player, String message) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(
                Text.literal("✓ " + message).formatted(Formatting.GREEN), 
                false
            );
        }
    }
    
    /**
     * Показывает подробную информацию о состоянии квестов игрока
     */
    public void showQuestStatus(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        QuestManager questManager = QuestManager.getInstance();
        int activeCount = questManager.getActiveQuestCount(player);
        int maxQuests = questManager.getMaxActiveQuests();
        int availableSlots = questManager.getAvailableQuestSlots(player);
        
        serverPlayer.sendMessage(
            Text.literal("=== Состояние квестов ===").formatted(Formatting.GOLD), 
            false
        );
        
        serverPlayer.sendMessage(
            Text.literal("Активных квестов: " + activeCount + "/" + maxQuests).formatted(Formatting.AQUA), 
            false
        );
        
        serverPlayer.sendMessage(
            Text.literal("Доступно слотов: " + availableSlots).formatted(Formatting.GREEN), 
            false
        );
        
        if (activeCount > 0) {
            serverPlayer.sendMessage(
                Text.literal("Активные квесты:").formatted(Formatting.YELLOW), 
                false
            );
            
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            List<ItemStack> questTickets = inventoryManager.findQuestTickets(player);
            
            for (ItemStack ticket : questTickets) {
                Quest quest = QuestItem.getQuestFromStack(ticket);
                if (quest != null) {
                    QuestTicketState state = QuestTicketItem.getTicketState(ticket);
                    Formatting stateColor = getStateFormatting(state);
                    
                    serverPlayer.sendMessage(
                        Text.literal("  • " + quest.getTitle() + " (" + state.getDisplayName() + ")")
                            .formatted(stateColor), 
                        false
                    );
                }
            }
        }
    }
    

    
    /**
     * Проверяет готовность квеста к завершению
     */
    public boolean isQuestReadyForCompletion(PlayerEntity player, Quest quest) {
        if (player == null || quest == null) {
            return false;
        }
        
        try {
            // Получаем активный квест игрока
            QuestManager questManager = QuestManager.getInstance();
            ActiveQuest activeQuest = questManager.getActiveQuest(player, quest.getId());
            
            if (activeQuest == null) {
                Origins.LOGGER.warn("Активный квест {} не найден для игрока {}", quest.getId(), player.getName().getString());
                return false;
            }
            
            // Проверяем, не истек ли квест
            if (activeQuest.isExpired()) {
                Origins.LOGGER.warn("Квест {} истек по времени", quest.getId());
                return false;
            }
            
            // Проверяем выполнение всех целей через билет квеста
            return validateQuestCompletionFromTicket(player, quest);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке готовности квеста к завершению: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Валидирует завершение квеста на основе данных в билете
     */
    private boolean validateQuestCompletionFromTicket(PlayerEntity player, Quest quest) {
        // Находим билет квеста в инвентаре
        QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
        ItemStack questTicket = inventoryManager.getActiveQuestTicket(player, quest.getId());
        
        if (questTicket.isEmpty()) {
            Origins.LOGGER.warn("Билет квеста {} не найден в инвентаре игрока {}", quest.getId(), player.getName().getString());
            return false;
        }
        
        // Проверяем состояние билета
        QuestTicketState state = QuestTicketItem.getTicketState(questTicket);
        if (state != QuestTicketState.COMPLETED) {
                        return false;
        }
        
        // Проверяем все цели в билете
        return validateAllObjectivesCompleted(questTicket);
    }
    
    /**
     * Проверяет, что все цели квеста выполнены
     */
    private boolean validateAllObjectivesCompleted(ItemStack questTicket) {
        net.minecraft.nbt.NbtCompound nbt = questTicket.getNbt();
        if (nbt == null) {
            return false;
        }
        
        net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
        int objectivesCount = nbt.getInt("objectives_count");
        
        for (int i = 0; i < objectivesCount; i++) {
            net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
            
            boolean completed = objNbt.getBoolean("completed");
            int progress = objNbt.getInt("progress");
            int amount = objNbt.getInt("amount");
            
            // Проверяем, что цель действительно выполнена
            if (!completed || progress < amount) {
                Origins.LOGGER.info("Цель {} не выполнена: completed={}, progress={}/{}", 
                    i, completed, progress, amount);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Обновляет прогресс квеста и проверяет готовность к завершению
     */
    public void updateQuestProgress(PlayerEntity player, String questId, String objectiveType, String target, int progressDelta) {
        if (player == null || questId == null) {
            return;
        }
        
        try {
            // Находим билет квеста
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            ItemStack questTicket = inventoryManager.getActiveQuestTicket(player, questId);
            
            if (questTicket.isEmpty()) {
                return;
            }
            
            // Обновляем прогресс в билете
            updateTicketProgress(questTicket, objectiveType, target, progressDelta);
            
            // Проверяем, готов ли квест к завершению
            checkAndUpdateQuestCompletion(questTicket);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обновлении прогресса квеста: {}", e.getMessage());
        }
    }
    
    /**
     * Обновляет прогресс в билете квеста
     */
    private void updateTicketProgress(ItemStack questTicket, String objectiveType, String target, int progressDelta) {
        net.minecraft.nbt.NbtCompound nbt = questTicket.getOrCreateNbt();
        net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
        int objectivesCount = nbt.getInt("objectives_count");
        
        for (int i = 0; i < objectivesCount; i++) {
            net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
            
            if (objNbt.getString("type").equals(objectiveType) && 
                objNbt.getString("target").equals(target)) {
                
                int currentProgress = objNbt.getInt("progress");
                int amount = objNbt.getInt("amount");
                int newProgress = Math.min(currentProgress + progressDelta, amount);
                
                objNbt.putInt("progress", newProgress);
                objNbt.putBoolean("completed", newProgress >= amount);
                
                                break;
            }
        }
        
        // Обновляем состояние квеста
        QuestTicketState currentState = QuestTicketItem.getTicketState(questTicket);
        if (currentState == QuestTicketState.ACCEPTED) {
            nbt.putString("quest_state", QuestTicketState.IN_PROGRESS.getName());
        }
    }
    
    /**
     * Проверяет и обновляет статус завершения квеста
     */
    private void checkAndUpdateQuestCompletion(ItemStack questTicket) {
        if (validateAllObjectivesCompleted(questTicket)) {
            net.minecraft.nbt.NbtCompound nbt = questTicket.getOrCreateNbt();
            nbt.putString("quest_state", QuestTicketState.COMPLETED.getName());
            nbt.putBoolean("completion_ready", true);
            
            // Добавляем визуальные эффекты
            QuestTicketItem.addVisualCompletionEffect(questTicket);
            
                    }
    }
    
    /**
     * Выдает награды за квест
     */
    private void giveQuestRewards(PlayerEntity player, Quest quest) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        try {
            for (QuestReward reward : quest.getRewards()) {
                switch (reward.getType()) {
                    case EXPERIENCE:
                        // Добавляем опыт профессии
                        io.github.apace100.origins.profession.ProfessionComponent professionComponent = 
                            io.github.apace100.origins.profession.ProfessionComponent.KEY.get(serverPlayer);
                        professionComponent.addExperience(reward.getExperience());
                        break;
                        
                    case SKILL_POINT_TOKEN:
                        // Добавляем токены очков навыков
                        reward.giveReward(serverPlayer);
                        break;
                        
                    case ITEM:
                        // Добавляем предметы (если будет реализовано)
                                                break;
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выдаче наград за квест: {}", e.getMessage());
        }
    }
    
    /**
     * Показывает детали прогресса квеста
     */
    private void showQuestProgressDetails(PlayerEntity player, ItemStack ticketStack, Quest quest) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        serverPlayer.sendMessage(
            Text.literal("Квест еще не выполнен! Проверьте все условия:")
                .formatted(Formatting.YELLOW), 
            false
        );
        
        net.minecraft.nbt.NbtCompound nbt = ticketStack.getNbt();
        if (nbt != null) {
            net.minecraft.nbt.NbtCompound objectivesNbt = nbt.getCompound("objectives");
            int objectivesCount = nbt.getInt("objectives_count");
            
            for (int i = 0; i < objectivesCount; i++) {
                net.minecraft.nbt.NbtCompound objNbt = objectivesNbt.getCompound("objective_" + i);
                
                String type = objNbt.getString("type");
                String target = objNbt.getString("target");
                int amount = objNbt.getInt("amount");
                int progress = objNbt.getInt("progress");
                boolean completed = objNbt.getBoolean("completed");
                
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
                
                String statusIcon = completed ? "✓" : "✗";
                Formatting statusColor = completed ? Formatting.GREEN : Formatting.RED;
                
                serverPlayer.sendMessage(
                    Text.literal("  " + statusIcon + " " + actionText + ": " + target + " (" + progress + "/" + amount + ")")
                        .formatted(statusColor), 
                    false
                );
            }
        }
    }
    
    /**
     * Показывает детали полученных наград
     */
    private void showRewardDetails(ServerPlayerEntity player, Quest quest) {
        player.sendMessage(
            Text.literal("Получены награды:")
                .formatted(Formatting.GREEN), 
            false
        );
        
        for (QuestReward reward : quest.getRewards()) {
            switch (reward.getType()) {
                case EXPERIENCE:
                    player.sendMessage(
                        Text.literal("  +" + reward.getExperience() + " опыта профессии")
                            .formatted(Formatting.AQUA), 
                        false
                    );
                    break;
                    
                case SKILL_POINT_TOKEN:
                    player.sendMessage(
                        Text.literal("  +1 токен очков навыков (уровень " + reward.getTier() + ")")
                            .formatted(Formatting.LIGHT_PURPLE), 
                        false
                    );
                    break;
                    
                case ITEM:
                    player.sendMessage(
                        Text.literal("  +предметы (в разработке)")
                            .formatted(Formatting.GRAY), 
                        false
                    );
                    break;
            }
        }
    }
    
    /**
     * Проверяет, истек ли квест по времени
     */
    private boolean isQuestExpired(ItemStack ticketStack, Quest quest) {
        if (quest.getTimeLimit() <= 0) {
            return false; // Квест без ограничения по времени
        }
        
        long acceptTime = QuestTicketItem.getAcceptTime(ticketStack);
        if (acceptTime <= 0) {
            return false; // Квест еще не принят
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedMinutes = (currentTime - acceptTime) / (1000 * 60);
        
        return elapsedMinutes >= quest.getTimeLimit();
    }
    
    /**
     * Получает форматирование для состояния квеста
     */
    private static Formatting getStateFormatting(QuestTicketState state) {
        return switch (state) {
            case AVAILABLE -> Formatting.WHITE;
            case ACCEPTED -> Formatting.GREEN;
            case IN_PROGRESS -> Formatting.YELLOW;
            case COMPLETED -> Formatting.GOLD;
            case FINISHED -> Formatting.GRAY;
            default -> Formatting.GRAY;
        };
    }
}