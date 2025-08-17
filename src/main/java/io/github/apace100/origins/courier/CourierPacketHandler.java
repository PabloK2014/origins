package io.github.apace100.origins.courier;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Обработчик сетевых пакетов для системы заказов курьера
 */
public class CourierPacketHandler {
    
    /**
     * Регистрирует все серверные обработчики пакетов
     */
    public static void registerServerHandlers() {
        // Создание заказа
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.CREATE_ORDER, 
            CourierPacketHandler::handleCreateOrder);
        
        // Принятие заказа
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.ACCEPT_ORDER, 
            CourierPacketHandler::handleAcceptOrder);
        
        // Отклонение заказа
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.DECLINE_ORDER, 
            CourierPacketHandler::handleDeclineOrder);
        
        // Завершение заказа
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.COMPLETE_ORDER, 
            CourierPacketHandler::handleCompleteOrder);
        
        // Отмена заказа
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.CANCEL_ORDER, 
            CourierPacketHandler::handleCancelOrder);
        
        // Удаление заказа
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.DELETE_ORDER, 
            CourierPacketHandler::handleDeleteOrder);
        
        // Запрос синхронизации заказов
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.REQUEST_ORDERS_SYNC, 
            CourierPacketHandler::handleRequestOrdersSync);
        
            }
    
    /**
     * Обработка создания заказа
     */
    private static void handleCreateOrder(net.minecraft.server.MinecraftServer server, 
                                        ServerPlayerEntity player, 
                                        net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                        PacketByteBuf buf,
                                        net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        try {
            // Проверяем, может ли игрок создавать заказы
            // Курьеры не могут создавать заказы - они их принимают и выполняют
            if (CourierUtils.isCourier(player)) {
                player.sendMessage(Text.literal("Курьеры не могут создавать заказы! Вы можете только принимать и выполнять заказы других игроков.")
                    .formatted(Formatting.RED), false);
                return;
            }
            
            // Читаем данные из пакета
            String description = buf.readString(CourierNetworking.MAX_DESCRIPTION_LENGTH);
            
            // Читаем опыт для курьера
            int experienceReward = buf.readInt();
            // Ограничиваем значение
            final int finalExperienceReward = Math.max(0, Math.min(experienceReward, 10000));
            
            // Читаем предметы запроса
            int requestItemsCount = buf.readInt();
            List<ItemStack> requestItems = new ArrayList<>();
            for (int i = 0; i < Math.min(requestItemsCount, CourierNetworking.MAX_ITEMS_PER_CATEGORY); i++) {
                ItemStack item = buf.readItemStack();
                if (!item.isEmpty()) {
                    requestItems.add(item);
                }
            }
            
            // Читаем предметы награды
            int rewardItemsCount = buf.readInt();
            List<ItemStack> rewardItems = new ArrayList<>();
            for (int i = 0; i < Math.min(rewardItemsCount, CourierNetworking.MAX_ITEMS_PER_CATEGORY); i++) {
                ItemStack item = buf.readItemStack();
                if (!item.isEmpty()) {
                    rewardItems.add(item);
                }
            }
            
            server.execute(() -> {
                try {
                    ServerWorld world = player.getServerWorld();
                    CourierOrderManager manager = CourierOrderManager.get(world);
                    
                    // Проверяем, может ли игрок создать новый заказ
                    if (!manager.canPlayerCreateOrder(player.getUuid())) {
                        player.sendMessage(Text.literal("Вы достигли лимита активных заказов!")
                            .formatted(Formatting.RED), false);
                        return;
                    }
                    
                    // Проверяем, есть ли у игрока предметы для награды
                    if (!CourierUtils.hasRequiredItems(player, rewardItems)) {
                        player.sendMessage(Text.literal("У вас нет необходимых предметов для награды!")
                            .formatted(Formatting.RED), false);
                        return;
                    }
                    
                    // Забираем предметы награды у заказчика
                    if (!CourierUtils.takeItemsFromPlayer(player, rewardItems)) {
                        player.sendMessage(Text.literal("Не удалось забрать предметы награды из инвентаря!")
                            .formatted(Formatting.RED), false);
                        return;
                    }
                    
                    // Создаем заказ
                    Order order = new Order(UUID.randomUUID(), player.getName().getString(), player.getUuid());
                    order.setDescription(CourierNetworking.truncateDescription(description));
                    order.setExperienceReward(finalExperienceReward);
                    
                    // Добавляем предметы
                    for (ItemStack item : requestItems) {
                        order.addRequestItem(item);
                    }
                    for (ItemStack item : rewardItems) {
                        order.addRewardItem(item);
                    }
                    
                    // Валидируем и сохраняем заказ
                    if (order.isValid() && manager.createOrder(order)) {
                        player.sendMessage(Text.literal("Заказ успешно создан! Предметы награды забраны из вашего инвентаря и будут переданы курьеру при выполнении заказа.")
                            .formatted(Formatting.GREEN), false);
                        
                        // Уведомляем всех курьеров о новом заказе
                        notifyNewOrder(world, order);
                        
                                            } else {
                        player.sendMessage(Text.literal("Ошибка при создании заказа. Проверьте корректность данных.")
                            .formatted(Formatting.RED), false);
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при создании заказа: " + e.getMessage(), e);
                    player.sendMessage(Text.literal("Произошла ошибка при создании заказа.")
                        .formatted(Formatting.RED), false);
                }
            });
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке пакета создания заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обработка принятия заказа
     */
    private static void handleAcceptOrder(net.minecraft.server.MinecraftServer server, 
                                        ServerPlayerEntity player, 
                                        net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                        PacketByteBuf buf,
                                        net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        try {
            UUID orderId = buf.readUuid();
            
            server.execute(() -> {
                try {
                    ServerWorld world = player.getServerWorld();
                    CourierOrderManager manager = CourierOrderManager.get(world);
                    
                    if (manager.acceptOrder(orderId, player)) {
                        player.sendMessage(Text.literal("Заказ принят!")
                            .formatted(Formatting.GREEN), false);
                        
                        // Уведомляем об изменении статуса
                        notifyOrderStatusUpdate(world, orderId);
                    } else {
                        player.sendMessage(Text.literal("Не удалось принять заказ.")
                            .formatted(Formatting.RED), false);
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при принятии заказа: " + e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке пакета принятия заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обработка отклонения заказа
     */
    private static void handleDeclineOrder(net.minecraft.server.MinecraftServer server, 
                                         ServerPlayerEntity player, 
                                         net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                         PacketByteBuf buf,
                                         net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        try {
            UUID orderId = buf.readUuid();
            
            server.execute(() -> {
                try {
                    ServerWorld world = player.getServerWorld();
                    CourierOrderManager manager = CourierOrderManager.get(world);
                    
                    if (manager.declineOrder(orderId, player)) {
                        player.sendMessage(Text.literal("Заказ отклонен.")
                            .formatted(Formatting.YELLOW), false);
                        
                        // Уведомляем об изменении статуса
                        notifyOrderStatusUpdate(world, orderId);
                    } else {
                        player.sendMessage(Text.literal("Не удалось отклонить заказ.")
                            .formatted(Formatting.RED), false);
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при отклонении заказа: " + e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке пакета отклонения заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обработка завершения заказа
     */
    private static void handleCompleteOrder(net.minecraft.server.MinecraftServer server, 
                                          ServerPlayerEntity player, 
                                          net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                          PacketByteBuf buf,
                                          net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        try {
            UUID orderId = buf.readUuid();
            
            server.execute(() -> {
                try {
                    ServerWorld world = player.getServerWorld();
                    CourierOrderManager manager = CourierOrderManager.get(world);
                    
                    if (manager.completeOrder(orderId, player)) {
                        player.sendMessage(Text.literal("Заказ завершен! Вы получили награду.")
                            .formatted(Formatting.GREEN), false);
                        
                        // Уведомляем об изменении статуса
                        notifyOrderStatusUpdate(world, orderId);
                    } else {
                        player.sendMessage(Text.literal("Не удалось завершить заказ.")
                            .formatted(Formatting.RED), false);
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при завершении заказа: " + e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке пакета завершения заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обработка отмены заказа
     */
    private static void handleCancelOrder(net.minecraft.server.MinecraftServer server, 
                                        ServerPlayerEntity player, 
                                        net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                        PacketByteBuf buf,
                                        net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        try {
            UUID orderId = buf.readUuid();
            
            server.execute(() -> {
                try {
                    ServerWorld world = player.getServerWorld();
                    CourierOrderManager manager = CourierOrderManager.get(world);
                    
                    if (manager.cancelOrder(orderId, player)) {
                        player.sendMessage(Text.literal("Заказ отменен.")
                            .formatted(Formatting.YELLOW), false);
                        
                        // Уведомляем об изменении статуса
                        notifyOrderStatusUpdate(world, orderId);
                    } else {
                        player.sendMessage(Text.literal("Не удалось отменить заказ.")
                            .formatted(Formatting.RED), false);
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при отмене заказа: " + e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке пакета отмены заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обработка удаления заказа
     */
    private static void handleDeleteOrder(net.minecraft.server.MinecraftServer server, 
                                        ServerPlayerEntity player, 
                                        net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                        PacketByteBuf buf,
                                        net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        try {
            UUID orderId = buf.readUuid();
            
            server.execute(() -> {
                try {
                    ServerWorld world = player.getServerWorld();
                    CourierOrderManager manager = CourierOrderManager.get(world);
                    
                    if (manager.deleteOrder(orderId, player)) {
                        player.sendMessage(Text.literal("Заказ удален.")
                            .formatted(Formatting.YELLOW), false);
                        
                        // Уведомляем об изменении статуса
                        notifyOrderStatusUpdate(world, orderId);
                    } else {
                        player.sendMessage(Text.literal("Не удалось удалить заказ.")
                            .formatted(Formatting.RED), false);
                    }
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при удалении заказа: " + e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке пакета удаления заказа: " + e.getMessage(), e);
        }
    }
    
    /**
     * Обработка запроса синхронизации заказов
     */
    private static void handleRequestOrdersSync(net.minecraft.server.MinecraftServer server, 
                                              ServerPlayerEntity player, 
                                              net.minecraft.server.network.ServerPlayNetworkHandler handler, 
                                              PacketByteBuf buf,
                                              net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        server.execute(() -> {
            try {
                ServerWorld world = player.getServerWorld();
                CourierOrderManager manager = CourierOrderManager.get(world);
                
                List<Order> orders;
                String playerClass = CourierUtils.getPlayerClass(player);
                
                if (CourierUtils.isCourier(player)) {
                    // Курьеры видят все открытые заказы + свои принятые заказы
                    orders = manager.getAllOrders().stream()
                        .filter(order -> order.getStatus() == Order.Status.OPEN || 
                                        (order.getAcceptedByUuid() != null && order.getAcceptedByUuid().equals(player.getUuid())))
                        .collect(java.util.stream.Collectors.toList());
                } else {
                    // Другие игроки видят только свои заказы
                    orders = manager.getOrdersByPlayer(player.getUuid());
                }
                
                Origins.LOGGER.info("Игрок {} (класс: {}) получает {} заказов", 
                    player.getName().getString(), playerClass, orders.size());
                
                sendOrdersToPlayer(player, orders);
                
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при синхронизации заказов: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Отправляет список заказов игроку
     */
    public static void sendOrdersToPlayer(ServerPlayerEntity player, List<Order> orders) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            
            buf.writeInt(orders.size());
            for (Order order : orders) {
                buf.writeNbt(order.toNbt());
            }
            
            ServerPlayNetworking.send(player, CourierNetworking.SYNC_ORDERS, buf);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при отправке заказов игроку {}: {}", 
                               player.getName().getString(), e.getMessage(), e);
        }
    }
    
    /**
     * Уведомляет всех курьеров о новом заказе
     */
    public static void notifyNewOrder(ServerWorld world, Order order) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(order.getId());
            buf.writeString(order.getOwnerName());
            buf.writeString(order.getShortDescription());
            
            // Отправляем всем курьерам
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (CourierUtils.isCourier(player)) {
                    ServerPlayNetworking.send(player, CourierNetworking.NEW_ORDER_NOTIFY, buf);
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при уведомлении о новом заказе: " + e.getMessage(), e);
        }
    }
    
    /**
     * Уведомляет об изменении статуса заказа
     */
    public static void notifyOrderStatusUpdate(ServerWorld world, UUID orderId) {
        try {
            CourierOrderManager manager = CourierOrderManager.get(world);
            Order order = manager.getOrder(orderId);
            if (order == null) return;
            
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(orderId);
            buf.writeString(order.getStatus().name());
            
            // Отправляем всем игрокам (они сами решат, нужно ли им это обновление)
            for (ServerPlayerEntity player : world.getPlayers()) {
                ServerPlayNetworking.send(player, CourierNetworking.ORDER_STATUS_UPDATE, buf);
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при уведомлении об изменении статуса заказа: " + e.getMessage(), e);
        }
    }
}