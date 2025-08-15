package io.github.apace100.origins.courier;

import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Упрощенная версия Order для клиентской стороны
 * Содержит только данные, необходимые для отображения в UI
 */
public class ClientOrder {
    public final UUID id;
    public final String ownerName;
    public final String description;
    public final List<ItemStack> requestItems;
    public final List<ItemStack> rewardItems;
    public final Order.Status status;
    public final String acceptedByName;
    public final long createdTime;

    public ClientOrder(UUID id, String ownerName, String description, 
                      List<ItemStack> requestItems, List<ItemStack> rewardItems, 
                      Order.Status status, String acceptedByName, long createdTime) {
        this.id = id;
        this.ownerName = ownerName;
        this.description = description;
        this.requestItems = new ArrayList<>(requestItems);
        this.rewardItems = new ArrayList<>(rewardItems);
        this.status = status;
        this.acceptedByName = acceptedByName;
        this.createdTime = createdTime;
    }

    /**
     * Создает ClientOrder из полного Order
     */
    public static ClientOrder fromOrder(Order order) {
        return new ClientOrder(
            order.getId(),
            order.getOwnerName(),
            order.getDescription(),
            order.getRequestItems(),
            order.getRewardItems(),
            order.getStatus(),
            order.getAcceptedByName(),
            order.getCreatedTime()
        );
    }

    /**
     * Получает краткое описание заказа для отображения в списке
     */
    public String getShortDescription() {
        if (description.length() <= 40) {
            return description;
        }
        return description.substring(0, 37) + "...";
    }

    /**
     * Получает первый предмет из запроса для отображения иконки
     */
    public ItemStack getFirstRequestItem() {
        return requestItems.isEmpty() ? ItemStack.EMPTY : requestItems.get(0);
    }

    /**
     * Получает количество запрашиваемых предметов
     */
    public int getRequestItemsCount() {
        return requestItems.size();
    }

    /**
     * Получает количество предметов награды
     */
    public int getRewardItemsCount() {
        return rewardItems.size();
    }

    /**
     * Проверяет, является ли заказ активным (доступным для принятия)
     */
    public boolean isActive() {
        return status == Order.Status.OPEN;
    }

    /**
     * Проверяет, принят ли заказ
     */
    public boolean isAccepted() {
        return status == Order.Status.ACCEPTED || status == Order.Status.IN_PROGRESS;
    }

    /**
     * Проверяет, завершен ли заказ
     */
    public boolean isCompleted() {
        return status == Order.Status.COMPLETED;
    }

    /**
     * Получает локализованное название статуса
     */
    public String getLocalizedStatus() {
        return switch (status) {
            case OPEN -> "Открыт";
            case ACCEPTED -> "Принят";
            case IN_PROGRESS -> "Выполняется";
            case COMPLETED -> "Завершен";
            case DECLINED -> "Отклонен";
            case CANCELLED -> "Отменен";
        };
    }

    /**
     * Получает цвет для отображения статуса
     */
    public int getStatusColor() {
        return switch (status) {
            case OPEN -> 0x7ED321;        // Зеленый
            case ACCEPTED -> 0x4A90E2;    // Синий
            case IN_PROGRESS -> 0xF5A623; // Оранжевый
            case COMPLETED -> 0x50E3C2;   // Бирюзовый
            case DECLINED -> 0xD0021B;    // Красный
            case CANCELLED -> 0x9B9B9B;   // Серый
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClientOrder that = (ClientOrder) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("ClientOrder{id=%s, owner=%s, status=%s}", 
                           id, ownerName, status);
    }
}