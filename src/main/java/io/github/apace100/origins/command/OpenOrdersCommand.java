package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.courier.client.ClientOrderManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Команда для открытия экрана заказов курьера
 */
public class OpenOrdersCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("orders")
            .executes(OpenOrdersCommand::openOrders));
    }
    
    private static int openOrders(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() != null) {
            // Отправляем сообщение игроку с инструкцией
            source.sendFeedback(() -> Text.literal("Откройте экран заказов через интерфейс игры"), false);
        }
        
        return 1;
    }
}