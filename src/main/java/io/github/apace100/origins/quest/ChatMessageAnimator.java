package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Класс для создания анимированных сообщений в чате Minecraft
 */
public class ChatMessageAnimator {
    
    /**
     * Отправляет анимированное сообщение с эффектом печатания
     */
    public static void sendAnimatedMessage(ServerCommandSource source, String message, Formatting color) {
        // Сначала показываем индикатор печатания
        sendTypingIndicator(source, "AI думает");
        
        // Через 1 секунду показываем ответ
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
            try {
                // Определяем тип сообщения и форматируем соответственно
                Text formattedMessage;
                if (message.contains("рецепт") || message.contains("крафт")) {
                    formattedMessage = formatCraftingRecipe(message);
                } else if (message.contains("механика") || message.contains("работает")) {
                    formattedMessage = formatGameMechanic(message);
                } else {
                    formattedMessage = Text.literal("🤖 " + message).formatted(color);
                }
                
                source.sendFeedback(() -> formattedMessage, false);
                
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при отправке анимированного сообщения", e);
                source.sendFeedback(() -> Text.literal("🤖 " + message).formatted(color), false);
            }
        });
    }
    
    /**
     * Показывает индикатор печатания
     */
    public static void sendTypingIndicator(ServerCommandSource source, String indicator) {
        source.sendFeedback(() -> Text.literal("🤖 " + indicator + "...")
            .formatted(Formatting.GRAY), false);
    }
    
    /**
     * Отправляет многочастное сообщение с задержками
     */
    public static void sendMultipartMessage(ServerCommandSource source, List<String> parts, int delayTicks) {
        if (parts.isEmpty()) return;
        
        // Отправляем первую часть сразу
        source.sendFeedback(() -> Text.literal("🤖 " + parts.get(0))
            .formatted(Formatting.AQUA), false);
        
        // Отправляем остальные части с задержкой
        for (int i = 1; i < parts.size(); i++) {
            final int partIndex = i;
            final String part = parts.get(i);
            
            CompletableFuture.delayedExecutor(delayTicks * i * 50, TimeUnit.MILLISECONDS).execute(() -> {
                try {
                    source.sendFeedback(() -> Text.literal("   " + part)
                        .formatted(Formatting.WHITE), false);
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при отправке части сообщения " + partIndex, e);
                }
            });
        }
    }
    
    /**
     * Форматирует рецепт крафта для красивого отображения
     */
    private static Text formatCraftingRecipe(String recipe) {
        Text baseText = Text.literal("🔨 ").formatted(Formatting.GOLD);
        
        // Разбиваем рецепт на строки и форматируем каждую
        String[] lines = recipe.split("\n");
        Text result = baseText;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            if (i == 0) {
                // Первая строка - заголовок
                result = result.copy().append(Text.literal(line).formatted(Formatting.YELLOW));
            } else if (line.contains("→") || line.contains("=")) {
                // Строка с результатом крафта
                result = result.copy().append(Text.literal("\n   ").formatted(Formatting.WHITE))
                    .append(Text.literal(line).formatted(Formatting.GREEN));
            } else {
                // Обычная строка рецепта
                result = result.copy().append(Text.literal("\n   ").formatted(Formatting.WHITE))
                    .append(Text.literal(line).formatted(Formatting.AQUA));
            }
        }
        
        return result;
    }
    
    /**
     * Форматирует объяснение игровой механики
     */
    private static Text formatGameMechanic(String mechanic) {
        Text baseText = Text.literal("⚙️ ").formatted(Formatting.BLUE);
        
        String[] lines = mechanic.split("\n");
        Text result = baseText;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            if (i == 0) {
                // Первая строка - заголовок
                result = result.copy().append(Text.literal(line).formatted(Formatting.AQUA));
            } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                // Пункт списка
                result = result.copy().append(Text.literal("\n   ").formatted(Formatting.WHITE))
                    .append(Text.literal(line).formatted(Formatting.YELLOW));
            } else {
                // Обычная строка объяснения
                result = result.copy().append(Text.literal("\n   ").formatted(Formatting.WHITE))
                    .append(Text.literal(line).formatted(Formatting.WHITE));
            }
        }
        
        return result;
    }
    
    /**
     * Отправляет сообщение об ошибке с соответствующим форматированием
     */
    public static void sendErrorMessage(ServerCommandSource source, String errorMessage) {
        source.sendFeedback(() -> Text.literal("❌ ").formatted(Formatting.RED)
            .append(Text.literal(errorMessage).formatted(Formatting.GRAY)), false);
    }
    
    /**
     * Отправляет сообщение о загрузке
     */
    public static void sendLoadingMessage(ServerCommandSource source, String loadingText) {
        source.sendFeedback(() -> Text.literal("⏳ ").formatted(Formatting.YELLOW)
            .append(Text.literal(loadingText).formatted(Formatting.GRAY)), false);
    }
    
    /**
     * Разбивает длинное сообщение на части для отображения
     */
    public static List<String> splitLongMessage(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return List.of(message);
        }
        
        java.util.List<String> parts = new java.util.ArrayList<>();
        String[] words = message.split(" ");
        StringBuilder currentPart = new StringBuilder();
        
        for (String word : words) {
            if (currentPart.length() + word.length() + 1 > maxLength) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString().trim());
                    currentPart = new StringBuilder();
                }
            }
            
            if (currentPart.length() > 0) {
                currentPart.append(" ");
            }
            currentPart.append(word);
        }
        
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString().trim());
        }
        
        return parts;
    }
}