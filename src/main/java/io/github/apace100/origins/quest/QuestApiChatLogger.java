package io.github.apace100.origins.quest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Утилита для отправки сообщений о состоянии API в чат игроков
 */
public class QuestApiChatLogger {
    
    // Защита от спама сообщений
    private static long lastRequestMessage = 0;
    private static long lastSuccessMessage = 0;
    private static long lastErrorMessage = 0;
    private static final long MESSAGE_COOLDOWN = 30000L; // 30 секунд между одинаковыми сообщениями
    
    /**
     * Отправляет сообщение о запросе к API всем игрокам
     */
    public static void logApiRequest(MinecraftServer server, String requestType, int questCount) {
        if (server == null) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestMessage < MESSAGE_COOLDOWN) {
            return; // Пропускаем сообщение, если прошло меньше 30 секунд
        }
        lastRequestMessage = currentTime;
        
        Text message = Text.literal("🌐 [Quest API] Отправляем запросы: ")
                .formatted(Formatting.BLUE)
                .append(Text.literal(requestType).formatted(Formatting.YELLOW))
                .append(Text.literal(" (ожидаем " + questCount + " квестов)").formatted(Formatting.GRAY));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение об успешном получении квестов
     */
    public static void logApiSuccess(MinecraftServer server, String playerClass, int questCount) {
        if (server == null) return;
        
        // Не спамим сообщениями об успехе для отдельных классов - только итоговое сообщение
        // Это сообщение будет показано только через logQuestsAppeared
    }
    
    /**
     * Отправляет сообщение об ошибке API
     */
    public static void logApiError(MinecraftServer server, String playerClass, String error) {
        if (server == null) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastErrorMessage < MESSAGE_COOLDOWN) {
            return; // Пропускаем сообщение, если прошло меньше 30 секунд
        }
        lastErrorMessage = currentTime;
        
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
     * Отправляет сообщение о том, что API дал ответ и квесты появились
     */
    public static void logQuestsAppeared(MinecraftServer server, int totalQuests) {
        if (server == null) return;
        
        Text message = Text.literal("🎯 [Quest API] API дал ответ, квесты появились! Загружено ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(String.valueOf(totalQuests)).formatted(Formatting.YELLOW))
                .append(Text.literal(" квестов для всех классов.").formatted(Formatting.GREEN));
        
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
    
    /**
     * Отправляет предупреждение о скором обновлении квестов
     */
    public static void logQuestUpdateWarning(MinecraftServer server, int minutesLeft) {
        if (server == null) return;
        
        Text message = Text.literal("⏰ [Quest API] Обновление квестов через ")
                .formatted(Formatting.YELLOW)
                .append(Text.literal(String.valueOf(minutesLeft)).formatted(Formatting.GOLD))
                .append(Text.literal(" минуту!").formatted(Formatting.YELLOW));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет уведомление о количестве накопленных квестов
     */
    public static void logQuestAccumulation(MinecraftServer server, String playerClass, int newQuests, int totalQuests, int requestNumber, int maxRequests) {
        if (server == null) return;
        
        Text message = Text.literal("📋 [Quest API] Класс ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(playerClass).formatted(Formatting.YELLOW))
                .append(Text.literal(": получено ").formatted(Formatting.AQUA))
                .append(Text.literal(String.valueOf(newQuests)).formatted(Formatting.GREEN))
                .append(Text.literal(" новых квестов. Всего: ").formatted(Formatting.AQUA))
                .append(Text.literal(String.valueOf(totalQuests)).formatted(Formatting.GOLD))
                .append(Text.literal(" (запрос ").formatted(Formatting.GRAY))
                .append(Text.literal(requestNumber + "/" + maxRequests).formatted(Formatting.WHITE))
                .append(Text.literal(")").formatted(Formatting.GRAY));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет уведомление об очистке доски квестов
     */
    public static void logBoardCleared(MinecraftServer server, String playerClass) {
        if (server == null) return;
        
        Text message = Text.literal("🗑️ [Quest API] Доска класса ")
                .formatted(Formatting.GOLD)
                .append(Text.literal(playerClass).formatted(Formatting.YELLOW))
                .append(Text.literal(" очищена после 3 запросов. Начинаем новый цикл накопления.").formatted(Formatting.GOLD));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет детальное сообщение об ошибке с предложением повторить запрос
     */
    public static void logDetailedApiError(MinecraftServer server, String playerClass, String error, boolean willRetry) {
        if (server == null) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastErrorMessage < MESSAGE_COOLDOWN) {
            return;
        }
        lastErrorMessage = currentTime;
        
        Text message = Text.literal("❌ [Quest API] Ошибка для класса ")
                .formatted(Formatting.RED)
                .append(Text.literal(playerClass).formatted(Formatting.YELLOW))
                .append(Text.literal(": ").formatted(Formatting.RED))
                .append(Text.literal(error).formatted(Formatting.GRAY));
        
        if (willRetry) {
            message = message.copy().append(Text.literal(" Повторим запрос через минуту.").formatted(Formatting.YELLOW));
        }
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет обратный отсчет до следующего запроса
     */
    public static void logCountdownToNextRequest(MinecraftServer server, int minutesLeft) {
        if (server == null) return;
        
        // Показываем обратный отсчет только на определенных интервалах
        if (minutesLeft == 10 || minutesLeft == 5 || minutesLeft == 1) {
            Text message = Text.literal("⏳ [Quest API] До следующего обновления квестов: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(String.valueOf(minutesLeft)).formatted(Formatting.YELLOW))
                    .append(Text.literal(" мин.").formatted(Formatting.GRAY));
            
            broadcastToAllPlayers(server, message);
        }
    }
    
    /**
     * Отправляет сообщение о результатах отдельных запросов
     */
    public static void logSeparateRequestsResult(MinecraftServer server, int successfulClasses, int totalClasses, int totalQuests) {
        if (server == null) return;
        
        Text message = Text.literal("📊 [Quest API] Результат отдельных запросов: ")
                .formatted(Formatting.AQUA)
                .append(Text.literal(successfulClasses + "/" + totalClasses).formatted(Formatting.YELLOW))
                .append(Text.literal(" классов получили квесты, всего ").formatted(Formatting.AQUA))
                .append(Text.literal(String.valueOf(totalQuests)).formatted(Formatting.GREEN))
                .append(Text.literal(" квестов").formatted(Formatting.AQUA));
        
        broadcastToAllPlayers(server, message);
    }
    
    /**
     * Отправляет сообщение о генерации квестов для конкретного класса
     */
    public static void logClassQuestsGenerated(MinecraftServer server, String playerClass, int questCount) {
        if (server == null) return;
        
        Text message = Text.literal("✅ [Quest API] Квесты для класса ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(playerClass).formatted(Formatting.YELLOW))
                .append(Text.literal(" сгенерированы! Получено ").formatted(Formatting.GREEN))
                .append(Text.literal(String.valueOf(questCount)).formatted(Formatting.GOLD))
                .append(Text.literal(" квестов.").formatted(Formatting.GREEN));
        
        broadcastToAllPlayers(server, message);
    }
}