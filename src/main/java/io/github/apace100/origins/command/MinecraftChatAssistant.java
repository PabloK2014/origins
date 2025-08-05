package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.quest.ChatAssistantApiClient;
import io.github.apace100.origins.quest.ChatMessageAnimator;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Команда для чат-помощника Minecraft
 */
public class MinecraftChatAssistant {
    
    private static final String MINECRAFT_VERSION = "1.20.1";
    private static final int MAX_MESSAGE_LENGTH = 500; // Максимальная длина одной части сообщения
    
    /**
     * Регистрирует команду /ask
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("ask")
            .then(CommandManager.argument("question", StringArgumentType.greedyString())
                .executes(context -> handleAskCommand(context, 
                    StringArgumentType.getString(context, "question")))));
    }
    
    /**
     * Обрабатывает команду /ask
     */
    private static int handleAskCommand(CommandContext<ServerCommandSource> context, String question) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Валидация входных данных
            if (question == null || question.trim().isEmpty()) {
                ChatMessageAnimator.sendErrorMessage(source, "Пожалуйста, задайте вопрос. Пример: /ask как сделать палки");
                return 0;
            }
            
            if (question.length() > 2000) {
                ChatMessageAnimator.sendErrorMessage(source, "Вопрос слишком длинный. Максимум 2000 символов.");
                return 0;
            }
            
            Origins.LOGGER.info("🤖 [ChatAssistant] Игрок задал вопрос: " + question);
            
            // Показываем индикатор загрузки
            ChatMessageAnimator.sendLoadingMessage(source, "Отправляю ваш вопрос AI помощнику...");
            
            // Проверяем доступность API
            ChatAssistantApiClient.isChatApiAvailable()
                .thenAccept(available -> {
                    if (!available) {
                        ChatMessageAnimator.sendErrorMessage(source, "AI помощник временно недоступен. Попробуйте позже.");
                        return;
                    }
                    
                    // Отправляем вопрос к AI
                    ChatAssistantApiClient.askQuestion(question, MINECRAFT_VERSION)
                        .thenAccept(response -> {
                            try {
                                if (response.success) {
                                    // Успешный ответ - отображаем с анимацией
                                    handleSuccessfulResponse(source, response);
                                } else {
                                    // Ошибка от API
                                    ChatMessageAnimator.sendErrorMessage(source, 
                                        response.errorMessage != null ? response.errorMessage : "Произошла ошибка при обработке вопроса");
                                }
                            } catch (Exception e) {
                                Origins.LOGGER.error("Ошибка при обработке ответа от AI", e);
                                ChatMessageAnimator.sendErrorMessage(source, "Произошла ошибка при обработке ответа");
                            }
                        })
                        .exceptionally(throwable -> {
                            Origins.LOGGER.error("Ошибка при запросе к AI API", throwable);
                            ChatMessageAnimator.sendErrorMessage(source, "Не удалось связаться с AI помощником");
                            return null;
                        });
                })
                .exceptionally(throwable -> {
                    Origins.LOGGER.error("Ошибка при проверке доступности API", throwable);
                    ChatMessageAnimator.sendErrorMessage(source, "Не удалось проверить доступность AI помощника");
                    return null;
                });
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Критическая ошибка в команде /ask", e);
            ChatMessageAnimator.sendErrorMessage(source, "Произошла критическая ошибка");
            return 0;
        }
    }
    
    /**
     * Обрабатывает успешный ответ от AI
     */
    private static void handleSuccessfulResponse(ServerCommandSource source, ChatAssistantApiClient.ChatResponse response) {
        try {
            String answer = response.answer;
            
            // Определяем цвет форматирования на основе типа ответа
            Formatting color = switch (response.responseType) {
                case "recipe" -> Formatting.GOLD;
                case "mechanic" -> Formatting.BLUE;
                case "error" -> Formatting.RED;
                default -> Formatting.AQUA;
            };
            
            // Если ответ длинный, разбиваем на части
            if (answer.length() > MAX_MESSAGE_LENGTH) {
                List<String> parts = ChatMessageAnimator.splitLongMessage(answer, MAX_MESSAGE_LENGTH);
                
                if (parts.size() > 1) {
                    // Отправляем многочастное сообщение
                    ChatMessageAnimator.sendMultipartMessage(source, parts, 20); // 20 тиков = 1 секунда задержки
                    Origins.LOGGER.info("🤖 [ChatAssistant] Отправлен многочастный ответ (" + parts.size() + " частей)");
                } else {
                    // Отправляем как обычное анимированное сообщение
                    ChatMessageAnimator.sendAnimatedMessage(source, answer, color);
                    Origins.LOGGER.info("🤖 [ChatAssistant] Отправлен длинный ответ");
                }
            } else {
                // Отправляем как обычное анимированное сообщение
                ChatMessageAnimator.sendAnimatedMessage(source, answer, color);
                Origins.LOGGER.info("🤖 [ChatAssistant] Отправлен обычный ответ");
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при форматировании ответа", e);
            ChatMessageAnimator.sendErrorMessage(source, "Ошибка при форматировании ответа");
        }
    }
    
    /**
     * Показывает анимацию печатания
     */
    private static void sendTypingAnimation(ServerCommandSource source) {
        ChatMessageAnimator.sendTypingIndicator(source, "AI обрабатывает ваш вопрос");
    }
    
    /**
     * Отправляет форматированный ответ
     */
    private static void sendFormattedResponse(ServerCommandSource source, String response) {
        // Определяем тип ответа по содержимому
        Formatting color = Formatting.AQUA;
        if (response.toLowerCase().contains("рецепт") || response.toLowerCase().contains("крафт")) {
            color = Formatting.GOLD;
        } else if (response.toLowerCase().contains("механика") || response.toLowerCase().contains("работает")) {
            color = Formatting.BLUE;
        }
        
        ChatMessageAnimator.sendAnimatedMessage(source, response, color);
    }
}