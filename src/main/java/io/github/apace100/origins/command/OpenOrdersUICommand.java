package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.courier.CourierNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Команда для открытия UI заказов через сетевой пакет
 */
public class OpenOrdersUICommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("open-orders")
            .requires(source -> source.hasPermissionLevel(0)) // Доступно всем
            .executes(OpenOrdersUICommand::openOrdersUI)
        );
    }
    
    private static int openOrdersUI(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            
            // Отправляем пакет на клиент для открытия UI
            PacketByteBuf buf = PacketByteBufs.create();
            ServerPlayNetworking.send(player, CourierNetworking.OPEN_ORDERS_UI, buf);
            
            player.sendMessage(Text.literal("Открываем интерфейс заказов...").formatted(Formatting.GREEN), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Ошибка: " + e.getMessage()).formatted(Formatting.RED), false);
            return 0;
        }
    }
}