package io.github.apace100.origins.mixin;

import io.github.apace100.origins.progression.OriginProgressionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для начисления опыта за крафт, готовку и варку зелий
 */
@Mixin({CraftingScreenHandler.class, FurnaceScreenHandler.class, BrewingStandScreenHandler.class})
public class CraftingExperienceMixin {
    
    /**
     * Опыт за быстрое перемещение предметов (крафт)
     */
    @Inject(method = "quickMove", at = @At("RETURN"))
    private void onQuickMove(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        
        ItemStack result = cir.getReturnValue();
        if (result.isEmpty()) return;
        
        OriginProgressionComponent component = OriginProgressionComponent.KEY.get(serverPlayer);
        String currentOrigin = getCurrentOriginId(serverPlayer);
        
        if (currentOrigin == null) return;
        
        int exp = 0;
        Object handler = this;
        
        if (handler instanceof CraftingScreenHandler) {
            // Опыт за крафт
            switch (currentOrigin) {
                case "origins:blacksmith" -> {
                    if (isToolOrWeapon(result) || isArmor(result)) {
                        exp = 30; // Больше опыта за инструменты и броню
                    } else if (isMetalItem(result)) {
                        exp = 15; // Меньше за металлические предметы
                    } else {
                        exp = 5; // Минимум за остальное
                    }
                }
                case "origins:cook" -> {
                    if (isFood(result)) {
                        exp = 20; // Опыт за еду
                    } else if (isCookingTool(result)) {
                        exp = 15; // За кухонные инструменты
                    } else {
                        exp = 3;
                    }
                }
                case "origins:courier" -> {
                    if (isTransportItem(result)) {
                        exp = 25; // За предметы для транспорта
                    } else {
                        exp = 5;
                    }
                }
                default -> exp = 2; // Минимальный опыт для всех остальных
            }
        } else if (handler instanceof FurnaceScreenHandler) {
            // Опыт за готовку в печи
            switch (currentOrigin) {
                case "origins:cook" -> exp = 25; // Повар получает больше опыта за готовку
                case "origins:blacksmith" -> {
                    if (isMetalItem(result)) {
                        exp = 20; // Кузнец за переплавку металла
                    } else {
                        exp = 5;
                    }
                }
                default -> exp = 5;
            }
        } else if (handler instanceof BrewingStandScreenHandler) {
            // Опыт за варку зелий
            switch (currentOrigin) {
                case "origins:brewer" -> exp = 35; // Алхимик получает больше опыта за зелья
                case "origins:cook" -> exp = 15; // Повар тоже умеет варить
                default -> exp = 10;
            }
        }
        
        if (exp > 0) {
            component.addExperience(exp);
        }
    }
    
    // Вспомогательные методы для определения типов предметов
    private boolean isToolOrWeapon(ItemStack stack) {
        String itemName = stack.getItem().getTranslationKey();
        return itemName.contains("sword") || itemName.contains("pickaxe") || 
               itemName.contains("axe") || itemName.contains("shovel") || 
               itemName.contains("hoe") || itemName.contains("bow");
    }
    
    private boolean isArmor(ItemStack stack) {
        String itemName = stack.getItem().getTranslationKey();
        return itemName.contains("helmet") || itemName.contains("chestplate") || 
               itemName.contains("leggings") || itemName.contains("boots");
    }
    
    private boolean isMetalItem(ItemStack stack) {
        String itemName = stack.getItem().getTranslationKey();
        return itemName.contains("iron") || itemName.contains("gold") || 
               itemName.contains("copper") || itemName.contains("ingot");
    }
    
    private boolean isFood(ItemStack stack) {
        return stack.getItem().isFood();
    }
    
    private boolean isCookingTool(ItemStack stack) {
        String itemName = stack.getItem().getTranslationKey();
        return itemName.contains("furnace") || itemName.contains("smoker") || 
               itemName.contains("campfire") || itemName.contains("cauldron");
    }
    
    private boolean isTransportItem(ItemStack stack) {
        String itemName = stack.getItem().getTranslationKey();
        return itemName.contains("boat") || itemName.contains("minecart") || 
               itemName.contains("saddle") || itemName.contains("lead");
    }
    
    /**
     * Получить ID текущего происхождения игрока
     */
    private String getCurrentOriginId(ServerPlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.component.OriginComponent.KEY.get(player);
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(
                io.github.apace100.origins.Origins.identifier("origin")));
            
            return origin != null ? origin.getIdentifier().toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}