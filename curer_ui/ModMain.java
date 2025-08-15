package com.example.courier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;

import java.util.UUID;

public class ModMain implements ModInitializer {
    @Override
    public void onInitialize() {
        // Обработчик создания заказа (от клиента на сервер)
        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.CREATE_ORDER, (server, player, handler, buf, responder) -> {
            PacketByteBuf copy = new PacketByteBuf(buf.copy());
            server.execute(() -> {
                try {
                    // парсим пакет: description + requestItemsList + rewardItemsList
                    // Для простоты предполагаем: сначала description (string), затем int reqCount, затем N itemstacks, затем int rewCount, затем itemstacks.
                    String description = copy.readString(32767);
                    int reqCount = copy.readInt();
                    Order o = new Order(UUID.randomUUID(), player.getEntityName(), player.getUuid());
                    for (int i=0;i<reqCount;i++) {
                        o.requestItems.add(copy.readItemStack());
                    }
                    int rewCount = copy.readInt();
                    for (int i=0;i<rewCount;i++) {
                        o.rewardItems.add(copy.readItemStack());
                    }
                    o.description = description;

                    ServerWorld world = player.getWorld().getServer().getOverworld(); // or player.world
                    OrderManager manager = OrderManager.get(world);
                    manager.add(o);

                    // Уведомляем всех игроков — отправляем "новый заказ"
                    server.getPlayerManager().getPlayerList().forEach(p -> {
                        PacketByteBuf out = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
                        out.writeUuid(o.id);
                        out.writeString(o.ownerName);
                        ServerPlayNetworking.send((ServerPlayerEntity)p, CourierNetworking.NEW_ORDER_NOTIFY, out);
                    });

                    // также можно отправить SYNC_ORDERS
                } catch (Exception e) {
                    player.sendMessage(Text.of("Ошибка создания заказа: " + e.getMessage()), false);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.ACCEPT_ORDER, (server, player, handler, buf, responder) -> {
            PacketByteBuf copy = new PacketByteBuf(buf.copy());
            server.execute(() -> {
                UUID id = copy.readUuid();
                ServerWorld world = player.getWorld().getServer().getOverworld();
                OrderManager manager = OrderManager.get(world);
                Order o = manager.get(id);
                if (o != null && o.status == Order.Status.OPEN) {
                    o.status = Order.Status.ACCEPTED;
                    o.acceptedByName = player.getEntityName();
                    o.acceptedByUuid = player.getUuid();
                    manager.markDirty();
                    player.sendMessage(Text.of("Вы приняли заказ " + id), false);
                    // уведомить хозяина:
                    server.getPlayerManager().getPlayerList().stream().filter(p -> p.getUuid().equals(o.ownerUuid)).findFirst().ifPresent(owner -> {
                        owner.sendMessage(Text.of("Ваш заказ был принят игроком " + player.getEntityName()), false);
                    });
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CourierNetworking.DECLINE_ORDER, (server, player, handler, buf, responder) -> {
            PacketByteBuf copy = new PacketByteBuf(buf.copy());
            server.execute(() -> {
                UUID id = copy.readUuid();
                ServerWorld world = player.getWorld().getServer().getOverworld();
                OrderManager manager = OrderManager.get(world);
                Order o = manager.get(id);
                if (o != null && o.status == Order.Status.OPEN) {
                    o.status = Order.Status.DECLINED;
                    manager.markDirty();
                    player.sendMessage(Text.of("Вы отклонили заказ " + id), false);
                }
            });
        });

        // При подключении игрока — можно слать SYNC_ORDERS (реализация пакета SYNC_ORDERS тут опущена, но идея — сериализовать список заказов).
    }
}
