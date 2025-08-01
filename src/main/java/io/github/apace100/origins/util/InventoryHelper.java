package io.github.apace100.origins.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Утилиты для работы с инвентарем
 */
public class InventoryHelper {
    
    /**
     * Проверяет, находится ли ItemStack в инвентаре игрока
     */
    public static boolean isStackInPlayerInventory(ItemStack stack, PlayerEntity player) {
        if (stack == null || stack.isEmpty() || player == null) {
            return false;
        }
        
        // Проверяем основной инвентарь
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack inventoryStack = player.getInventory().getStack(i);
            if (inventoryStack == stack) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, находится ли ItemStack в инвентаре текущего клиентского игрока
     */
    public static boolean isStackInClientPlayerInventory(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        
        return isStackInPlayerInventory(stack, client.player);
    }
    
    /**
     * Проверяет, отображается ли тултип в контексте инвентаря игрока
     * (а не в GUI доски объявлений или другого интерфейса)
     */
    public static boolean isTooltipInPlayerInventoryContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            return true; // Если экран не открыт, значит тултип в HUD
        }
        
        // Проверяем, что это НЕ экран доски объявлений
        String screenClass = client.currentScreen.getClass().getSimpleName();
        if (screenClass.equals("BountyBoardScreen")) {
            return false; // Это доска объявлений, не инициализируем время
        }
        
        // Проверяем, что это экран инвентаря игрока или другой допустимый экран
        return screenClass.equals("InventoryScreen") || 
               screenClass.equals("CreativeInventoryScreen") ||
               screenClass.equals("SurvivalInventoryScreen") ||
               screenClass.contains("Inventory"); // Общая проверка для других типов инвентаря
    }
}