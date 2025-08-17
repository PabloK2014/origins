package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Обновляет время в билетах квестов
 */
public class QuestTicketTimeUpdater {
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 20; // Обновляем каждую секунду (20 тиков)
    
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(QuestTicketTimeUpdater::onServerTick);
    }
    
    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updateAllQuestTickets(server);
        }
    }
    
    private static void updateAllQuestTickets(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            updatePlayerQuestTickets(player);
        }
    }
    
    private static void updatePlayerQuestTickets(PlayerEntity player) {
        // Обновляем билеты в инвентаре игрока
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (QuestTicketItem.isQuestTicket(stack)) {
                updateTicketTime(stack);
            }
        }
    }
    
    private static void updateTicketTime(ItemStack stack) {
        if (!QuestTicketItem.isAccepted(stack)) {
            return;
        }
        
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest == null || quest.getTimeLimit() <= 0) {
            return;
        }
        
        long acceptTime = QuestTicketItem.getAcceptTime(stack);
        if (acceptTime <= 0) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - acceptTime) / 1000;
        long totalLimitSeconds = quest.getTimeLimit() * 60; // конвертируем минуты в секунды
        
        // Если время истекло, помечаем квест как проваленный
        if (elapsedSeconds >= totalLimitSeconds) {
            QuestTicketItem.markAsFailed(stack);
      }
    }
}