package io.github.apace100.origins.courier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/**
 * Административные команды для системы заказов курьера
 */
public class CourierCommand {
    
    /**
     * Регистрирует команды курьера
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("courier")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("orders")
                .then(CommandManager.literal("list")
                    .executes(CourierCommand::listOrders))
                .then(CommandManager.literal("stats")
                    .executes(CourierCommand::showStats))
                .then(CommandManager.literal("clear")
                    .executes(CourierCommand::clearAllOrders))
                .then(CommandManager.literal("cleanup")
                    .executes(CourierCommand::cleanupExpiredOrders))
                .then(CommandManager.literal("delete")
                    .then(CommandManager.argument("orderId", StringArgumentType.string())
                        .executes(CourierCommand::deleteOrder)))
                .then(CommandManager.literal("complete")
                    .then(CommandManager.argument("orderId", StringArgumentType.string())
                        .executes(CourierCommand::forceCompleteOrder)))
                .then(CommandManager.literal("player")
                    .then(CommandManager.argument("playerName", StringArgumentType.string())
                        .executes(CourierCommand::showPlayerOrders))))
            .then(CommandManager.literal("reload")
                .executes(CourierCommand::reloadSystem)));
    }
    
    /**
     * Показывает список всех заказов
     */
    private static int listOrders(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            List<Order> orders = manager.getAllOrders();
            
            if (orders.isEmpty()) {
                source.sendFeedback(() -> Text.literal("Заказов нет").formatted(Formatting.YELLOW), false);
                return 0;
            }
            
            source.sendFeedback(() -> Text.literal("=== Список заказов ===").formatted(Formatting.GOLD), false);
            
            for (Order order : orders) {
                Text orderInfo = Text.literal(String.format("ID: %s | Владелец: %s | Статус: %s | Создан: %s",
                    order.getId().toString().substring(0, 8),
                    order.getOwnerName(),
                    order.getStatus().name(),
                    CourierNetworking.formatOrderTime(order.getCreatedTime())
                )).formatted(getStatusFormatting(order.getStatus()));
                
                source.sendFeedback(() -> orderInfo, false);
                
                if (order.getAcceptedByName() != null) {
                    Text acceptedInfo = Text.literal("  Принят: " + order.getAcceptedByName())
                        .formatted(Formatting.GRAY);
                    source.sendFeedback(() -> acceptedInfo, false);
                }
            }
            
            source.sendFeedback(() -> Text.literal("Всего заказов: " + orders.size()).formatted(Formatting.AQUA), false);
            return orders.size();
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды listOrders: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при получении списка заказов: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Показывает статистику заказов
     */
    private static int showStats(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            CourierOrderManager.OrderStatistics stats = manager.getStatistics();
            
            source.sendFeedback(() -> Text.literal("=== Статистика заказов ===").formatted(Formatting.GOLD), false);
            source.sendFeedback(() -> Text.literal("Всего заказов: " + stats.total).formatted(Formatting.WHITE), false);
            source.sendFeedback(() -> Text.literal("Открыто: " + stats.open).formatted(Formatting.GREEN), false);
            source.sendFeedback(() -> Text.literal("Принято: " + stats.accepted).formatted(Formatting.BLUE), false);
            source.sendFeedback(() -> Text.literal("Завершено: " + stats.completed).formatted(Formatting.AQUA), false);
            source.sendFeedback(() -> Text.literal("Отклонено: " + stats.declined).formatted(Formatting.RED), false);
            source.sendFeedback(() -> Text.literal("Отменено: " + stats.cancelled).formatted(Formatting.GRAY), false);
            
            return stats.total;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды showStats: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при получении статистики: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Очищает все заказы
     */
    private static int clearAllOrders(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            
            manager.clearAllOrders();
            
            source.sendFeedback(() -> Text.literal("Все заказы удалены").formatted(Formatting.GREEN), true);
            Origins.LOGGER.info("Администратор {} очистил все заказы", source.getName());
            
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды clearAllOrders: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при очистке заказов: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Очищает истекшие заказы
     */
    private static int cleanupExpiredOrders(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            
            int removed = manager.cleanupExpiredOrders();
            
            if (removed > 0) {
                source.sendFeedback(() -> Text.literal("Удалено истекших заказов: " + removed).formatted(Formatting.GREEN), true);
            } else {
                source.sendFeedback(() -> Text.literal("Истекших заказов не найдено").formatted(Formatting.YELLOW), false);
            }
            
            return removed;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды cleanupExpiredOrders: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при очистке истекших заказов: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Удаляет конкретный заказ
     */
    private static int deleteOrder(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String orderIdStr = StringArgumentType.getString(context, "orderId");
        
        try {
            UUID orderId = UUID.fromString(orderIdStr);
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            
            Order order = manager.getOrder(orderId);
            if (order == null) {
                source.sendError(Text.literal("Заказ с ID " + orderIdStr + " не найден"));
                return 0;
            }
            
            // Принудительно отменяем заказ
            order.setStatus(Order.Status.CANCELLED);
            
            source.sendFeedback(() -> Text.literal("Заказ " + orderIdStr + " удален").formatted(Formatting.GREEN), true);
            Origins.LOGGER.info("Администратор {} удалил заказ {}", source.getName(), orderId);
            
            return 1;
            
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Неверный формат UUID: " + orderIdStr));
            return 0;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды deleteOrder: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при удалении заказа: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Принудительно завершает заказ
     */
    private static int forceCompleteOrder(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String orderIdStr = StringArgumentType.getString(context, "orderId");
        
        try {
            UUID orderId = UUID.fromString(orderIdStr);
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            
            if (manager.completeOrder(orderId)) {
                source.sendFeedback(() -> Text.literal("Заказ " + orderIdStr + " принудительно завершен").formatted(Formatting.GREEN), true);
                Origins.LOGGER.info("Администратор {} принудительно завершил заказ {}", source.getName(), orderId);
                return 1;
            } else {
                source.sendError(Text.literal("Не удалось завершить заказ " + orderIdStr));
                return 0;
            }
            
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Неверный формат UUID: " + orderIdStr));
            return 0;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды forceCompleteOrder: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при завершении заказа: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Показывает заказы конкретного игрока
     */
    private static int showPlayerOrders(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerName = StringArgumentType.getString(context, "playerName");
        
        try {
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            
            // Ищем игрока по имени
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerName);
            if (player == null) {
                source.sendError(Text.literal("Игрок " + playerName + " не найден"));
                return 0;
            }
            
            List<Order> playerOrders = manager.getOrdersByPlayer(player.getUuid());
            
            if (playerOrders.isEmpty()) {
                source.sendFeedback(() -> Text.literal("У игрока " + playerName + " нет заказов").formatted(Formatting.YELLOW), false);
                return 0;
            }
            
            source.sendFeedback(() -> Text.literal("=== Заказы игрока " + playerName + " ===").formatted(Formatting.GOLD), false);
            
            for (Order order : playerOrders) {
                Text orderInfo = Text.literal(String.format("ID: %s | Статус: %s | Создан: %s",
                    order.getId().toString().substring(0, 8),
                    order.getStatus().name(),
                    CourierNetworking.formatOrderTime(order.getCreatedTime())
                )).formatted(getStatusFormatting(order.getStatus()));
                
                source.sendFeedback(() -> orderInfo, false);
            }
            
            source.sendFeedback(() -> Text.literal("Всего заказов: " + playerOrders.size()).formatted(Formatting.AQUA), false);
            return playerOrders.size();
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды showPlayerOrders: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при получении заказов игрока: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Перезагружает систему заказов
     */
    private static int reloadSystem(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // Очищаем истекшие заказы
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            int cleaned = manager.cleanupExpiredOrders();
            
            source.sendFeedback(() -> Text.literal("Система заказов курьера перезагружена").formatted(Formatting.GREEN), true);
            if (cleaned > 0) {
                source.sendFeedback(() -> Text.literal("Очищено истекших заказов: " + cleaned).formatted(Formatting.YELLOW), false);
            }
            
            Origins.LOGGER.info("Администратор {} перезагрузил систему заказов курьера", source.getName());
            return 1;
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при выполнении команды reloadSystem: " + e.getMessage(), e);
            source.sendError(Text.literal("Ошибка при перезагрузке системы: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Получает форматирование для статуса заказа
     */
    private static Formatting getStatusFormatting(Order.Status status) {
        return switch (status) {
            case OPEN -> Formatting.GREEN;
            case ACCEPTED, IN_PROGRESS -> Formatting.BLUE;
            case COMPLETED -> Formatting.AQUA;
            case DECLINED -> Formatting.RED;
            case CANCELLED -> Formatting.GRAY;
        };
    }
}