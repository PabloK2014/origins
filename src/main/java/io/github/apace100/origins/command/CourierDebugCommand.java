package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.courier.CourierOrderManager;
import io.github.apace100.origins.courier.Order;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Команда для отладки системы курьерских заказов
 */
public class CourierDebugCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("courier-debug")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("list")
                .executes(CourierDebugCommand::listOrders))
            .then(CommandManager.literal("count")
                .executes(CourierDebugCommand::countOrders))
            .then(CommandManager.literal("save")
                .executes(CourierDebugCommand::forceSave))
        );
    }
    
    private static int listOrders(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        CourierOrderManager manager = CourierOrderManager.get(world);
        
        List<Order> orders = manager.getAllOrders();
        
        source.sendFeedback(() -> Text.literal("=== Список заказов ===").formatted(Formatting.YELLOW), false);
        
        if (orders.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Заказов нет").formatted(Formatting.GRAY), false);
        } else {
            for (Order order : orders) {
                String orderInfo = String.format("ID: %s | Владелец: %s | Статус: %s | Описание: %s", 
                    order.getId().toString().substring(0, 8), 
                    order.getOwnerName(), 
                    order.getStatus(), 
                    order.getShortDescription());
                source.sendFeedback(() -> Text.literal(orderInfo).formatted(Formatting.WHITE), false);
            }
        }
        
        return orders.size();
    }
    
    private static int countOrders(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        CourierOrderManager manager = CourierOrderManager.get(world);
        
        int count = manager.getAllOrders().size();
        source.sendFeedback(() -> Text.literal("Всего заказов: " + count).formatted(Formatting.GREEN), false);
        
        return count;
    }
    
    private static int forceSave(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        CourierOrderManager manager = CourierOrderManager.get(world);
        
        manager.markDirty();
        world.getPersistentStateManager().save();
        
        source.sendFeedback(() -> Text.literal("Принудительное сохранение выполнено").formatted(Formatting.GREEN), false);
        
        return 1;
    }
}