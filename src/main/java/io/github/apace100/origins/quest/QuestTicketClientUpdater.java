package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Обновляет время в билетах квестов на клиенте для корректного отображения tooltip
 */
public class QuestTicketClientUpdater {
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 5; // Обновляем каждые 5 тиков (4 раза в секунду) для плавности
    
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(QuestTicketClientUpdater::onClientTick);
    }
    
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updatePlayerQuestTickets(client.player);
        }
    }
    
    private static void updatePlayerQuestTickets(PlayerEntity player) {
        // Обновляем билеты в инвентаре игрока
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (QuestTicketItem.isQuestTicket(stack)) {
                updateTicketTooltipData(stack);
            }
        }
    }
    
    private static void updateTicketTooltipData(ItemStack stack) {
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest == null || quest.getTimeLimit() <= 0) {
            return;
        }
        
        // Проверяем, что билет находится в инвентаре игрока
        if (!io.github.apace100.origins.util.InventoryHelper.isStackInClientPlayerInventory(stack)) {
            return; // Не обновляем время для билетов не в инвентаре игрока
        }
        
        QuestTicketState state = QuestTicketItem.getTicketState(stack);
        long currentTime = System.currentTimeMillis();
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        
        // Если билет доступен, но еще не принят, автоматически инициализируем время
        if (state == QuestTicketState.AVAILABLE) {
            if (!nbt.contains("accept_time") || nbt.getLong("accept_time") == 0) {
                nbt.putLong("accept_time", currentTime);
                nbt.putString("quest_state", QuestTicketState.ACCEPTED.getName());
                state = QuestTicketState.ACCEPTED; // Обновляем локальную переменную
            }
        }
        
        // Теперь проверяем активные квесты
        if (state.isActive()) {
            long acceptTime = nbt.getLong("accept_time");
            if (acceptTime > 0) {
                long elapsedSeconds = (currentTime - acceptTime) / 1000;
                long totalLimitSeconds = quest.getTimeLimit() * 60;
                
                // Проверяем, не истекло ли время на клиенте
                if (elapsedSeconds >= totalLimitSeconds) {
                    // Время истекло, помечаем квест как проваленный на клиенте
                    nbt.putString("quest_state", QuestTicketState.FAILED.getName());
                    nbt.putBoolean("completion_ready", false);
                }
            }
        }
        
        // Обновляем кэшированное время для tooltip
        // Это заставит tooltip пересчитать время при следующем отображении
        nbt.putLong("last_tooltip_update", currentTime);
        nbt.putLong("client_current_time", currentTime);
    }
}