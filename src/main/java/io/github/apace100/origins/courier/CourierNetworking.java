package io.github.apace100.origins.courier;

import net.minecraft.util.Identifier;
import io.github.apace100.origins.Origins;

/**
 * Константы и утилиты для сетевого взаимодействия системы заказов курьера
 */
public class CourierNetworking {
    
    // Идентификаторы пакетов
    public static final Identifier CREATE_ORDER = Origins.identifier("courier_create_order");
    public static final Identifier ACCEPT_ORDER = Origins.identifier("courier_accept_order");
    public static final Identifier DECLINE_ORDER = Origins.identifier("courier_decline_order");
    public static final Identifier COMPLETE_ORDER = Origins.identifier("courier_complete_order");
    public static final Identifier CANCEL_ORDER = Origins.identifier("courier_cancel_order");
    public static final Identifier DELETE_ORDER = Origins.identifier("courier_delete_order");
    public static final Identifier OPEN_ORDERS_UI = Origins.identifier("courier_open_orders_ui");
    public static final Identifier SYNC_ORDERS = Origins.identifier("courier_sync_orders");
    public static final Identifier NEW_ORDER_NOTIFY = Origins.identifier("courier_new_order_notify");
    public static final Identifier ORDER_STATUS_UPDATE = Origins.identifier("courier_order_status_update");
    public static final Identifier REQUEST_ORDERS_SYNC = Origins.identifier("courier_request_orders_sync");
    
    // Константы для ограничений
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_ITEMS_PER_CATEGORY = 10;
    public static final int MAX_ITEM_COUNT = 64;
    public static final int MAX_ORDERS_PER_PLAYER = 5;
    public static final long ORDER_EXPIRY_TIME = 24 * 60 * 60 * 1000L; // 24 часа в миллисекундах
    
    // Константы для UI
    public static final int ORDERS_LIST_WIDTH = 400;
    public static final int ORDERS_LIST_HEIGHT = 300;
    public static final int ORDER_DETAILS_WIDTH = 450;
    public static final int ORDER_DETAILS_HEIGHT = 350;
    public static final int CREATE_ORDER_WIDTH = 500;
    public static final int CREATE_ORDER_HEIGHT = 400;
    public static final int ITEM_PICKER_WIDTH = 600;
    public static final int ITEM_PICKER_HEIGHT = 450;
    
    // Цветовая схема
    public static final int COLOR_PRIMARY = 0x4A90E2;      // Синий
    public static final int COLOR_ACCENT = 0x7ED321;       // Зеленый
    public static final int COLOR_WARNING = 0xF5A623;      // Оранжевый
    public static final int COLOR_ERROR = 0xD0021B;        // Красный
    public static final int COLOR_TEXT = 0xFFFFFF;         // Белый
    public static final int COLOR_TEXT_SECONDARY = 0xAAAAAA; // Светло-серый
    public static final int COLOR_BACKGROUND = 0x2C2C2C;   // Темно-серый
    public static final int COLOR_BACKGROUND_LIGHT = 0x3C3C3C; // Светло-серый фон
    
    // Константы анимации
    public static final int ANIMATION_DURATION_SHORT = 150;  // мс
    public static final int ANIMATION_DURATION_MEDIUM = 300; // мс
    public static final int ANIMATION_DURATION_LONG = 500;   // мс
    
    // Константы для поиска предметов
    public static final int ITEMS_PER_PAGE = 50;
    public static final int SEARCH_DELAY_MS = 300; // Задержка перед поиском при вводе
    
    /**
     * Проверяет, является ли описание валидным
     */
    public static boolean isValidDescription(String description) {
        return description != null && 
               !description.trim().isEmpty() && 
               description.length() <= MAX_DESCRIPTION_LENGTH;
    }
    
    /**
     * Обрезает описание до максимальной длины
     */
    public static String truncateDescription(String description) {
        if (description == null) return "";
        if (description.length() <= MAX_DESCRIPTION_LENGTH) return description;
        return description.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
    }
    
    /**
     * Проверяет, является ли количество предмета валидным
     */
    public static boolean isValidItemCount(int count) {
        return count > 0 && count <= MAX_ITEM_COUNT;
    }
    
    /**
     * Нормализует количество предмета к валидному диапазону
     */
    public static int normalizeItemCount(int count) {
        return Math.max(1, Math.min(count, MAX_ITEM_COUNT));
    }
    
    /**
     * Проверяет, не превышает ли заказ лимиты по количеству предметов
     */
    public static boolean isValidItemList(java.util.List<?> items) {
        return items != null && items.size() <= MAX_ITEMS_PER_CATEGORY;
    }
    
    /**
     * Получает отформатированное время создания заказа
     */
    public static String formatOrderTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60 * 1000) { // Меньше минуты
            return "только что";
        } else if (diff < 60 * 60 * 1000) { // Меньше часа
            long minutes = diff / (60 * 1000);
            return minutes + " мин. назад";
        } else if (diff < 24 * 60 * 60 * 1000) { // Меньше дня
            long hours = diff / (60 * 60 * 1000);
            return hours + " ч. назад";
        } else { // Больше дня
            long days = diff / (24 * 60 * 60 * 1000);
            return days + " дн. назад";
        }
    }
    
    /**
     * Проверяет, истек ли срок заказа
     */
    public static boolean isOrderExpired(long createdTime) {
        return System.currentTimeMillis() - createdTime > ORDER_EXPIRY_TIME;
    }
}