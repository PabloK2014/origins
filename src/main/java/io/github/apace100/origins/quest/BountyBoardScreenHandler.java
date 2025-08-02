package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BountyBoardScreenHandler extends ScreenHandler {
    private final BountyBoardBlockEntity blockEntity;
    private final QuestInventory questInventory;
    private final Inventory inventory;

    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, (BountyBoardBlockEntity) playerInventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }

    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, BountyBoardBlockEntity blockEntity) {
        super(QuestRegistry.BOUNTY_BOARD_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.questInventory = new QuestInventory(22, blockEntity);
        this.inventory = new SimpleInventory(3);
        setupSlots(playerInventory);
    }

    private void setupSlots(PlayerInventory playerInventory) {
        // Слоты для квестов (3x7 сетка)
        int bountySlotSize = 18;
        int adjustX = 173;
        int adjustY = 0;
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 7; k++) {
                addSlot(new QuestSlot(blockEntity.getBounties(), k + j * 7, 8 + k * bountySlotSize + adjustX, 18 + j * bountySlotSize + adjustY));
            }
        }

        // Слот для выбранного квеста
        addSlot(new SelectedQuestSlot(questInventory, 21, 50, 50));

        // Слоты для декретов
        for (int j = 0; j < 3; j++) {
            addSlot(new DecreeSlot(inventory, j, 317, 18 + j * 18));
        }

        // Слоты инвентаря игрока
        for (int m = 0; m < 3; ++m) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 181 + l * 18, 84 + m * 18));
            }
        }
        for (int m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 181 + m * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return blockEntity != null && player.squaredDistanceTo(
                blockEntity.getPos().getX() + 0.5,
                blockEntity.getPos().getY() + 0.5,
                blockEntity.getPos().getZ() + 0.5
        ) <= 64.0;
    }
    
    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
        io.github.apace100.origins.Origins.LOGGER.info("ScreenHandler.onSlotClick: slotIndex={}, button={}, actionType={}", slotIndex, button, actionType);
        
        // Клик по центральной части (3x7 сетка) - слоты 0-20
        if (slotIndex >= 0 && slotIndex < 21) {
            io.github.apace100.origins.Origins.LOGGER.info("Клик по центральной части, слот: {}", slotIndex);
            
            // Получаем ItemStack из слота
            Slot slot = this.slots.get(slotIndex);
            io.github.apace100.origins.Origins.LOGGER.info("Слот: {}, hasStack: {}", slot != null ? slot.getClass().getSimpleName() : "null", slot != null ? slot.hasStack() : false);
            
            if (slot != null && slot.hasStack()) {
                ItemStack stack = slot.getStack();
                io.github.apace100.origins.Origins.LOGGER.info("ItemStack в слоте: {}, количество: {}", stack.getItem().getClass().getSimpleName(), stack.getCount());
                
                // Проверяем все возможные типы предметов
                if (stack.getItem() instanceof BountifulQuestItem) {
                    io.github.apace100.origins.Origins.LOGGER.info("Найден BountifulQuestItem - обрабатываем клик");
                    
                    if (actionType == net.minecraft.screen.slot.SlotActionType.PICKUP && button == 0) {
                        // Проверяем совместимость класса
                        BountifulQuestInfo info = BountifulQuestInfo.get(stack);
                        String playerClass = getCurrentPlayerClass();
                        String questClass = info.getProfession();
                        
                        io.github.apace100.origins.Origins.LOGGER.info("Проверка класса: игрок='{}', квест='{}'", playerClass, questClass);
                        
                        if (isClassCompatible(playerClass, questClass)) {
                            // Принимаем квест и позволяем взять билет
                            io.github.apace100.origins.Origins.LOGGER.info("Класс совместим - принимаем квест и позволяем взять билет");
                            if (acceptBountifulQuest(player, stack)) {
                                player.sendMessage(Text.literal("Квест принят!").formatted(Formatting.GREEN), false);
                                // Позволяем стандартной логике взять предмет
                                super.onSlotClick(slotIndex, button, actionType, player);
                                return;
                            } else {
                                player.sendMessage(Text.literal("Не удалось принять квест!").formatted(Formatting.RED), false);
                                return;
                            }
                        } else {
                            io.github.apace100.origins.Origins.LOGGER.info("Класс не совместим - блокируем взятие билета");
                            player.sendMessage(Text.literal("Этот квест недоступен для вашего класса!").formatted(Formatting.RED), false);
                            return;
                        }
                    } else {
                        io.github.apace100.origins.Origins.LOGGER.info("Неправильный тип клика: actionType={}, button={}", actionType, button);
                    }
                } else if (stack.getItem() instanceof QuestTicketItem) {
                    io.github.apace100.origins.Origins.LOGGER.info("Найден QuestTicketItem в центральной части - позволяем взять");
                    // Позволяем взять билет квеста
                    super.onSlotClick(slotIndex, button, actionType, player);
                    return;
                } else {
                    io.github.apace100.origins.Origins.LOGGER.info("Неизвестный тип предмета: {} - позволяем взять", stack.getItem().getClass().getSimpleName());
                    // Позволяем взять любой другой предмет
                    super.onSlotClick(slotIndex, button, actionType, player);
                    return;
                }
            } else {
                io.github.apace100.origins.Origins.LOGGER.info("Слот пустой или null");
            }
            
            // Если дошли сюда, позволяем стандартную обработку
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }
        
        // Клик по билету квеста (слот 21) - позволяем взаимодействие
        if (slotIndex == 21) {
            io.github.apace100.origins.Origins.LOGGER.info("Клик по слоту выбранного квеста (21) - позволяем взаимодействие");
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }
        
        // Остальные клики передаем дальше
        super.onSlotClick(slotIndex, button, actionType, player);
    }
    
    /**
     * Проверяет совместимость классов
     */
    private boolean isClassCompatible(String playerClass, String questClass) {
        io.github.apace100.origins.Origins.LOGGER.info("=== ПРОВЕРКА СОВМЕСТИМОСТИ КЛАССОВ ===");
        io.github.apace100.origins.Origins.LOGGER.info("Исходный класс игрока: '{}'", playerClass);
        io.github.apace100.origins.Origins.LOGGER.info("Исходный класс квеста: '{}'", questClass);
        
        if (questClass == null || questClass.equals("any")) {
            io.github.apace100.origins.Origins.LOGGER.info("Квест для любого класса - РАЗРЕШЕНО");
            return true;
        }
        
        // Нормализуем названия классов
        String normalizedPlayerClass = normalizeClassName(playerClass);
        String normalizedQuestClass = normalizeClassName(questClass);
        
        io.github.apace100.origins.Origins.LOGGER.info("Нормализованный класс игрока: '{}'", normalizedPlayerClass);
        io.github.apace100.origins.Origins.LOGGER.info("Нормализованный класс квеста: '{}'", normalizedQuestClass);
        
        boolean compatible = normalizedPlayerClass.equals(normalizedQuestClass);
        io.github.apace100.origins.Origins.LOGGER.info("Результат проверки: {}", compatible ? "РАЗРЕШЕНО" : "ЗАПРЕЩЕНО");
        io.github.apace100.origins.Origins.LOGGER.info("=== КОНЕЦ ПРОВЕРКИ ===");
        
        return compatible;
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
     * Принимает Bountiful квест
     */
    private boolean acceptBountifulQuest(PlayerEntity player, ItemStack questStack) {
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
            return acceptanceHandler.acceptQuestFromBoard(player, quest, blockEntity);
            
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

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);
        
        if (slotObj != null && slotObj.hasStack()) {
            ItemStack itemStack2 = slotObj.getStack();
            itemStack = itemStack2.copy();
            
            // Для слотов центральной части (0-20)
            if (slot < 21) {
                // Проверяем, можно ли взять предмет (включая проверку класса)
                if (slotObj.canTakeItems(player)) {
                    // Если это BountifulQuestItem, принимаем квест
                    if (itemStack2.getItem() instanceof BountifulQuestItem) {
                        if (acceptBountifulQuest(player, itemStack2)) {
                            player.sendMessage(Text.literal("Квест принят!").formatted(Formatting.GREEN), false);
                        }
                    }
                    
                    // Перемещаем в инвентарь игрока (слоты 25+)
                    if (!this.insertItem(itemStack2, 25, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Если нельзя взять (например, неподходящий класс), блокируем
                    return ItemStack.EMPTY;
                }
            }
            // Для слотов инвентаря игрока
            else if (slot >= 25) {
                // Пытаемся переместить в центральную часть (но это заблокировано canInsert)
                if (!this.insertItem(itemStack2, 0, 21, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (itemStack2.isEmpty()) {
                slotObj.setStack(ItemStack.EMPTY);
            } else {
                slotObj.markDirty();
            }
        }
        
        return itemStack;
    }

    // Основные методы
    public List<Quest> getAvailableQuests() {
        return questInventory.getAvailableQuests();
    }

    public int getSelectedQuestIndex() {
        return questInventory.getSelectedIndex();
    }

    public void setSelectedQuestIndex(int index) {
        questInventory.selectQuest(index);
    }

    public Quest getSelectedQuest() {
        return questInventory.getSelectedQuest();
    }
    
    public Quest getQuest(int index) {
        return questInventory.getQuest(index);
    }
    
    public BountyBoardBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public BountyBoardBlockEntity getBoardEntity() {
        return blockEntity;
    }
    
    public void refreshAvailableQuests() {
        if (blockEntity != null) {
            blockEntity.refreshQuests();
            questInventory.refreshQuests();
        }
    }
    
    public void completeQuest(Quest quest, PlayerEntity player) {
        // Заглушка для совместимости
    }
    
    // Заглушки для drag-and-drop (для совместимости с BountyBoardScreen)
    public QuestDragHandler getDragHandler() {
        return new QuestDragHandler(); // Возвращаем пустой обработчик
    }
    
    public boolean canPlaceQuestInSlot(Quest quest, int slotIndex) {
        return false; // Отключаем drag-and-drop
    }
    
    public void acceptQuestViaDragDrop(Quest quest) {
        // Заглушка
    }
    
    public void finishDragging(int targetSlot) {
        // Заглушка
    }
    
    public void cancelDragging() {
        // Заглушка
    }
    
    public void updateDragState(double mouseX, double mouseY) {
        // Заглушка
    }
    
    public boolean isQuestSlotMasked(int index) {
        return false; // Отключаем маскировку
    }

    public boolean acceptQuest(Quest quest, PlayerEntity player) {
        if (quest == null || player == null) return false;
        
        if (!canAcceptQuest(quest)) {
            return false;
        }
        
        try {
            QuestTicketAcceptanceHandler acceptanceHandler = QuestTicketAcceptanceHandler.getInstance();
            return acceptanceHandler.acceptQuestFromBoard(player, quest, blockEntity);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean canAcceptQuest(Quest quest) {
        if (quest == null) return false;
        
        String boardClass = getBoardClass();
        String playerClass = getCurrentPlayerClass();
        
        // Если это обычная доска (general), проверяем только класс квеста
        if ("general".equals(boardClass)) {
            if (quest.getPlayerClass() != null && !quest.getPlayerClass().equals("any") && !quest.getPlayerClass().equals(playerClass)) {
                return false;
            }
            return true;
        }
        
        // Если это специализированная доска, проверяем соответствие класса доски
        if (!boardClass.equals(playerClass)) {
            return false;
        }
        
        // Проверяем класс квеста (если указан)
        if (quest.getPlayerClass() != null && !quest.getPlayerClass().equals("any") && !quest.getPlayerClass().equals(playerClass)) {
            return false;
        }
        
        return true;
    }
    
    private String getBoardClass() {
        if (blockEntity != null) {
            return blockEntity.getBoardClass();
        }
        return "general";
    }

    private PlayerEntity getCurrentPlayer() {
        if (blockEntity != null && blockEntity.getWorld() != null) {
            return blockEntity.getWorld().getClosestPlayer(
                    blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ(), 10.0, false);
        }
        return null;
    }
    
    private String getCurrentPlayerClass() {
        PlayerEntity player = getCurrentPlayer();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            return getPlayerOriginClass(serverPlayer);
        }
        return "human";
    }

    private String getPlayerOriginClass(ServerPlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(
                        io.github.apace100.origins.origin.OriginLayers.getLayer(
                                io.github.apace100.origins.Origins.identifier("origin")));
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
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при получении класса игрока: " + e.getMessage());
        }
        return "human";
    }

    // Кастомные слоты
    private static class DecreeSlot extends Slot {
        public DecreeSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }

    private class QuestSlot extends Slot {
        private final int questIndex;

        public QuestSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.questIndex = index;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false; // Нельзя вставлять предметы в слоты квестов
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            // Теперь МОЖНО брать билеты из слотов!
            if (!hasStack()) {
                return false;
            }
            
            ItemStack stack = getStack();
            
            // Если это BountifulQuestItem, проверяем класс
            if (stack.getItem() instanceof BountifulQuestItem) {
                BountifulQuestInfo info = BountifulQuestInfo.get(stack);
                
                // Получаем класс игрока - используем переданного игрока или текущего
                String playerClass;
                if (playerEntity instanceof ServerPlayerEntity serverPlayer) {
                    playerClass = getPlayerOriginClass(serverPlayer);
                } else {
                    playerClass = getCurrentPlayerClass();
                }
                
                String questClass = info.getProfession();
                
                // Разрешаем взять только если класс подходит
                boolean compatible = isClassCompatible(playerClass, questClass);
                
                // Логируем для отладки
                io.github.apace100.origins.Origins.LOGGER.info("canTakeItems: игрок='{}', квест='{}', совместим={}", 
                    playerClass, questClass, compatible);
                
                return compatible;
            }
            
            // Для всех остальных предметов разрешаем взять
            return true;
        }

        @Override
        public ItemStack takeStack(int amount) {
            PlayerEntity player = getCurrentPlayer();
            
            // Проверяем, можно ли взять предмет с учетом игрока
            if (!canTakeItems(player)) {
                io.github.apace100.origins.Origins.LOGGER.info("takeStack заблокирован - canTakeItems вернул false для слота {}", questIndex);
                return ItemStack.EMPTY;
            }
            
            ItemStack stack = getStack();
            
            // Если это BountifulQuestItem, принимаем квест при взятии
            if (stack.getItem() instanceof BountifulQuestItem && player != null) {
                io.github.apace100.origins.Origins.LOGGER.info("Принимаем квест при взятии билета из слота {}", questIndex);
                if (acceptBountifulQuest(player, stack)) {
                    player.sendMessage(Text.literal("Квест принят!").formatted(Formatting.GREEN), false);
                } else {
                    io.github.apace100.origins.Origins.LOGGER.warn("Не удалось принять квест при взятии билета");
                    return ItemStack.EMPTY;
                }
            }
            
            // Возвращаем стандартное поведение
            return super.takeStack(amount);
        }

        public Quest getQuest() {
            return questInventory.getQuest(questIndex);
        }
    }

    private class SelectedQuestSlot extends Slot {
        public SelectedQuestSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }
    }
}