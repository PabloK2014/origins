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
    private final Inventory inventory;
    private int selectedQuestIndex = -1;

    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, (BountyBoardBlockEntity) playerInventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }

    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, BountyBoardBlockEntity blockEntity) {
        super(QuestRegistry.BOUNTY_BOARD_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.inventory = new SimpleInventory(24); // 21 слот для квестов + 3 для декретов
        setupSlots(playerInventory);
    }

    private void setupSlots(PlayerInventory playerInventory) {
        // Слоты для квестов (3x7 сетка)
        int bountySlotSize = 18;
        int adjustX = 173;
        int adjustY = 0;
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 7; k++) {
                addSlot(new BountySlot(inventory, k + j * 7, 8 + k * bountySlotSize + adjustX, 18 + j * bountySlotSize + adjustY));
            }
        }

        // Слоты для декретов (3 слота справа)
        for (int j = 0; j < 3; j++) {
            addSlot(new DecreeSlot(inventory, 21 + j, 317, 18 + j * 18));
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
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        if (slot2 != null && slot2.hasStack()) {
            ItemStack itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();
            if (slot < inventory.size()) {
                if (!this.insertItem(itemStack2, inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(itemStack2, 0, inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }
        return itemStack;
    }

    public List<Quest> getAvailableQuests() {
        return blockEntity != null ? blockEntity.getAvailableQuests() : List.of();
    }

    public int getSelectedQuestIndex() {
        return selectedQuestIndex;
    }

    public void setSelectedQuestIndex(int index) {
        this.selectedQuestIndex = index;
    }

    public Quest getSelectedQuest() {
        if (blockEntity == null || selectedQuestIndex < 0 || selectedQuestIndex >= blockEntity.getAvailableQuests().size()) {
            return null;
        }
        return blockEntity.getAvailableQuests().get(selectedQuestIndex);
    }
    
    public Quest getQuest(int index) {
        if (blockEntity == null || index < 0 || index >= blockEntity.getAvailableQuests().size()) {
            return null;
        }
        return blockEntity.getAvailableQuests().get(index);
    }

    public void acceptQuest(Quest quest) {
        if (quest != null && quest.getPlayerClass().equals(getCurrentPlayerClass())) {
            // Логика принятия квеста (можно добавить сохранение в будущем)
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
    }
    
    public BountyBoardBlockEntity getBlockEntity() {
        return blockEntity;
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

    private String getCurrentPlayerClass() {
        if (blockEntity != null && blockEntity.getWorld() != null) {
            PlayerEntity nearestPlayer = blockEntity.getWorld().getClosestPlayer(
                    blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ(), 10.0, false);
            if (nearestPlayer instanceof ServerPlayerEntity serverPlayer) {
                return getPlayerOriginClass(serverPlayer);
            }
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