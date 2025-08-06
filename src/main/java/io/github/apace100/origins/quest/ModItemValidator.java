package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Утилитный класс для валидации и обработки модовых предметов в квестах
 */
public class ModItemValidator {
    
    // Кэш для проверенных предметов
    private static final Map<String, Item> itemCache = new HashMap<>();
    
    // Fallback предметы для разных категорий
    private static final Map<String, String> FALLBACK_ITEMS = new HashMap<>();
    
    static {
        // Инициализируем fallback предметы
        FALLBACK_ITEMS.put("food", "minecraft:bread");
        FALLBACK_ITEMS.put("drink", "minecraft:milk_bucket");
        FALLBACK_ITEMS.put("tool", "minecraft:iron_pickaxe");
        FALLBACK_ITEMS.put("weapon", "minecraft:iron_sword");
        FALLBACK_ITEMS.put("armor", "minecraft:iron_chestplate");
        FALLBACK_ITEMS.put("material", "minecraft:iron_ingot");
        FALLBACK_ITEMS.put("block", "minecraft:stone");
        FALLBACK_ITEMS.put("default", "minecraft:stick");
    }
    
    /**
     * Проверяет существует ли предмет с данным ID
     */
    public static boolean isValidItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        
        // Проверяем кэш
        if (itemCache.containsKey(itemId)) {
            return itemCache.get(itemId) != Items.AIR;
        }
        
        try {
            Identifier identifier = new Identifier(itemId);
            Item item = Registries.ITEM.get(identifier);
            
            // Кэшируем результат
            itemCache.put(itemId, item);
            
            boolean isValid = item != Items.AIR;
            if (!isValid) {
                Origins.LOGGER.warn("🔍 [ModItemValidator] Предмет не найден: " + itemId);
            }
            
            return isValid;
        } catch (Exception e) {
            Origins.LOGGER.error("🔥 [ModItemValidator] Ошибка при проверке предмета " + itemId + ": " + e.getMessage());
            itemCache.put(itemId, Items.AIR);
            return false;
        }
    }
    
    /**
     * Получает предмет по ID или возвращает fallback
     */
    public static Item getItemOrFallback(String itemId) {
        if (isValidItem(itemId)) {
            return itemCache.get(itemId);
        }
        
        // Возвращаем fallback предмет
        String fallbackId = getFallbackItemId(itemId);
        Origins.LOGGER.info("🔄 [ModItemValidator] Используем fallback для " + itemId + " -> " + fallbackId);
        
        try {
            return Registries.ITEM.get(new Identifier(fallbackId));
        } catch (Exception e) {
            Origins.LOGGER.error("🔥 [ModItemValidator] Ошибка при получении fallback предмета: " + e.getMessage());
            return Items.STICK; // Последний fallback
        }
    }
    
    /**
     * Создает ItemStack с валидацией модовых предметов
     */
    public static ItemStack createValidItemStack(String itemId, int count) {
        Item item = getItemOrFallback(itemId);
        return new ItemStack(item, count);
    }
    
    /**
     * Получает fallback ID для предмета на основе его названия/категории
     */
    private static String getFallbackItemId(String itemId) {
        if (itemId == null) return FALLBACK_ITEMS.get("default");
        
        String lowerItemId = itemId.toLowerCase();
        
        // Определяем категорию по названию предмета
        if (lowerItemId.contains("food") || lowerItemId.contains("bread") || lowerItemId.contains("cake") || 
            lowerItemId.contains("soup") || lowerItemId.contains("stew") || lowerItemId.contains("pie")) {
            return FALLBACK_ITEMS.get("food");
        }
        
        if (lowerItemId.contains("drink") || lowerItemId.contains("tea") || lowerItemId.contains("coffee") || 
            lowerItemId.contains("wine") || lowerItemId.contains("beer") || lowerItemId.contains("milk")) {
            return FALLBACK_ITEMS.get("drink");
        }
        
        if (lowerItemId.contains("sword") || lowerItemId.contains("bow") || lowerItemId.contains("crossbow")) {
            return FALLBACK_ITEMS.get("weapon");
        }
        
        if (lowerItemId.contains("pickaxe") || lowerItemId.contains("axe") || lowerItemId.contains("shovel") || 
            lowerItemId.contains("hoe") || lowerItemId.contains("tool")) {
            return FALLBACK_ITEMS.get("tool");
        }
        
        if (lowerItemId.contains("helmet") || lowerItemId.contains("chestplate") || lowerItemId.contains("leggings") || 
            lowerItemId.contains("boots") || lowerItemId.contains("armor")) {
            return FALLBACK_ITEMS.get("armor");
        }
        
        if (lowerItemId.contains("ingot") || lowerItemId.contains("ore") || lowerItemId.contains("gem") || 
            lowerItemId.contains("dust") || lowerItemId.contains("nugget")) {
            return FALLBACK_ITEMS.get("material");
        }
        
        if (lowerItemId.contains("block") || lowerItemId.contains("stone") || lowerItemId.contains("wood")) {
            return FALLBACK_ITEMS.get("block");
        }
        
        return FALLBACK_ITEMS.get("default");
    }
    
    /**
     * Получает читаемое название предмета с fallback
     */
    public static String getItemDisplayName(String itemId) {
        if (isValidItem(itemId)) {
            try {
                Item item = itemCache.get(itemId);
                return item.getName().getString();
            } catch (Exception e) {
                Origins.LOGGER.warn("🔍 [ModItemValidator] Не удалось получить название для " + itemId);
            }
        }
        
        // Возвращаем обработанное название на основе ID
        return formatItemIdAsName(itemId);
    }
    
    /**
     * Форматирует ID предмета как читаемое название
     */
    private static String formatItemIdAsName(String itemId) {
        if (itemId == null) return "Неизвестный предмет";
        
        // Убираем namespace (minecraft:, create:, etc.)
        String name = itemId.contains(":") ? itemId.split(":", 2)[1] : itemId;
        
        // Заменяем подчеркивания на пробелы и делаем первую букву заглавной
        String[] parts = name.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (result.length() > 0) result.append(" ");
            if (!part.isEmpty()) {
                result.append(part.substring(0, 1).toUpperCase())
                      .append(part.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
    
    /**
     * Очищает кэш предметов (для перезагрузки)
     */
    public static void clearCache() {
        itemCache.clear();
        Origins.LOGGER.info("🗑️ [ModItemValidator] Кэш предметов очищен");
    }
    
    /**
     * Получает статистику кэша
     */
    public static String getCacheStats() {
        int validItems = 0;
        int invalidItems = 0;
        
        for (Item item : itemCache.values()) {
            if (item == Items.AIR) {
                invalidItems++;
            } else {
                validItems++;
            }
        }
        
        return String.format("Кэш предметов: %d валидных, %d невалидных, всего %d", 
                           validItems, invalidItems, itemCache.size());
    }
}