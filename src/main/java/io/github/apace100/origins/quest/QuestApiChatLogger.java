package io.github.apace100.origins.quest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Утилита для отправки сообщений о состоянии API в чат игроков
 */
public class QuestApiChatLogger {
    
    /**
     * Отправляет сообщение о запросе к API всем игрокам
     */
    public static void logApiRequest(MinecraftServer server, String playerClass, int questCount) {
        if (server == null) return;
        
        Text message = Text.literal("🌐 [Quest API] Запрос квестов для класса: ")
                .formatted(Formatting.BLUE)
                .append(Text.literal(playerClass).formatted(Formatting.YELLOW))
                .append(Text.literal(" (количество: " + questCount + ")").formatted(Formatting.GRAY));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение об успешном получении квестов
     */
    public static void logApiSuccess(MinecraftServer server, String playerClass, int questCount) {
        if (server == null) return;
        
        Text message = Text.literal("✅ [Quest API] Получено ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(String.valueOf(questCount)).formatted(Formatting.YELLOW))
                .append(Text.literal(" квестов для класса ").formatted(Formatting.GREEN))
                .append(Text.literal(playerClass).formatted(Formatting.YELLOW));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение об ошибке API
     */
    public static void logApiError(MinecraftServer server, String playerClass, String error) {
        if (server == null) return;
        
        Text message = Text.literal("❌ [Quest API] Ошибка для класса ")
                .formatted(Formatting.RED)
                .append(Text.literal(playerClass).formatted(Formatting.YELLOW))
                .append(Text.literal(": " + error).formatted(Formatting.GRAY));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение о недоступности API
     */
    public static void logApiUnavailable(MinecraftServer server) {
        if (server == null) return;
        
        Text message = Text.literal("🔴 [Quest API] API сервер недоступен! Классовые доски будут пустыми.")
                .formatted(Formatting.RED);
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение о подключении к API
     */
    public static void logApiConnected(MinecraftServer server) {
        if (server == null) return;
        
        Text message = Text.literal("🟢 [Quest API] API сервер подключен! Загружаем квесты...")
                .formatted(Formatting.GREEN);
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение о начале обновления квестов
     */
    public static void logQuestUpdate(MinecraftServer server) {
        if (server == null) return;
        
        Text message = Text.literal("🔄 [Quest API] Начинаем обновление квестов (каждые 30 минут)...")
                .formatted(Formatting.AQUA);
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение всем игрокам на сервере
     */
    private static void broadcastToAllPlayers(MinecraftServer server, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message, false);
        }
    }
    
    /**
     * Отправляет сообщение конкретному игроку
     */
    public static void sendToPlayer(ServerPlayerEntity player, Text message) {
        if (player != null) {
            player.sendMessage(message, false);
        }
    }
}