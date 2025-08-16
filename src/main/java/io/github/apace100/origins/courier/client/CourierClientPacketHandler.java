package io.github.apace100.origins.courier.client;

import io.github.apace100.origins.courier.CourierNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

/**
 * Обработчик клиентских пакетов для системы курьерских заказов
 */
public class CourierClientPacketHandler {
    
    public static void registerClientHandlers() {
        // Обработчик открытия UI заказов
        ClientPlayNetworking.registerGlobalReceiver(CourierNetworking.OPEN_ORDERS_UI, 
            (client, handler, buf, responseSender) -> {
                client.execute(() -> {
                    System.out.println("DEBUG: Received OPEN_ORDERS_UI packet, opening screen");
                    client.setScreen(new OrdersListScreen());
                });
            });
    }
}