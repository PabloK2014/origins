package io.github.apace100.origins.util;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Утилитарный класс для работы с поваром
 */
public class CookHelper {
    
    /**
     * Проверяет, является ли игрок поваром (по происхождению или профессии)
     */
    public static boolean isPlayerCook(ServerPlayerEntity player) {
        try {
            // Проверка по профессии
            ProfessionComponent comp = ProfessionComponent.KEY.get(player);
            if (comp != null) {
                Identifier professionId = comp.getCurrentProfessionId();
                if (professionId != null && "origins:cook".equals(professionId.toString())) {
                    return true;
                }
            }
            
            // Проверка по происхождению
            OriginComponent originComponent = ModComponents.ORIGIN.get(player);
            if (originComponent != null) {
                var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                if (origin != null && "origins:cook".equals(origin.getIdentifier().toString())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при проверке, является ли игрок поваром: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Начисляет опыт повару
     */
    public static void giveCookExperience(ServerPlayerEntity player, int amount, String source) {
        try {
            ProfessionComponent comp = ProfessionComponent.KEY.get(player);
            if (comp != null) {
                comp.addExperience(amount);
                
                player.sendMessage(
                    net.minecraft.text.Text.literal("+" + amount + " опыта за приготовление [" + source + "]")
                        .formatted(net.minecraft.util.Formatting.GREEN), 
                    false
                );
                
                Origins.LOGGER.info("Повару {} начислено {} опыта за приготовление [{}]", 
                    player.getName().getString(), amount, source);
            } else {
                Origins.LOGGER.error("Не удалось получить компонент профессии для игрока {}", 
                    player.getName().getString());
            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при начислении опыта повару: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет, является ли слот результатом приготовления в устройстве
     */
    public static boolean isCookingResultSlot(net.minecraft.screen.slot.Slot slot) {
        if (slot == null || slot.inventory == null) {
            return false;
        }
        
        String invClass = slot.inventory.getClass().getName().toLowerCase();
        int slotIdx = slot.getIndex();
        
        // Слот с индексом 2 обычно является слотом результата в печах и коптильнях
        return slotIdx == 2 && (
            invClass.contains("furnace") || // Печь
            invClass.contains("smoker") ||  // Коптильня
            invClass.contains("campfire")   // Костер
        );
    }
    
    /**
     * Получает базовое количество опыта за еду в зависимости от её типа
     */
    public static int getBaseExperienceForFood(net.minecraft.item.ItemStack stack) {
        if (stack == null || !stack.isFood()) {
            return 0;
        }
        
        var foodComponent = stack.getItem().getFoodComponent();
        if (foodComponent == null) {
            return 1;
        }
        
        // Базовый опыт зависит от питательности еды
        int nutrition = foodComponent.getHunger();
        float saturation = foodComponent.getSaturationModifier();
        
        // Формула: базовый опыт = питательность + (насыщение * 2)
        return Math.max(1, nutrition + (int)(saturation * 2));
    }
    
    /**
     * Получает бонусный опыт в зависимости от типа устройства приготовления
     */
    public static int getDeviceExperienceBonus(net.minecraft.screen.slot.Slot slot) {
        if (slot == null || slot.inventory == null) {
            return 0;
        }
        
        String invClass = slot.inventory.getClass().getName().toLowerCase();
        
        if (invClass.contains("smoker")) {
            return 2; // Коптильня дает больше опыта
        } else if (invClass.contains("furnace")) {
            return 1; // Печь дает стандартный бонус
        } else if (invClass.contains("campfire")) {
            return 1; // Костер дает стандартный бонус
        }
        
        return 0;
    }
    
    /**
     * Получает читаемое название устройства приготовления
     */
    public static String getDeviceName(String inventoryClassName) {
        if (inventoryClassName == null) {
            return "неизвестном устройстве";
        }
        
        String className = inventoryClassName.toLowerCase();
        
        if (className.contains("smoker")) {
            return "коптильне";
        } else if (className.contains("furnace")) {
            return "печи";
        } else if (className.contains("campfire")) {
            return "костре";
        }
        
        return "устройстве приготовления";
    }
    
    /**
     * Универсальный метод для начисления опыта повару за приготовление еды
     */
    public static void processCookingExperience(ServerPlayerEntity player, net.minecraft.screen.slot.Slot slot, net.minecraft.item.ItemStack stack, String source) {
        try {
                        
            if (stack == null || stack.isEmpty()) {
                                return;
            }
            
                        
            if (!stack.isFood()) {
                                return;
            }
            
                        
            String invClass = slot.inventory != null ? slot.inventory.getClass().getName() : "null";
            int slotIdx = slot.getIndex();
            
                        
            // Проверяем, что слот относится к результату приготовления
            boolean isCookingSlot = isCookingResultSlot(slot);
                        
            if (isCookingSlot) {
                // Проверяем, является ли игрок поваром
                boolean isCook = isPlayerCook(player);
                                
                if (isCook) {
                    // Используем продвинутую формулу расчета опыта
                    int baseExp = getBaseExperienceForFood(stack);
                    int deviceBonus = getDeviceExperienceBonus(slot);
                    int expAmount = (baseExp + deviceBonus) * stack.getCount();
                    
                    String deviceName = getDeviceName(slot.inventory.getClass().getName());
                    giveCookExperience(player, expAmount, deviceName + " (" + source + ")");
                } else {
                                    }
            } else {
                            }
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка при обработке опыта повару: " + e.getMessage(), e);
        }
    }
}