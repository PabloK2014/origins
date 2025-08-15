package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Команды для открытия UI системы заказов курьера
 */
public class CourierUICommands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // Команда для открытия списка заказов (только для курьеров)
        dispatcher.register(CommandManager.literal("courier:open_orders_ui")
            .executes(CourierUICommands::openOrdersUI));
            
        // Команда для открытия создания заказа (для всех кроме курьеров)
        dispatcher.register(CommandManager.literal("courier:open_create_order_ui")
            .executes(CourierUICommands::openCreateOrderUI));
    }
    
    /**
     * Открывает UI списка заказов
     */
    private static int openOrdersUI(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            // Отправляем сообщение игроку с инструкцией
            player.sendMessage(Text.literal("Откройте чат и введите команду: ")
                .append(Text.literal("/courier:client_open_orders").formatted(Formatting.GREEN)), false);
            
            Origins.LOGGER.info("Игрок {} запросил открытие UI заказов", player.getName().getString());
            return 1;
        }
        
        return 0;
    }
    
    /**
     * Открывает UI создания заказа
     */
    private static int openCreateOrderUI(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            // Отправляем сообщение игроку с инструкцией
            player.sendMessage(Text.literal("Откройте чат и введите команду: ")
                .append(Text.literal("/courier:client_create_order").formatted(Formatting.GREEN)), false);
            
            Origins.LOGGER.info("Игрок {} запросил открытие UI создания заказа", player.getName().getString());
            return 1;
        }
        
        return 0;
    }
}