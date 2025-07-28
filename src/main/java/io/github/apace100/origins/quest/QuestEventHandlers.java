package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Обработчики событий для отслеживания прогресса квестов
 */
public class QuestEventHandlers {
    
    /**
     * Инициализирует все обработчики событий
     */
    public static void initialize() {
        // Регистрируем обработчик разрушения блоков
        PlayerBlockBreakEvents.AFTER.register(QuestEventHandlers::onBlockBreak);
        
        Origins.LOGGER.info("Обработчики событий квестов инициализированы");
    }
    
    /**
     * Обработчик разрушения блоков (для майнинга)
     */
    private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClient || !(player instanceof ServerPlayerEntity)) {
            return;
        }
        
        try {
            String blockId = state.getBlock().toString();
            
            // Отслеживаем действие майнинга
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            tracker.trackPlayerAction(player, "mine", blockId, 1);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке разрушения блока: {}", e.getMessage());
        }
    }
    
    /**
     * Обработчик убийства сущностей
     */
    public static void onEntityKill(PlayerEntity player, LivingEntity killedEntity) {
        if (player == null || killedEntity == null || player.getWorld().isClient) {
            return;
        }
        
        try {
            String entityType = killedEntity.getType().toString();
            
            // Отслеживаем действие убийства
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            tracker.trackPlayerAction(player, "kill", entityType, 1);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке убийства сущности: {}", e.getMessage());
        }
    }
    
    /**
     * Обработчик крафта предметов
     */
    public static void onItemCraft(PlayerEntity player, ItemStack craftedItem) {
        if (player == null || craftedItem.isEmpty() || player.getWorld().isClient) {
            return;
        }
        
        try {
            String itemId = craftedItem.getItem().toString();
            int amount = craftedItem.getCount();
            
            // Отслеживаем действие крафта
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            tracker.trackPlayerAction(player, "craft", itemId, amount);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке крафта предмета: {}", e.getMessage());
        }
    }
    
    /**
     * Обработчик подбора предметов
     */
    public static void onItemPickup(PlayerEntity player, ItemStack pickedUpItem) {
        if (player == null || pickedUpItem.isEmpty() || player.getWorld().isClient) {
            return;
        }
        
        try {
            String itemId = pickedUpItem.getItem().toString();
            int amount = pickedUpItem.getCount();
            
            // Отслеживаем действие сбора
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            tracker.trackPlayerAction(player, "collect", itemId, amount);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке подбора предмета: {}", e.getMessage());
        }
    }
    
    /**
     * Обработчик приготовления еды
     */
    public static void onItemSmelt(PlayerEntity player, ItemStack smeltedItem) {
        if (player == null || smeltedItem.isEmpty() || player.getWorld().isClient) {
            return;
        }
        
        try {
            String itemId = smeltedItem.getItem().toString();
            int amount = smeltedItem.getCount();
            
            // Отслеживаем действие приготовления
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            tracker.trackPlayerAction(player, "cook", itemId, amount);
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке приготовления предмета: {}", e.getMessage());
        }
    }
    
    /**
     * Периодическое обновление прогресса квестов
     * Вызывается каждую секунду для проверки целей типа "collect"
     */
    public static void onPlayerTick(PlayerEntity player) {
        if (player == null || player.getWorld().isClient || !(player instanceof ServerPlayerEntity)) {
            return;
        }
        
        // Обновляем только каждые 20 тиков (1 секунда)
        if (player.getWorld().getTime() % 20 != 0) {
            return;
        }
        
        try {
            // Получаем все билеты квестов и обновляем их прогресс
            QuestInventoryManager inventoryManager = QuestInventoryManager.getInstance();
            QuestProgressTracker tracker = QuestProgressTracker.getInstance();
            
            for (ItemStack ticket : inventoryManager.findQuestTickets(player)) {
                tracker.updateTicketProgress(player, ticket);
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при периодическом обновлении прогресса квестов: {}", e.getMessage());
        }
    }
}