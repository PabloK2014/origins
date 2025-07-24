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

    // Конструктор для клиентской стороны (из пакета)
    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, (BountyBoardBlockEntity) playerInventory.player.getWorld().getBlockEntity(buf.readBlockPos()));
    }

    // Конструктор для серверной стороны
    public BountyBoardScreenHandler(int syncId, PlayerInventory playerInventory, BountyBoardBlockEntity blockEntity) {
        super(QuestRegistry.BOUNTY_BOARD_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.inventory = new SimpleInventory(9);
        setupSlots(playerInventory);
    }

    private void setupSlots(PlayerInventory playerInventory) {
        // Добавляем слоты для инвентаря игрока
        for (int m = 0; m < 3; ++m) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        // Добавляем слоты хотбара
        for (int m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return blockEntity == null || player.squaredDistanceTo(
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
            if (slot < this.inventory.size()) {
                if (!this.insertItem(itemStack2, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(itemStack2, 0, this.inventory.size(), false)) {
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

    public List<BountyQuest> getAvailableQuests() {
        return blockEntity != null ? blockEntity.getAvailableQuests() : List.of();
    }

    public int getSelectedQuestIndex() {
        return selectedQuestIndex;
    }

    public void setSelectedQuestIndex(int index) {
        this.selectedQuestIndex = index;
    }

    public BountyQuest getSelectedQuest() {
        if (blockEntity == null || selectedQuestIndex < 0 || selectedQuestIndex >= blockEntity.getAvailableQuests().size()) {
            return null;
        }
        return blockEntity.getAvailableQuests().get(selectedQuestIndex);
    }

    public void acceptQuest(int index) {
        if (blockEntity != null && index >= 0 && index < blockEntity.getAvailableQuests().size()) {
            BountyQuest quest = blockEntity.getAvailableQuests().get(index);
            // Проверяем, подходит ли квест для класса игрока
            if (quest.getProfession().equals(getCurrentPlayerClass())) {
                // Здесь будет логика принятия квеста
                // TODO: Реализовать систему активных квестов
            }
        }
    }

    public void completeQuest(BountyQuest quest, ServerPlayerEntity player) {
        if (quest == null) {
            return;
        }

        // Проверяем, есть ли у игрока необходимые предметы в инвентаре
        if (hasRequiredItems(player, quest)) {
            // Забираем предметы из инвентаря
            removeRequiredItems(player, quest);
            
            // Выдаем награду
            giveQuestReward(quest, player);
            
            // Удаляем квест с доски
            if (blockEntity != null) {
                blockEntity.removeQuest(quest);
            }
            
            // Отправляем сообщение об успешном выполнении
            player.sendMessage(
                Text.translatable("gui.origins.bounty_board.quest_completed"),
                false
            );
        } else {
            // Отправляем сообщение о недостатке предметов
            player.sendMessage(
                Text.literal("У вас недостаточно предметов для выполнения этого задания!"),
                false
            );
        }
    }
    
    private boolean hasRequiredItems(ServerPlayerEntity player, BountyQuest quest) {
        int foundAmount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == quest.getRequiredItem()) {
                foundAmount += stack.getCount();
                if (foundAmount >= quest.getRequiredAmount()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void removeRequiredItems(ServerPlayerEntity player, BountyQuest quest) {
        int remainingToRemove = quest.getRequiredAmount();
        
        for (int i = 0; i < player.getInventory().size() && remainingToRemove > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == quest.getRequiredItem()) {
                int toRemove = Math.min(remainingToRemove, stack.getCount());
                stack.decrement(toRemove);
                remainingToRemove -= toRemove;
            }
        }
    }

    private void giveQuestReward(BountyQuest quest, ServerPlayerEntity player) {
        // Создаем соответствующий SkillPointToken
        ItemStack reward;
        switch (quest.getLevel()) {
            case 1:
                reward = new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER1);
                break;
            case 2:
                reward = new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER2);
                break;
            case 3:
                reward = new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER3);
                break;
            default:
                reward = new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER1);
        }

        // Пытаемся добавить в инвентарь
        if (!player.getInventory().insertStack(reward)) {
            // Если инвентарь полный, дропаем рядом с игроком
            player.dropItem(reward, false);
        }

        // Отправляем сообщение игроку
        player.sendMessage(
            net.minecraft.text.Text.translatable("gui.origins.bounty_board.reward_received", quest.getRewardExp()),
            false
        );
    }

    private String getCurrentPlayerClass() {
        // Получаем текущий класс игрока из компонента Origins
        if (blockEntity != null && blockEntity.getWorld() != null) {
            // Ищем ближайшего игрока
            net.minecraft.entity.player.PlayerEntity nearestPlayer = blockEntity.getWorld().getClosestPlayer(
                blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ(), 10.0, false);
            
            if (nearestPlayer instanceof ServerPlayerEntity serverPlayer) {
                return getPlayerOriginClass(serverPlayer);
            }
        }
        return "human"; // По умолчанию
    }
    
    private String getPlayerOriginClass(ServerPlayerEntity player) {
        try {
            // Проверяем по компоненту Origins
            io.github.apace100.origins.component.OriginComponent originComponent = 
                io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            
            if (originComponent != null) {
                var origin = originComponent.getOrigin(
                    io.github.apace100.origins.origin.OriginLayers.getLayer(
                        io.github.apace100.origins.Origins.identifier("origin")));
                
                if (origin != null) {
                    String originId = origin.getIdentifier().toString();
                    // Преобразуем ID происхождения в класс
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
}