package io.github.apace100.origins.courier;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Модель данных заказа для системы курьера
 */
public class Order {
    
    public enum Status { 
        OPEN,           // Заказ создан и доступен
        ACCEPTED,       // Заказ принят курьером
        IN_PROGRESS,    // Заказ выполняется
        COMPLETED,      // Заказ выполнен
        DECLINED,       // Заказ отклонен
        CANCELLED       // Заказ отменен
    }

    private final UUID id;
    private final String ownerName;
    private final UUID ownerUuid;
    private String description;
    private final List<ItemStack> requestItems;
    private final List<ItemStack> rewardItems;
    private Status status;
    private String acceptedByName;
    private UUID acceptedByUuid;
    private final long createdTime;
    private long acceptedTime;
    private long completedTime;
    private int experienceReward; // Опыт для курьера

    public Order(UUID id, String ownerName, UUID ownerUuid) {
        this.id = id;
        this.ownerName = ownerName;
        this.ownerUuid = ownerUuid;
        this.description = "";
        this.requestItems = new ArrayList<>();
        this.rewardItems = new ArrayList<>();
        this.status = Status.OPEN;
        this.createdTime = System.currentTimeMillis();
        this.acceptedTime = 0;
        this.completedTime = 0;
        this.experienceReward = 0;
    }

    // Геттеры
    public UUID getId() { return id; }
    public String getOwnerName() { return ownerName; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getDescription() { return description; }
    public List<ItemStack> getRequestItems() { return new ArrayList<>(requestItems); }
    public List<ItemStack> getRewardItems() { return new ArrayList<>(rewardItems); }
    public Status getStatus() { return status; }
    public String getAcceptedByName() { return acceptedByName; }
    public UUID getAcceptedByUuid() { return acceptedByUuid; }
    public long getCreatedTime() { return createdTime; }
    public long getAcceptedTime() { return acceptedTime; }
    public long getCompletedTime() { return completedTime; }
    public int getExperienceReward() { return experienceReward; }

    // Сеттеры
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setAcceptedBy(String name, UUID uuid) {
        this.acceptedByName = name;
        this.acceptedByUuid = uuid;
        this.acceptedTime = System.currentTimeMillis();
    }

    public void setCompleted() {
        this.status = Status.COMPLETED;
        this.completedTime = System.currentTimeMillis();
    }
    
    public void setExperienceReward(int experience) {
        this.experienceReward = Math.max(0, experience);
    }

    /**
     * Получает краткое описание заказа для уведомлений
     */
    public String getShortDescription() {
        if (description.length() <= 40) {
            return description;
        }
        return description.substring(0, 37) + "...";
    }

    // Методы для работы с предметами
    public void addRequestItem(ItemStack item) {
        if (item != null && !item.isEmpty() && requestItems.size() < 10) {
            requestItems.add(item.copy());
        }
    }

    public void addRewardItem(ItemStack item) {
        if (item != null && !item.isEmpty() && rewardItems.size() < 10) {
            rewardItems.add(item.copy());
        }
    }

    public void clearRequestItems() {
        requestItems.clear();
    }

    public void clearRewardItems() {
        rewardItems.clear();
    }

    // Валидация заказа
    public boolean isValid() {
        return !requestItems.isEmpty() && !rewardItems.isEmpty() && 
               description != null && !description.trim().isEmpty();
    }

    public boolean canBeAcceptedBy(ServerPlayerEntity player) {
        if (status != Status.OPEN) return false;
        
        // Проверяем, что игрок не является владельцем заказа
        if (player.getUuid().equals(ownerUuid)) {
            return false;
        }
        
        // Проверяем, что игрок - курьер
        return CourierUtils.isCourier(player);
    }

    // Сериализация в NBT
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        
        nbt.putUuid("id", id);
        nbt.putString("ownerName", ownerName);
        nbt.putUuid("ownerUuid", ownerUuid);
        nbt.putString("description", description);
        nbt.putString("status", status.name());
        nbt.putLong("createdTime", createdTime);
        nbt.putLong("acceptedTime", acceptedTime);
        nbt.putLong("completedTime", completedTime);
        nbt.putInt("experienceReward", experienceReward);
        
        if (acceptedByName != null) {
            nbt.putString("acceptedByName", acceptedByName);
        }
        if (acceptedByUuid != null) {
            nbt.putUuid("acceptedByUuid", acceptedByUuid);
        }

        // Сериализация предметов запроса
        NbtList requestList = new NbtList();
        for (ItemStack item : requestItems) {
            if (!item.isEmpty()) {
                requestList.add(item.writeNbt(new NbtCompound()));
            }
        }
        nbt.put("requestItems", requestList);

        // Сериализация предметов награды
        NbtList rewardList = new NbtList();
        for (ItemStack item : rewardItems) {
            if (!item.isEmpty()) {
                rewardList.add(item.writeNbt(new NbtCompound()));
            }
        }
        nbt.put("rewardItems", rewardList);

        return nbt;
    }

    // Десериализация из NBT
    public static Order fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("id");
        String ownerName = nbt.getString("ownerName");
        UUID ownerUuid = nbt.getUuid("ownerUuid");
        
        Order order = new Order(id, ownerName, ownerUuid);
        order.setDescription(nbt.getString("description"));
        order.setStatus(Status.valueOf(nbt.getString("status")));
        
        if (nbt.contains("acceptedByName")) {
            order.acceptedByName = nbt.getString("acceptedByName");
        }
        if (nbt.contains("acceptedByUuid")) {
            order.acceptedByUuid = nbt.getUuid("acceptedByUuid");
        }
        
        // Восстановление временных меток (если они есть в старых версиях)
        if (nbt.contains("acceptedTime")) {
            order.acceptedTime = nbt.getLong("acceptedTime");
        }
        if (nbt.contains("completedTime")) {
            order.completedTime = nbt.getLong("completedTime");
        }

        // Десериализация предметов запроса
        NbtList requestList = nbt.getList("requestItems", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < requestList.size(); i++) {
            ItemStack item = ItemStack.fromNbt(requestList.getCompound(i));
            if (!item.isEmpty()) {
                order.requestItems.add(item);
            }
        }

        // Десериализация предметов награды
        NbtList rewardList = nbt.getList("rewardItems", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < rewardList.size(); i++) {
            ItemStack item = ItemStack.fromNbt(rewardList.getCompound(i));
            if (!item.isEmpty()) {
                order.rewardItems.add(item);
            }
        }

        return order;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Order order = (Order) obj;
        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Order{id=%s, owner=%s, status=%s, requestItems=%d, rewardItems=%d}", 
                           id, ownerName, status, requestItems.size(), rewardItems.size());
    }
}