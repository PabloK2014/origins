package io.github.apace100.origins.courier;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Identifier;

/**
 * Утилиты для системы курьерских заказов
 */
public class CourierUtils {
    
    /**
     * Получает класс игрока через систему профессий Origins
     */
    public static String getPlayerClass(ServerPlayerEntity player) {
        try {
            ProfessionComponent professionComponent = ProfessionComponent.KEY.get(player);
            if (professionComponent == null) {
                return "unknown";
            }
            
            Identifier currentProfessionId = professionComponent.getCurrentProfessionId();
            if (currentProfessionId == null) {
                return "unknown";
            }
            
            // Возвращаем путь идентификатора как класс (например, "courier", "blacksmith", etc.)
            return currentProfessionId.getPath();
            
        } catch (Exception e) {
            // Если произошла ошибка, возвращаем unknown
            return "unknown";
        }
    }
    
    /**
     * Проверяет, является ли игрок курьером
     */
    public static boolean isCourier(ServerPlayerEntity player) {
        return "courier".equals(getPlayerClass(player));
    }
    
    /**
     * Проверяет, является ли игрок администратором
     */
    public static boolean isAdmin(ServerPlayerEntity player) {
        return player.hasPermissionLevel(2); // Уровень OP 2 и выше
    }
    
    /**
     * Проверяет, есть ли у игрока необходимые предметы в инвентаре
     */
    public static boolean hasRequiredItems(ServerPlayerEntity player, java.util.List<ItemStack> requiredItems) {
        Inventory inventory = player.getInventory();
        
        for (ItemStack requiredItem : requiredItems) {
            if (requiredItem.isEmpty()) continue;
            
            int requiredCount = requiredItem.getCount();
            int foundCount = 0;
            
            // Проверяем все слоты инвентаря
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack slotItem = inventory.getStack(i);
                if (ItemStack.canCombine(requiredItem, slotItem)) {
                    foundCount += slotItem.getCount();
                    if (foundCount >= requiredCount) {
                        break;
                    }
                }
            }
            
            if (foundCount < requiredCount) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Забирает предметы из инвентаря игрока
     */
    public static boolean takeItemsFromPlayer(ServerPlayerEntity player, java.util.List<ItemStack> itemsToTake) {
        // Сначала проверяем, есть ли все необходимые предметы
        if (!hasRequiredItems(player, itemsToTake)) {
            return false;
        }
        
        Inventory inventory = player.getInventory();
        
        // Забираем предметы
        for (ItemStack requiredItem : itemsToTake) {
            if (requiredItem.isEmpty()) continue;
            
            int remainingToTake = requiredItem.getCount();
            
            for (int i = 0; i < inventory.size() && remainingToTake > 0; i++) {
                ItemStack slotItem = inventory.getStack(i);
                if (ItemStack.canCombine(requiredItem, slotItem)) {
                    int toTakeFromSlot = Math.min(remainingToTake, slotItem.getCount());
                    slotItem.decrement(toTakeFromSlot);
                    remainingToTake -= toTakeFromSlot;
                    
                    if (slotItem.isEmpty()) {
                        inventory.setStack(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Дает предметы игроку
     */
    public static void giveItemsToPlayer(ServerPlayerEntity player, java.util.List<ItemStack> itemsToGive) {
                for (ItemStack item : itemsToGive) {
            if (!item.isEmpty()) {
                                // Пытаемся добавить в инвентарь, если не помещается - дропаем
                if (!player.getInventory().insertStack(item.copy())) {
                                        player.dropItem(item.copy(), false);
                } else {
                                    }
            }
        }
            }
}