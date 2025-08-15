package com.example.courier;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CourierNetworking {
    public static final Identifier CREATE_ORDER = new Identifier("courier", "create_order");
    public static final Identifier ACCEPT_ORDER = new Identifier("courier", "accept_order");
    public static final Identifier DECLINE_ORDER = new Identifier("courier", "decline_order");
    public static final Identifier SYNC_ORDERS = new Identifier("courier", "sync_orders");
    public static final Identifier NEW_ORDER_NOTIFY = new Identifier("courier", "new_order_notify");

    // Сервер отправляет список заказов клиенту
    public static void sendOrdersToPlayer(ServerPlayerEntity player, PacketByteBuf buf) {
        ServerPlayNetworking.send(player, SYNC_ORDERS, buf);
    }

    // Короткие методы для отправки клиентом пакетов создания/действия
    public static void sendCreateOrder(PacketByteBuf buf) {
        ClientPlayNetworking.send(CREATE_ORDER, buf);
    }
    public static void sendAcceptOrder(PacketByteBuf buf) {
        ClientPlayNetworking.send(ACCEPT_ORDER, buf);
    }
    public static void sendDeclineOrder(PacketByteBuf buf) {
        ClientPlayNetworking.send(DECLINE_ORDER, buf);
    }
}
