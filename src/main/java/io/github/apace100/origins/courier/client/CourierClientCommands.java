package io.github.apace100.origins.courier.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Клиентские команды для системы заказов курьера
 */
public class CourierClientCommands {
    
    /**
     * Регистрирует клиентские команды
     */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerClientCommands(dispatcher);
        });
    }
    
    private static void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // Команда для открытия списка заказов
        dispatcher.register(ClientCommandManager.literal("courier:client_open_orders")
            .executes(CourierClientCommands::openOrdersList));
            
        // Команда для открытия создания заказа
        dispatcher.register(ClientCommandManager.literal("courier:client_create_order")
            .executes(CourierClientCommands::openCreateOrder));
    }
    
    /**
     * Открывает экран списка заказов
     */
    private static int openOrdersList(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player != null) {
            // Открываем экран списка заказов
            client.execute(() -> {
                ClientOrderManager.getInstance().openOrdersScreen();
            });
            
            client.player.sendMessage(Text.literal("Открываю список заказов..."), false);
            return 1;
        }
        
        return 0;
    }
    
    /**
     * Открывает экран создания заказа
     */
    private static int openCreateOrder(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player != null) {
            // Открываем экран создания заказа
            client.execute(() -> {
                client.setScreen(new CreateOrderScreen());
            });
            
            client.player.sendMessage(Text.literal("Открываю создание заказа..."), false);
            return 1;
        }
        
        return 0;
    }
}