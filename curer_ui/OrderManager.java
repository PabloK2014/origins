package com.example.courier;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;

public class OrderManager extends PersistentState {
    public static final String KEY = "courier:orders";
    private final Map<UUID, Order> orders = new LinkedHashMap<>();

    public OrderManager() {}
    public static OrderManager get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(OrderManager::fromNbt, OrderManager::new, KEY);
    }

    public static OrderManager fromNbt(NbtCompound n) {
        OrderManager m = new OrderManager();
        NbtList list = n.getList("orders", NbtElement.COMPOUND_TYPE);
        for (int i=0;i<list.size();i++) {
            m.orders.put(Order.fromNbt(list.getCompound(i)).id, Order.fromNbt(list.getCompound(i)));
        }
        return m;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound n) {
        NbtList list = new NbtList();
        for (Order o : orders.values()) list.add(o.toNbt());
        n.put("orders", list);
        return n;
    }

    public Collection<Order> getAll() { return orders.values(); }
    public void add(Order o) { orders.put(o.id, o); markDirty(); }
    public void remove(UUID id) { orders.remove(id); markDirty(); }
    public Order get(UUID id) { return orders.get(id); }

    // Notify all players on server/world (simple broadcast). В реале можно цельно шлёшь только курьерам
    public void broadcastOrders(ServerWorld world) {
        orders.values().forEach(o -> {}); // placeholder
        // формируем пакет для всех игроков:
        // (см. ModInitializer — как отправлять SYNC_ORDERS)
    }
}
