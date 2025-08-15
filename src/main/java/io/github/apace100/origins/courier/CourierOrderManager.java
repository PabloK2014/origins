package io.github.apace100.origins.courier;

import io.github.apace100.origins.Origins;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Менеджер заказов курьера на серверной стороне
 * Управляет жизненным циклом заказов и их сохранением
 */
public class CourierOrderManager extends PersistentState {
    
    private static final String KEY = "origins:courier_orders";
    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerOrders = new ConcurrentHashMap<>(); // UUID игрока -> Set<UUID заказов>
    
    public CourierOrderManager() {
        super();
    }
    
    /**
     * Получает экземпляр менеджера для мира
     */
    public static CourierOrderManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            CourierOrderManager::fromNbt, 
            CourierOrderManager::new, 
            KEY
        );
    }
    
    /**
     * Создает менеджер из NBT данных
     */
    public static CourierOrderManager fromNbt(NbtCompound nbt) {
        CourierOrderManager manager = new CourierOrderManager();
        
        NbtList ordersList = nbt.getList("orders", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < ordersList.size(); i++) {
            try {
                Order order = Order.fromNbt(ordersList.getCompound(i));
                manager.orders.put(order.getId(), order);
                
                // Восстанавливаем индекс заказов по игрокам
                manager.playerOrders.computeIfAbsent(order.getOwnerUuid(), k -> new HashSet<>())
                    .add(order.getId());
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при загрузке заказа: " + e.getMessage());
            }
        }
        
        Origins.LOGGER.info("Загружено {} заказов курьера", manager.orders.size());
        return manager;
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList ordersList = new NbtList();
        
        for (Order order : orders.values()) {
            try {
                ordersList.add(order.toNbt());
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при сохранении заказа {}: {}", order.getId(), e.getMessage());
            }
        }
        
        nbt.put("orders", ordersList);
        Origins.LOGGER.debug("Сохранено {} заказов курьера", orders.size());
        return nbt;
    }
    
    /**
     * Создает новый заказ
     */
    public boolean createOrder(Order order) {
        if (order == null || !order.isValid()) {
            return false;
        }
        
        // Проверяем лимит заказов на игрока
        UUID ownerUuid = order.getOwnerUuid();
        Set<UUID> playerOrderIds = playerOrders.computeIfAbsent(ownerUuid, k -> new HashSet<>());
        
        // Удаляем завершенные/отмененные заказы из счетчика
        playerOrderIds.removeIf(orderId -> {
            Order existingOrder = orders.get(orderId);
            return existingOrder == null || 
                   existingOrder.getStatus() == Order.Status.COMPLETED ||
                   existingOrder.getStatus() == Order.Status.CANCELLED ||
                   existingOrder.getStatus() == Order.Status.DECLINED;
        });
        
        if (playerOrderIds.size() >= CourierNetworking.MAX_ORDERS_PER_PLAYER) {
            return false;
        }
        
        orders.put(order.getId(), order);
        playerOrderIds.add(order.getId());
        markDirty();
        
        Origins.LOGGER.info("Создан новый заказ {} от игрока {}", order.getId(), order.getOwnerName());
        return true;
    }
    
    /**
     * Принимает заказ курьером
     */
    public boolean acceptOrder(UUID orderId, ServerPlayerEntity courier) {
        Order order = orders.get(orderId);
        if (order == null || !order.canBeAcceptedBy(courier)) {
            return false;
        }
        
        order.setAcceptedBy(courier.getName().getString(), courier.getUuid());
        order.setStatus(Order.Status.ACCEPTED);
        markDirty();
        
        Origins.LOGGER.info("Заказ {} принят курьером {}", orderId, courier.getName().getString());
        return true;
    }
    
    /**
     * Отклоняет заказ курьером
     */
    public boolean declineOrder(UUID orderId, ServerPlayerEntity courier) {
        Order order = orders.get(orderId);
        if (order == null || order.getStatus() != Order.Status.OPEN) {
            return false;
        }
        
        order.setStatus(Order.Status.DECLINED);
        markDirty();
        
        Origins.LOGGER.info("Заказ {} отклонен курьером {}", orderId, courier.getName().getString());
        return true;
    }
    
    /**
     * Завершает заказ
     */
    public boolean completeOrder(UUID orderId) {
        Order order = orders.get(orderId);
        if (order == null || (order.getStatus() != Order.Status.ACCEPTED && order.getStatus() != Order.Status.IN_PROGRESS)) {
            return false;
        }
        
        order.setCompleted();
        markDirty();
        
        Origins.LOGGER.info("Заказ {} завершен", orderId);
        return true;
    }
    
    /**
     * Отменяет заказ (может сделать только владелец или админ)
     */
    public boolean cancelOrder(UUID orderId, ServerPlayerEntity player) {
        Order order = orders.get(orderId);
        if (order == null) {
            return false;
        }
        
        // Проверяем права на отмену
        boolean canCancel = order.getOwnerUuid().equals(player.getUuid()) || 
                           CourierUtils.isAdmin(player);
        
        if (!canCancel) {
            return false;
        }
        
        order.setStatus(Order.Status.CANCELLED);
        markDirty();
        
        Origins.LOGGER.info("Заказ {} отменен игроком {}", orderId, player.getName().getString());
        return true;
    }
    
    /**
     * Получает заказ по ID
     */
    public Order getOrder(UUID orderId) {
        return orders.get(orderId);
    }
    
    /**
     * Получает все активные заказы (открытые)
     */
    public List<Order> getActiveOrders() {
        return orders.values().stream()
            .filter(order -> order.getStatus() == Order.Status.OPEN)
            .filter(order -> !CourierNetworking.isOrderExpired(order.getCreatedTime()))
            .sorted(Comparator.comparing(Order::getCreatedTime).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Получает все заказы
     */
    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
    
    /**
     * Получает заказы конкретного игрока
     */
    public List<Order> getOrdersByPlayer(UUID playerUuid) {
        Set<UUID> playerOrderIds = playerOrders.get(playerUuid);
        if (playerOrderIds == null) {
            return new ArrayList<>();
        }
        
        return playerOrderIds.stream()
            .map(orders::get)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(Order::getCreatedTime).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Получает заказы, принятые конкретным курьером
     */
    public List<Order> getOrdersAcceptedBy(UUID courierUuid) {
        return orders.values().stream()
            .filter(order -> courierUuid.equals(order.getAcceptedByUuid()))
            .sorted(Comparator.comparing(Order::getAcceptedTime).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Получает статистику заказов
     */
    public OrderStatistics getStatistics() {
        int total = orders.size();
        int open = 0;
        int accepted = 0;
        int completed = 0;
        int declined = 0;
        int cancelled = 0;
        
        for (Order order : orders.values()) {
            switch (order.getStatus()) {
                case OPEN -> open++;
                case ACCEPTED, IN_PROGRESS -> accepted++;
                case COMPLETED -> completed++;
                case DECLINED -> declined++;
                case CANCELLED -> cancelled++;
            }
        }
        
        return new OrderStatistics(total, open, accepted, completed, declined, cancelled);
    }
    
    /**
     * Очищает старые заказы
     */
    public int cleanupExpiredOrders() {
        int removed = 0;
        Iterator<Map.Entry<UUID, Order>> iterator = orders.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, Order> entry = iterator.next();
            Order order = entry.getValue();
            
            // Удаляем истекшие заказы или завершенные/отмененные заказы старше недели
            boolean shouldRemove = CourierNetworking.isOrderExpired(order.getCreatedTime()) ||
                (order.getStatus() == Order.Status.COMPLETED || 
                 order.getStatus() == Order.Status.CANCELLED ||
                 order.getStatus() == Order.Status.DECLINED) &&
                (System.currentTimeMillis() - order.getCreatedTime() > 7 * 24 * 60 * 60 * 1000L);
            
            if (shouldRemove) {
                iterator.remove();
                
                // Удаляем из индекса игрока
                Set<UUID> playerOrderIds = playerOrders.get(order.getOwnerUuid());
                if (playerOrderIds != null) {
                    playerOrderIds.remove(order.getId());
                    if (playerOrderIds.isEmpty()) {
                        playerOrders.remove(order.getOwnerUuid());
                    }
                }
                
                removed++;
            }
        }
        
        if (removed > 0) {
            markDirty();
            Origins.LOGGER.info("Очищено {} старых заказов", removed);
        }
        
        return removed;
    }
    
    /**
     * Удаляет все заказы (только для админов)
     */
    public void clearAllOrders() {
        int count = orders.size();
        orders.clear();
        playerOrders.clear();
        markDirty();
        
        Origins.LOGGER.info("Удалено {} заказов администратором", count);
    }
    
    /**
     * Проверяет, может ли игрок создать новый заказ
     */
    public boolean canPlayerCreateOrder(UUID playerUuid) {
        Set<UUID> playerOrderIds = playerOrders.get(playerUuid);
        if (playerOrderIds == null) {
            return true;
        }
        
        // Считаем только активные заказы
        long activeOrders = playerOrderIds.stream()
            .map(orders::get)
            .filter(Objects::nonNull)
            .filter(order -> order.getStatus() == Order.Status.OPEN || 
                           order.getStatus() == Order.Status.ACCEPTED ||
                           order.getStatus() == Order.Status.IN_PROGRESS)
            .count();
        
        return activeOrders < CourierNetworking.MAX_ORDERS_PER_PLAYER;
    }
    
    /**
     * Статистика заказов
     */
    public static class OrderStatistics {
        public final int total;
        public final int open;
        public final int accepted;
        public final int completed;
        public final int declined;
        public final int cancelled;
        
        public OrderStatistics(int total, int open, int accepted, int completed, int declined, int cancelled) {
            this.total = total;
            this.open = open;
            this.accepted = accepted;
            this.completed = completed;
            this.declined = declined;
            this.cancelled = cancelled;
        }
        
        @Override
        public String toString() {
            return String.format("Статистика заказов: Всего=%d, Открыто=%d, Принято=%d, Завершено=%d, Отклонено=%d, Отменено=%d",
                               total, open, accepted, completed, declined, cancelled);
        }
    }
}