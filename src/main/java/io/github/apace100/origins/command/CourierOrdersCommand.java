package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.courier.CourierUtils;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Команда для работы с заказами курьера
 */
public class CourierOrdersCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("orders")
            .executes(CourierOrdersCommand::openOrdersInterface));
            
        dispatcher.register(CommandManager.literal("createorder")
            .executes(CourierOrdersCommand::openCreateOrderInterface));
    }
    
    /**
     * Открывает интерфейс заказов
     */
    private static int openOrdersInterface(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            // Отправляем сообщение с инструкцией для открытия интерфейса
            Text message = Text.literal("📦 Система заказов курьера").formatted(Formatting.GOLD)
                .append(Text.literal("\n\n"))
                .append(Text.literal("Для курьеров: ").formatted(Formatting.YELLOW))
                .append(Text.literal("Нажмите ").formatted(Formatting.WHITE))
                .append(Text.literal("[ОТКРЫТЬ ЗАКАЗЫ]").formatted(Formatting.GREEN)
                    .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/courier:client_open_orders"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Text.literal("Нажмите, чтобы открыть список заказов")))))
                .append(Text.literal("\n\n"))
                .append(Text.literal("Для заказчиков: ").formatted(Formatting.YELLOW))
                .append(Text.literal("Нажмите ").formatted(Formatting.WHITE))
                .append(Text.literal("[СОЗДАТЬ ЗАКАЗ]").formatted(Formatting.BLUE)
                    .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/courier:client_create_order"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Text.literal("Нажмите, чтобы создать новый заказ")))));
            
            player.sendMessage(message, false);
            
                        return 1;
        }
        
        return 0;
    }
    
    /**
     * Открывает интерфейс создания заказа
     */
    private static int openCreateOrderInterface(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            if (CourierUtils.isCourier(player)) {
                player.sendMessage(Text.literal("Курьеры не могут создавать заказы!")
                    .formatted(Formatting.RED), false);
                return 0;
            }
            
            // Отправляем команду для открытия интерфейса создания заказа
            Text message = Text.literal("Нажмите для создания заказа: ")
                .append(Text.literal("[СОЗДАТЬ ЗАКАЗ]").formatted(Formatting.GREEN)
                    .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/courier:client_create_order"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Text.literal("Открыть интерфейс создания заказа")))));
            
            player.sendMessage(message, false);
            return 1;
        }
        
        return 0;
    }
}