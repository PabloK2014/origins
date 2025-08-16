package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.apace100.origins.courier.CourierOrderManager;
import io.github.apace100.origins.courier.Order;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * Команда для тестирования создания заказов
 */
public class TestCreateOrderCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("test-create-order")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(TestCreateOrderCommand::createTestOrder)
        );
    }
    
    private static int createTestOrder(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = source.getWorld();
            CourierOrderManager manager = CourierOrderManager.get(world);
            
            // Создаем тестовый заказ
            Order order = new Order(UUID.randomUUID(), player.getName().getString(), player.getUuid());
            order.setDescription("Тестовый заказ для проверки сохранения");
            
            // Добавляем тестовые предметы
            order.addRequestItem(new ItemStack(Items.DIAMOND, 5));
            order.addRequestItem(new ItemStack(Items.GOLD_INGOT, 10));
            
            order.addRewardItem(new ItemStack(Items.EMERALD, 3));
            order.addRewardItem(new ItemStack(Items.EXPERIENCE_BOTTLE, 5));
            
            // Сохраняем заказ
            if (manager.createOrder(order)) {
                source.sendFeedback(() -> Text.literal("Тестовый заказ создан: " + order.getId())
                    .formatted(Formatting.GREEN), false);
                
                // Принудительно сохраняем
                manager.markDirty();
                world.getPersistentStateManager().save();
                
                source.sendFeedback(() -> Text.literal("Заказ сохранен на диск")
                    .formatted(Formatting.YELLOW), false);
                
                return 1;
            } else {
                source.sendFeedback(() -> Text.literal("Не удалось создать заказ")
                    .formatted(Formatting.RED), false);
                return 0;
            }
            
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("Ошибка: " + e.getMessage())
                .formatted(Formatting.RED), false);
            return 0;
        }
    }
}