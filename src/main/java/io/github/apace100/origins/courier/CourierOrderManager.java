package io.github.apace100.origins.courier;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.profession.Profession;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.profession.ProfessionProgress;
import io.github.apace100.origins.profession.ProfessionRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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
        Origins.LOGGER.info("Загружаем {} заказов из NBT", ordersList.size());
        
        for (int i = 0; i < ordersList.size(); i++) {
            try {
                Order order = Order.fromNbt(ordersList.getCompound(i));
                manager.orders.put(order.getId(), order);
                
                // Восстанавливаем индекс заказов по игрокам
                manager.playerOrders.computeIfAbsent(order.getOwnerUuid(), k -> new HashSet<>())
                    .add(order.getId());
                    
                Origins.LOGGER.debug("Загружен заказ {}: {} от {}", order.getId(), order.getDescription(), order.getOwnerName());
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при загрузке заказа {}: {}", i, e.getMessage(), e);
            }
        }
        
        Origins.LOGGER.info("Успешно загружено {} заказов курьера", manager.orders.size());
        return manager;
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList ordersList = new NbtList();
        
        Origins.LOGGER.info("Сохраняем {} заказов в NBT", orders.size());
        
        for (Order order : orders.values()) {
            try {
                ordersList.add(order.toNbt());
                Origins.LOGGER.debug("Сохранен заказ {}: {} от {}", order.getId(), order.getDescription(), order.getOwnerName());
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при сохранении заказа {}: {}", order.getId(), e.getMessage(), e);
            }
        }
        
        nbt.put("orders", ordersList);
        Origins.LOGGER.info("Успешно сохранено {} заказов курьера", ordersList.size());
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
        
        // Принудительное сохранение
        try {
            courier.getServerWorld().getPersistentStateManager().save();
            Origins.LOGGER.info("Принудительное сохранение после принятия заказа");
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принудительном сохранении: " + e.getMessage());
        }
        
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
        
        // Принудительное сохранение
        try {
            courier.getServerWorld().getPersistentStateManager().save();
            Origins.LOGGER.info("Принудительное сохранение после отклонения заказа");
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принудительном сохранении: " + e.getMessage());
        }
        
        Origins.LOGGER.info("Заказ {} отклонен курьером {}", orderId, courier.getName().getString());
        return true;
    }
    
    /**
     * Завершает заказ
     */
    public boolean completeOrder(UUID orderId, ServerPlayerEntity player) {
        Order order = orders.get(orderId);
        if (order == null || (order.getStatus() != Order.Status.ACCEPTED && order.getStatus() != Order.Status.IN_PROGRESS)) {
            return false;
        }
        
        // Проверяем, что заказ принят этим игроком
        if (!player.getUuid().equals(order.getAcceptedByUuid())) {
            return false;
        }
        
        // Проверяем наличие необходимых предметов
        if (!CourierUtils.hasRequiredItems(player, order.getRequestItems())) {
            player.sendMessage(Text.literal("У вас нет необходимых предметов для выполнения заказа!")
                .formatted(Formatting.RED), false);
            return false;
        }
        
        // Забираем запрашиваемые предметы у курьера
        if (!CourierUtils.takeItemsFromPlayer(player, order.getRequestItems())) {
            player.sendMessage(Text.literal("Не удалось забрать предметы из инвентаря!")
                .formatted(Formatting.RED), false);
            return false;
        }
        
        // Даем награду курьеру
        CourierUtils.giveItemsToPlayer(player, order.getRewardItems());
        
        // Добавляем опыт курьеру, если указан
        if (order.getExperienceReward() > 0) {
            try {
                ProfessionComponent professionComponent = ProfessionComponent.KEY.get(player);
                if (professionComponent != null) {
                    boolean leveledUp = professionComponent.addExperience(order.getExperienceReward());
                    player.sendMessage(Text.literal("Вы получили " + order.getExperienceReward() + " опыта за выполнение заказа!")
                        .formatted(Formatting.GREEN), false);
                    
                    if (leveledUp) {
                        // Уведомляем игрока о повышении уровня
                        Identifier currentProfessionId = professionComponent.getCurrentProfessionId();
                        if (currentProfessionId != null) {
                            Profession profession = ProfessionRegistry.get(currentProfessionId);
                            if (profession != null) {
                                ProfessionProgress progress = professionComponent.getProgress(currentProfessionId);
                                player.sendMessage(
                                    Text.literal("Уровень профессии повышен! ")
                                        .append(profession.getName())
                                        .append(Text.literal(" достиг уровня " + progress.getLevel()))
                                        .formatted(Formatting.GREEN, Formatting.BOLD),
                                    false
                                );
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Origins.LOGGER.error("Ошибка при начислении опыта курьеру: " + e.getMessage(), e);
            }
        }
        
        // Находим заказчика и даем ему запрашиваемые предметы
        ServerPlayerEntity orderOwner = player.getServer().getPlayerManager().getPlayer(order.getOwnerUuid());
        if (orderOwner != null) {
            CourierUtils.giveItemsToPlayer(orderOwner, order.getRequestItems());
            orderOwner.sendMessage(Text.literal("Ваш заказ выполнен! Курьер " + player.getName().getString() + " доставил вам предметы.")
                .formatted(Formatting.GREEN), false);
        } else {
            // Если заказчик не онлайн, возвращаем предметы курьеру
            CourierUtils.giveItemsToPlayer(player, order.getRequestItems());
            player.sendMessage(Text.literal("Заказчик не в сети. Предметы возвращены вам.")
                .formatted(Formatting.YELLOW), false);
        }
        
        // Завершаем заказ
        order.setCompleted();
        markDirty();
        
        // Принудительное сохранение
        try {
            player.getServerWorld().getPersistentStateManager().save();
            Origins.LOGGER.info("Принудительное сохранение после завершения заказа");
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принудительном сохранении: " + e.getMessage());
        }
        
        // Уведомляем заказчика
        notifyOrderOwner(order, "Ваш заказ выполнен курьером " + player.getName().getString() + "!");
        
        Origins.LOGGER.info("Заказ {} завершен игроком {}", orderId, player.getName().getString());
        return true;
    }
    
    /**
     * Завершает заказ (старый метод для совместимости)
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
        
        // Возвращаем предметы награды заказчику
        ServerPlayerEntity orderOwner = player.getServer().getPlayerManager().getPlayer(order.getOwnerUuid());
        if (orderOwner != null) {
            Origins.LOGGER.info("Возвращаем {} предметов награды игроку {}", order.getRewardItems().size(), orderOwner.getName().getString());
            for (ItemStack item : order.getRewardItems()) {
                Origins.LOGGER.info("Возвращаем предмет: {} x{}", item.getName().getString(), item.getCount());
            }
            CourierUtils.giveItemsToPlayer(orderOwner, order.getRewardItems());
            orderOwner.sendMessage(Text.literal("Ваш заказ отменен. Предметы награды возвращены в ваш инвентарь.")
                .formatted(Formatting.YELLOW), false);
        } else {
            // Если заказчик не онлайн, возвращаем предметы тому, кто отменил заказ (если это владелец)
            if (player.getUuid().equals(order.getOwnerUuid())) {
                Origins.LOGGER.info("Возвращаем {} предметов награды игроку {} (владелец отменил заказ)", order.getRewardItems().size(), player.getName().getString());
                for (ItemStack item : order.getRewardItems()) {
                    Origins.LOGGER.info("Возвращаем предмет: {} x{}", item.getName().getString(), item.getCount());
                }
                CourierUtils.giveItemsToPlayer(player, order.getRewardItems());
                player.sendMessage(Text.literal("Заказ отменен. Предметы награды возвращены в ваш инвентарь.")
                    .formatted(Formatting.YELLOW), false);
            }
        }
        
        order.setStatus(Order.Status.CANCELLED);
        
        markDirty();
        
        // Принудительное сохранение
        try {
            player.getServerWorld().getPersistentStateManager().save();
            Origins.LOGGER.info("Принудительное сохранение после отмены заказа");
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принудительном сохранении: " + e.getMessage());
        }
        
        Origins.LOGGER.info("Заказ {} отменен игроком {}", orderId, player.getName().getString());
        return true;
    }
    
    /**
     * Удаляет заказ полностью (может сделать только владелец или админ)
     */
    public boolean deleteOrder(UUID orderId, ServerPlayerEntity player) {
        Order order = orders.get(orderId);
        if (order == null) {
            return false;
        }
        
        // Проверяем права на удаление
        boolean canDelete = player.getUuid().equals(order.getOwnerUuid()) || 
                           CourierUtils.isAdmin(player) ||
                           // Разрешаем удалять завершенные заказы всем участникам
                           (order.getStatus() == Order.Status.COMPLETED || 
                            order.getStatus() == Order.Status.DECLINED || 
                            order.getStatus() == Order.Status.CANCELLED);
        
        if (!canDelete) {
            return false;
        }
        
        // Если заказ не завершен и удаляет владелец, возвращаем награду
        if (player.getUuid().equals(order.getOwnerUuid()) && 
            (order.getStatus() == Order.Status.OPEN || order.getStatus() == Order.Status.ACCEPTED || order.getStatus() == Order.Status.IN_PROGRESS)) {
            CourierUtils.giveItemsToPlayer(player, order.getRewardItems());
            player.sendMessage(Text.literal("Заказ удален. Предметы награды возвращены в ваш инвентарь.")
                .formatted(Formatting.YELLOW), false);
        }
        
        // Удаляем заказ
        orders.remove(orderId);
        
        // Удаляем из индекса игрока
        Set<UUID> playerOrderIds = playerOrders.get(order.getOwnerUuid());
        if (playerOrderIds != null) {
            playerOrderIds.remove(orderId);
            if (playerOrderIds.isEmpty()) {
                playerOrders.remove(order.getOwnerUuid());
            }
        }
        
        markDirty();
        
        // Принудительное сохранение
        try {
            player.getServerWorld().getPersistentStateManager().save();
            Origins.LOGGER.info("Принудительное сохранение после удаления заказа");
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при принудительном сохранении: " + e.getMessage());
        }
        
        Origins.LOGGER.info("Заказ {} удален игроком {}", orderId, player.getName().getString());
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
     * Уведомляет владельца заказа о изменениях
     */
    private void notifyOrderOwner(Order order, String message) {
        // Здесь можно добавить логику уведомления владельца заказа
        // Например, отправить сообщение в чат, если игрок онлайн
        Origins.LOGGER.info("Уведомление для {}: {}", order.getOwnerName(), message);
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