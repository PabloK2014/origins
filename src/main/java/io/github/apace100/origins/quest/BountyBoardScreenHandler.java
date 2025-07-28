package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
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

import java.util.List;

public class BountyBoardScreenHandler extends ScreenHandler {
    private final BountyBoardBlockEntity blockEntity;
    private final QuestInventory questInventory;
    private final Inventory inventory;
    private final QuestDragHandler dragHandler;
    private final QuestMaskManager maskManager;

    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, (BountyBoardBlockEntity) playerInventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }

    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, BountyBoardBlockEntity blockEntity) {
        super(QuestRegistry.BOUNTY_BOARD_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.questInventory = new QuestInventory(22, blockEntity); // 21 слот для квестов + 1 для выбранного
        this.inventory = new SimpleInventory(3); // 3 слота для декретов
        this.dragHandler = new QuestDragHandler();
        this.maskManager = new QuestMaskManager();
        setupSlots(playerInventory);
        initializeQuestMasking();
    }

    private void setupSlots(PlayerInventory playerInventory) {
        // Слоты для квестов (3x7 сетка)
        int bountySlotSize = 18;
        int adjustX = 173;
        int adjustY = 0;
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 7; k++) {
                addSlot(new QuestSlot(questInventory, k + j * 7, 8 + k * bountySlotSize + adjustX, 18 + j * bountySlotSize + adjustY));
            }
        }

        // Слот для выбранного квеста
        addSlot(new SelectedQuestSlot(questInventory, 21, 50, 50));

        // Слоты для декретов (3 слота справа)
        for (int j = 0; j < 3; j++) {
            addSlot(new DecreeSlot(inventory, j, 317, 18 + j * 18));
        }

        // Слоты инвентаря игрока (по умолчанию)
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
        io.github.apace100.origins.Origins.LOGGER.info("BountyBoardScreenHandler.onSlotClick: slotIndex={}, button={}, actionType={}, player={}", 
            slotIndex, button, actionType, player != null ? player.getName().getString() : "null");
        
        // Обработка кликов по квестам
        if (slotIndex >= 0 && slotIndex < 21) { // Quest slots
            io.github.apace100.origins.Origins.LOGGER.info("Клик по слоту квеста: {}", slotIndex);
            
            Slot slot = this.slots.get(slotIndex);
            io.github.apace100.origins.Origins.LOGGER.info("Слот: {}, является QuestSlot: {}", 
                slot.getClass().getSimpleName(), slot instanceof QuestSlot);
            
            if (slot instanceof QuestSlot questSlot && !questSlot.isMasked()) {
                io.github.apace100.origins.Origins.LOGGER.info("QuestSlot не замаскирован");
                
                Quest quest = questInventory.getQuest(slotIndex);
                io.github.apace100.origins.Origins.LOGGER.info("Квест в слоте: {}", 
                    quest != null ? quest.getTitle() : "null");
                
                if (quest != null) {
                    if (actionType == net.minecraft.screen.slot.SlotActionType.PICKUP) {
                        io.github.apace100.origins.Origins.LOGGER.info("Действие PICKUP");
                        
                        if (button == 0) { // Left click - принимаем квест
                            io.github.apace100.origins.Origins.LOGGER.info("Левый клик - попытка принять квест: {}", quest.getTitle());
                            
                            if (acceptQuest(quest, player)) {
                                io.github.apace100.origins.Origins.LOGGER.info("Квест {} успешно принят", quest.getTitle());
                                return; // Квест принят, не продолжаем обработку
                            } else {
                                io.github.apace100.origins.Origins.LOGGER.warn("Не удалось принять квест: {}", quest.getTitle());
                            }
                        } else if (button == 1) { // Right click - выбираем квест
                            io.github.apace100.origins.Origins.LOGGER.info("Правый клик - выбираем квест: {}", quest.getTitle());
                            setSelectedQuestIndex(slotIndex);
                            return;
                        }
                    } else {
                        io.github.apace100.origins.Origins.LOGGER.info("Действие не PICKUP: {}", actionType);
                    }
                } else {
                    io.github.apace100.origins.Origins.LOGGER.warn("Квест в слоте {} равен null", slotIndex);
                }
            } else {
                io.github.apace100.origins.Origins.LOGGER.warn("Слот {} не является QuestSlot или замаскирован", slotIndex);
            }
        } else if (slotIndex == 21) { // Selected quest slot
            io.github.apace100.origins.Origins.LOGGER.info("Клик по слоту выбранного квеста");
            if (isDragging()) {
                finishDragging(slotIndex);
                return;
            }
        } else {
            io.github.apace100.origins.Origins.LOGGER.info("Клик по другому слоту: {}", slotIndex);
        }
        
        // Отмена перетаскивания при клике в другие области
        if (isDragging() && actionType == net.minecraft.screen.slot.SlotActionType.PICKUP) {
            io.github.apace100.origins.Origins.LOGGER.info("Отмена перетаскивания");
            cancelDragging();
            return;
        }
        
        io.github.apace100.origins.Origins.LOGGER.info("Передача клика в super.onSlotClick");
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);
        if (slotObj != null && slotObj.hasStack()) {
            ItemStack itemStack2 = slotObj.getStack();
            itemStack = itemStack2.copy();
            
            // 21 quest slots + 1 selected quest slot + 3 decree slots = 25 total special slots
            int specialSlotsCount = 25;
            
            // Обработка квестов через drag-and-drop
            if (slot < 21 && QuestItem.isQuestStack(itemStack2)) {
                // Попытка переместить квест в selected slot
                Quest quest = QuestItem.getQuestFromStack(itemStack2);
                if (quest != null && canPlaceQuestInSlot(quest, 21)) {
                    acceptQuestViaDragDrop(quest);
                    return ItemStack.EMPTY;
                }
            }
            
            if (slot < specialSlotsCount) {
                // From special slots to player inventory
                if (!this.insertItem(itemStack2, specialSlotsCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to special slots (only decree slots can accept items)
                if (!this.insertItem(itemStack2, 22, 25, false)) {
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

    public List<Quest> getAvailableQuests() {
        return questInventory.getAvailableQuests();
    }

    public int getSelectedQuestIndex() {
        return questInventory.getSelectedIndex();
    }

    public void setSelectedQuestIndex(int index) {
        questInventory.selectQuest(index);
        // Auto-sync selection state
        syncQuestState();
    }

    public Quest getSelectedQuest() {
        return questInventory.getSelectedQuest();
    }
    
    public Quest getQuest(int index) {
        return questInventory.getQuest(index);
    }
    
    public QuestInventory getQuestInventory() {
        return questInventory;
    }

    public void acceptQuest(Quest quest) {
        PlayerEntity player = getCurrentPlayer();
        if (player != null) {
            acceptQuest(quest, player);
        }
    }

    public void completeQuest(Quest quest, PlayerEntity player) {
        if (quest == null) return;

        if (hasRequiredItems(player, quest)) {
            removeRequiredItems(player, quest);
            giveQuestReward(quest, player);
            if (blockEntity != null) {
                blockEntity.removeQuest(quest);
            }
            player.sendMessage(Text.translatable("gui.origins.bounty_board.quest_completed"), false);
        } else {
            player.sendMessage(Text.literal("У вас недостаточно предметов!"), false);
        }
    }
    
    public void refreshQuests() {
        if (blockEntity != null) {
            blockEntity.refreshQuests();
        }
        questInventory.refreshQuests();
        // Auto-sync after refresh
        syncQuestState();
    }
    
    public BountyBoardBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    /**
     * Маскирует слот квеста
     */
    public void maskQuestSlot(int index) {
        questInventory.maskSlot(index);
    }
    
    /**
     * Снимает маскировку со слота квеста
     */
    public void unmaskQuestSlot(int index) {
        questInventory.unmaskSlot(index);
    }
    
    /**
     * Проверяет, замаскирован ли слот квеста
     */
    public boolean isQuestSlotMasked(int index) {
        return questInventory.isSlotMasked(index);
    }
    
    /**
     * Маскирует похожие квесты при принятии квеста
     */
    public void maskSimilarQuests(Quest quest) {
        questInventory.maskSimilarQuests(quest);
    }
    
    /**
     * Снимает маскировку с похожих квестов при отмене квеста
     */
    public void unmaskSimilarQuests(Quest quest) {
        questInventory.unmaskSimilarQuests(quest);
    }
    
    /**
     * Получает список видимых квестов
     */
    public List<Quest> getVisibleQuests() {
        return questInventory.getVisibleQuests();
    }
    
    /**
     * Инициализирует маскировку квестов на основе активных квестов игрока
     */
    private void initializeQuestMasking() {
        // TODO: Получить UUID игрока и инициализировать маскировку
        // Пока что оставляем пустым
    }
    
    /**
     * Получает обработчик drag-and-drop
     */
    public QuestDragHandler getDragHandler() {
        return dragHandler;
    }
    
    /**
     * Получает менеджер маскировки квестов
     */
    public QuestMaskManager getMaskManager() {
        return maskManager;
    }
    
    /**
     * Обрабатывает начало перетаскивания квеста
     */
    public boolean startDragging(int slotIndex, Quest quest) {
        if (quest == null) return false;
        return dragHandler.startDrag(quest, slotIndex, 0, 0); // Координаты мыши будут обновлены позже
    }
    
    /**
     * Обрабатывает завершение перетаскивания квеста
     */
    public boolean finishDragging(int targetSlotIndex) {
        QuestDragHandler.DragResult result = dragHandler.completeDrag(0, 0, questInventory);
        return result == QuestDragHandler.DragResult.SUCCESS;
    }
    
    /**
     * Отменяет текущее перетаскивание
     */
    public void cancelDragging() {
        dragHandler.cancelDrag();
    }
    
    /**
     * Проверяет, происходит ли сейчас перетаскивание
     */
    public boolean isDragging() {
        return dragHandler.isDragging();
    }
    
    /**
     * Получает квест, который сейчас перетаскивается
     */
    public Quest getDraggedQuest() {
        return dragHandler.getDraggedQuest();
    }
    
    /**
     * Проверяет, может ли квест быть помещен в указанный слот
     */
    public boolean canPlaceQuestInSlot(Quest quest, int slotIndex) {
        if (slotIndex == 21) { // SelectedQuestSlot
            return quest != null && quest.getPlayerClass().equals(getCurrentPlayerClass());
        }
        return false; // Обычные слоты квестов не принимают перетаскиваемые квесты
    }
    
    /**
     * Принимает квест (основной метод)
     */
    public boolean acceptQuest(Quest quest, PlayerEntity player) {
        io.github.apace100.origins.Origins.LOGGER.info("BountyBoardScreenHandler.acceptQuest: quest={}, player={}", 
            quest != null ? quest.getTitle() : "null", 
            player != null ? player.getName().getString() : "null");
        
        if (quest == null || player == null) {
            io.github.apace100.origins.Origins.LOGGER.warn("Квест или игрок равен null");
            return false;
        }
        
        try {
            io.github.apace100.origins.Origins.LOGGER.info("Получаем QuestTicketAcceptanceHandler");
            // Используем QuestTicketAcceptanceHandler для принятия квеста
            QuestTicketAcceptanceHandler acceptanceHandler = QuestTicketAcceptanceHandler.getInstance();
            
            io.github.apace100.origins.Origins.LOGGER.info("Вызываем acceptQuestFromBoard с blockEntity: {}", 
                blockEntity != null ? blockEntity.getPos() : "null");
            
            boolean result = acceptanceHandler.acceptQuestFromBoard(player, quest, blockEntity);
            
            io.github.apace100.origins.Origins.LOGGER.info("Результат принятия квеста: {}", result);
            return result;
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Ошибка при принятии квеста в ScreenHandler: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Обрабатывает принятие квеста через drag-and-drop
     */
    public void acceptQuestViaDragDrop(Quest quest) {
        if (quest != null && canAcceptQuest(quest)) {
            PlayerEntity player = getCurrentPlayer();
            if (player != null) {
                acceptQuest(quest, player);
            }
            // TODO: Получить UUID игрока и замаскировать похожие квесты
            // QuestMaskManager.maskSimilarQuests(playerUUID, quest, questInventory.getAvailableQuests());
            // Синхронизация с клиентом будет добавлена позже
        }
    }
    
    /**
     * Проверяет, может ли игрок принять квест
     */
    private boolean canAcceptQuest(Quest quest) {
        if (quest == null) return false;
        
        // Проверяем класс игрока
        if (!quest.getPlayerClass().equals(getCurrentPlayerClass())) {
            return false;
        }
        
        // Проверяем, не принят ли уже этот квест
        // TODO: Добавить проверку активных квестов игрока
        
        return true;
    }
    
    /**
     * Обновляет состояние drag-and-drop
     */
    public void updateDragState(double mouseX, double mouseY) {
        dragHandler.updateDrag((int)mouseX, (int)mouseY);
    }
    
    /**
     * Синхронизирует состояние квестов с клиентом
     */
    public void syncQuestState() {
        PlayerEntity player = getCurrentPlayer();
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            syncQuestState(serverPlayer);
        }
        questInventory.markDirty();
    }
    
    /**
     * Синхронизирует состояние квестов с конкретным игроком
     */
    public void syncQuestState(net.minecraft.server.network.ServerPlayerEntity player) {
        try {
            // Send quest list update
            QuestSyncPacket questPacket = QuestSyncPacket.create(getAvailableQuests());
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                player, 
                QuestSyncPacket.ID, 
                questPacket.toPacketByteBuf()
            );
            
            // Send selection state update
            SelectionSyncPacket selectionPacket = SelectionSyncPacket.create(
                getSelectedQuestIndex(), 
                getSelectedQuest() != null ? getSelectedQuest().getId() : null
            );
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                player, 
                SelectionSyncPacket.ID, 
                selectionPacket.toPacketByteBuf()
            );
            
            // Send mask state update
            MaskSyncPacket maskPacket = MaskSyncPacket.create(questInventory.getMaskedSlots());
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                player, 
                MaskSyncPacket.ID, 
                maskPacket.toPacketByteBuf()
            );
            
            io.github.apace100.origins.Origins.LOGGER.info("Quest state synchronized for player {}", player.getName().getString());
            
        } catch (Exception e) {
            io.github.apace100.origins.Origins.LOGGER.error("Failed to sync quest state: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Отменяем любое активное перетаскивание при закрытии экрана
        if (isDragging()) {
            cancelDragging();
        }
        // TODO: Сохранить состояние маскировки
        // maskManager.savePlayerMask(playerUUID);
    }

    private boolean hasRequiredItems(PlayerEntity player, Quest quest) {
        // Проверяем выполнение всех целей квеста
        for (QuestObjective objective : quest.getObjectives()) {
            if (!isObjectiveCompleted(player, objective)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isObjectiveCompleted(PlayerEntity player, QuestObjective objective) {
        switch (objective.getType()) {
            case COLLECT:
                return hasItemInInventory(player, objective.getTarget(), objective.getAmount());
            case KILL:
                // Для убийства мобов нужна отдельная система отслеживания
                return objective.isCompleted();
            case CRAFT:
                // Для крафта также нужна отдельная система
                return objective.isCompleted();
            default:
                return false;
        }
    }
    
    private boolean hasItemInInventory(PlayerEntity player, String itemId, int amount) {
        int foundAmount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem().toString().equals(itemId)) {
                foundAmount += stack.getCount();
                if (foundAmount >= amount) return true;
            }
        }
        return false;
    }

    private void removeRequiredItems(PlayerEntity player, Quest quest) {
        for (QuestObjective objective : quest.getObjectives()) {
            if (objective.getType().equals("collect")) {
                removeItemFromInventory(player, objective.getTarget(), objective.getAmount());
            }
        }
    }
    
    private void removeItemFromInventory(PlayerEntity player, String itemId, int amount) {
        int remainingToRemove = amount;
        for (int i = 0; i < player.getInventory().size() && remainingToRemove > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem().toString().equals(itemId)) {
                int toRemove = Math.min(remainingToRemove, stack.getCount());
                stack.decrement(toRemove);
                remainingToRemove -= toRemove;
            }
        }
    }

    private void giveQuestReward(Quest quest, PlayerEntity player) {
        for (QuestReward reward : quest.getRewards()) {
            ItemStack rewardStack = switch (reward.getTier()) {
                case 1 -> new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER1);
                case 2 -> new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER2);
                case 3 -> new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER3);
                default -> new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER1);
            };

            if (!player.getInventory().insertStack(rewardStack)) {
                player.dropItem(rewardStack, false);
            }
        }
        player.sendMessage(Text.translatable("gui.origins.bounty_board.reward_received"), false);
    }

    /**
     * Получает текущего игрока
     */
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
    private static class BountySlot extends Slot {
        public BountySlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false; // Нельзя вставлять предметы в слоты квестов
        }
    }

    /**
     * Получает BlockEntity доски объявлений
     */
    public BountyBoardBlockEntity getBoardEntity() {
        return blockEntity;
    }
    
    /**
     * Обновляет список доступных квестов
     */
    public void refreshAvailableQuests() {
        if (blockEntity != null) {
            blockEntity.refreshQuests();
        }
    }

    private static class DecreeSlot extends Slot {
        public DecreeSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false; // Пока без декретов, можно доработать позже
        }
    }
}