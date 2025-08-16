package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.courier.ClientOrder;
import io.github.apace100.origins.courier.CourierNetworking;
import io.github.apace100.origins.courier.Order;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Клиентский менеджер заказов курьера
 * Управляет заказами на клиентской стороне и обрабатывает уведомления
 */
public class ClientOrderManager {
    
    private static ClientOrderManager instance;
    private final ConcurrentMap<UUID, ClientOrder> orders = new ConcurrentHashMap<>();
    private OrdersListScreen currentOrdersScreen = null;
    
    private ClientOrderManager() {}
    
    public static ClientOrderManager getInstance() {
        if (instance == null) {
            instance = new ClientOrderManager();
        }
        return instance;
    }
    
    /**
     * Регистрирует клиентские обработчики пакетов
     */
    public static void registerClientHandlers() {
        // Синхронизация заказов
        ClientPlayNetworking.registerGlobalReceiver(CourierNetworking.SYNC_ORDERS, 
            (client, handler, buf, responseSender) -> {
                try {
                    int orderCount = buf.readInt();
                    List<ClientOrder> newOrders = new ArrayList<>();
                    
                    for (int i = 0; i < orderCount; i++) {
                        NbtCompound orderNbt = buf.readNbt();
                        if (orderNbt != null) {
                            Order order = Order.fromNbt(orderNbt);
                            newOrders.add(ClientOrder.fromOrder(order));
                        }
                    }
                    
                    client.execute(() -> {
                        getInstance().updateOrders(newOrders);
                    });
                    
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при обработке синхронизации заказов: " + e.getMessage(), e);
                }
            });
        
        // Уведомление о новом заказе
        ClientPlayNetworking.registerGlobalReceiver(CourierNetworking.NEW_ORDER_NOTIFY, 
            (client, handler, buf, responseSender) -> {
                try {
                    UUID orderId = buf.readUuid();
                    String ownerName = buf.readString();
                    String shortDesc = buf.readString();
                    
                    client.execute(() -> {
                        getInstance().showNewOrderNotification(orderId, ownerName, shortDesc);
                    });
                    
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при обработке уведомления о новом заказе: " + e.getMessage(), e);
                }
            });
        
        // Обновление статуса заказа
        ClientPlayNetworking.registerGlobalReceiver(CourierNetworking.ORDER_STATUS_UPDATE, 
            (client, handler, buf, responseSender) -> {
                try {
                    UUID orderId = buf.readUuid();
                    String statusName = buf.readString();
                    Order.Status status = Order.Status.valueOf(statusName);
                    
                    client.execute(() -> {
                        getInstance().updateOrderStatus(orderId, status);
                    });
                    
                } catch (Exception e) {
                    Origins.LOGGER.error("Ошибка при обработке обновления статуса заказа: " + e.getMessage(), e);
                }
            });
        
        Origins.LOGGER.info("Зарегистрированы клиентские обработчики пакетов системы заказов курьера");
    }
    
    /**
     * Обновляет список заказов
     */
    public void updateOrders(List<ClientOrder> newOrders) {
        orders.clear();
        for (ClientOrder order : newOrders) {
            orders.put(order.id, order);
        }
        
        // Обновляем экран списка заказов, если он открыт
        if (currentOrdersScreen != null) {
            currentOrdersScreen.updateOrders(newOrders);
        }
        
        Origins.LOGGER.debug("Обновлено {} заказов на клиенте", newOrders.size());
    }
    
    /**
     * Обновляет статус конкретного заказа
     */
    public void updateOrderStatus(UUID orderId, Order.Status newStatus) {
        ClientOrder order = orders.get(orderId);
        if (order != null) {
            // Создаем новый объект с обновленным статусом
            ClientOrder updatedOrder = new ClientOrder(
                order.id, order.ownerName, order.description,
                order.requestItems, order.rewardItems,
                newStatus, order.acceptedByName, order.createdTime,
                order.experienceReward
            );
            
            orders.put(orderId, updatedOrder);
            
            // Обновляем экран, если он открыт
            if (currentOrdersScreen != null) {
                currentOrdersScreen.updateOrders(new ArrayList<>(orders.values()));
            }
            
            Origins.LOGGER.debug("Обновлен статус заказа {} на {}", orderId, newStatus);
        }
    }
    
    /**
     * Показывает уведомление о новом заказе
     */
    public void showNewOrderNotification(UUID orderId, String ownerName, String shortDesc) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        try {
            // Показываем toast уведомление
            ToastManager toastManager = client.getToastManager();
            Text title = Text.literal("Новый заказ от " + ownerName).formatted(Formatting.GREEN);
            Text description = shortDesc.isEmpty() ? 
                Text.literal("Нажмите, чтобы открыть список заказов") : 
                Text.literal(shortDesc);
            
            SystemToast toast = new SystemToast(SystemToast.Type.TUTORIAL_HINT, title, description);
            toastManager.add(toast);
            
            // Воспроизводим звук уведомления
            client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            
            // Отправляем сообщение в чат
            client.player.sendMessage(
                Text.literal("📦 ").formatted(Formatting.YELLOW)
                    .append(Text.literal("Новый заказ от ").formatted(Formatting.WHITE))
                    .append(Text.literal(ownerName).formatted(Formatting.AQUA))
                    .append(Text.literal("! Откройте список заказов для просмотра.").formatted(Formatting.WHITE)), 
                false
            );
            
            Origins.LOGGER.info("Показано уведомление о новом заказе {} от {}", orderId, ownerName);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при показе уведомления о новом заказе: " + e.getMessage(), e);
        }
    }
    
    /**
     * Открывает экран списка заказов
     */
    public void openOrdersScreen() {
        openOrdersScreen(null);
    }
    
    /**
     * Открывает экран списка заказов с выделением конкретного заказа
     */
    public void openOrdersScreen(UUID highlightOrderId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            currentOrdersScreen = new OrdersListScreen(highlightOrderId);
            client.setScreen(currentOrdersScreen);
        }
    }
    
    /**
     * Закрывает экран списка заказов
     */
    public void closeOrdersScreen() {
        currentOrdersScreen = null;
    }
    
    /**
     * Получает заказ по ID
     */
    public ClientOrder getOrder(UUID orderId) {
        return orders.get(orderId);
    }
    
    /**
     * Получает все заказы
     */
    public List<ClientOrder> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
    
    /**
     * Получает количество активных заказов
     */
    public int getActiveOrdersCount() {
        return (int) orders.values().stream()
            .filter(ClientOrder::isActive)
            .count();
    }
    
    /**
     * Получает количество принятых заказов
     */
    public int getAcceptedOrdersCount() {
        return (int) orders.values().stream()
            .filter(ClientOrder::isAccepted)
            .count();
    }
    
    /**
     * Очищает все заказы (при отключении от сервера)
     */
    public void clear() {
        orders.clear();
        currentOrdersScreen = null;
        Origins.LOGGER.debug("Очищены все заказы на клиенте");
    }
    
    /**
     * Проверяет, открыт ли экран заказов
     */
    public boolean isOrdersScreenOpen() {
        return currentOrdersScreen != null;
    }
    
    /**
     * Обновляет ссылку на текущий экран заказов
     */
    public void setCurrentOrdersScreen(OrdersListScreen screen) {
        this.currentOrdersScreen = screen;
    }
}