package io.github.apace100.origins.courier;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Утилиты для работы с системой заказов курьера
 */
public class CourierUtils {
    
    /**
     * Получает класс игрока на основе его происхождения в Origins
     */
    public static String getPlayerClass(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return "human";
        }
        
        try {
            var originComponent = ModComponents.ORIGIN.get(serverPlayer);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(
                    OriginLayers.getLayer(Origins.identifier("origin")));
                if (origin != null) {
                    String originId = origin.getIdentifier().toString();
                    return switch (originId) {
                        case "origins:courier" -> "courier";
                        case "origins:warrior" -> "warrior";
                        case "origins:miner" -> "miner";
                        case "origins:blacksmith" -> "blacksmith";
                        case "origins:brewer" -> "brewer";
                        case "origins:cook" -> "cook";
                        default -> "human";
                    };
                }
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при получении класса игрока: " + e.getMessage());
        }
        return "human";
    }
    
    /**
     * Проверяет, является ли игрок курьером
     */
    public static boolean isCourier(PlayerEntity player) {
        return "courier".equals(getPlayerClass(player));
    }
    
    /**
     * Проверяет, может ли игрок создавать заказы (не курьер)
     */
    public static boolean canCreateOrders(PlayerEntity player) {
        return !isCourier(player);
    }
    
    /**
     * Получает локализованное название класса игрока
     */
    public static String getLocalizedClassName(String playerClass) {
        return switch (playerClass) {
            case "courier" -> "Курьер";
            case "warrior" -> "Воин";
            case "miner" -> "Шахтер";
            case "blacksmith" -> "Кузнец";
            case "brewer" -> "Пивовар";
            case "cook" -> "Повар";
            case "human" -> "Человек";
            default -> "Неизвестный";
        };
    }
    
    /**
     * Проверяет, имеет ли игрок права администратора
     */
    public static boolean isAdmin(ServerPlayerEntity player) {
        return player.hasPermissionLevel(2);
    }
    
    /**
     * Безопасно получает имя игрока
     */
    public static String getPlayerName(PlayerEntity player) {
        return player != null ? player.getName().getString() : "Unknown";
    }
    
    /**
     * Проверяет валидность UUID
     */
    public static boolean isValidUUID(java.util.UUID uuid) {
        return uuid != null && !uuid.equals(new java.util.UUID(0, 0));
    }
}