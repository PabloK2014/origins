package io.github.apace100.origins.mixin;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import io.github.apace100.origins.power.CustomOriginsPowerTypes;
import io.github.apace100.origins.util.ItemQualityHelper;
import io.github.apace100.origins.Origins;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для системы качества крафта кузнеца
 * Применяет случайное качество к созданным инструментам и броне
 */
@Mixin(CraftingScreenHandler.class)
public class BlacksmithQualityCraftingMixin {

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void applyQualityCrafting(CallbackInfo ci) {
        CraftingScreenHandler handler = (CraftingScreenHandler) (Object) this;
        
        // Получаем игрока через поиск PlayerInventory в слотах
        PlayerEntity player = null;
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.inventory instanceof PlayerInventory playerInventory) {
                player = playerInventory.player;
                break;
            }
        }
        
        if (player != null && !player.getWorld().isClient) {
            final PlayerEntity finalPlayer = player;
            
            // Проверяем, есть ли у игрока сила кузнеца через PowerHolderComponent
            PowerHolderComponent powerHolder = PowerHolderComponent.KEY.get(finalPlayer);
            if (powerHolder.hasPower(CustomOriginsPowerTypes.BLACKSMITH_QUALITY_CRAFTING)) {
                BlacksmithQualityCraftingPower power = (BlacksmithQualityCraftingPower) 
                    powerHolder.getPower(CustomOriginsPowerTypes.BLACKSMITH_QUALITY_CRAFTING);
                
                if (power != null && power.shouldApplyQuality()) {
                    // Проверяем слот результата (индекс 0)
                    Slot resultSlot = handler.getSlot(0);
                    ItemStack resultStack = resultSlot.getStack();
                    
                    if (!resultStack.isEmpty() && isCraftableItem(resultStack)) {
                        applyQualityToItem(resultStack, power, finalPlayer.getRandom());
                    }
                }
            }
        }
    }
    
    private boolean isCraftableItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ToolItem || 
               item instanceof ArmorItem || 
               item instanceof SwordItem ||
               item instanceof BowItem ||
               item instanceof CrossbowItem ||
               item instanceof TridentItem ||
               item instanceof ShieldItem;
    }
    
    private void applyQualityToItem(ItemStack stack, BlacksmithQualityCraftingPower power, Random random) {
        // Проверяем, что предмет еще не имеет качества ИЛИ имеет только обычное качество
        BlacksmithQualityCraftingPower.ItemQuality currentQuality = BlacksmithQualityCraftingPower.getItemQuality(stack);
        
        // Применяем качество только если предмет новый (без NBT качества) или имеет обычное качество
        if (currentQuality == BlacksmithQualityCraftingPower.ItemQuality.NORMAL && 
            (!stack.hasNbt() || !stack.getNbt().contains("ItemQuality"))) {
            
            // Определяем качество на основе силы игрока
            BlacksmithQualityCraftingPower.ItemQuality quality = power.determineQuality(random);
            
            // Применяем качество к предмету
            ItemStack qualityItem = power.applyQualityToItem(stack, quality);
            
            // Копируем NBT данные обратно в оригинальный стек
            stack.setNbt(qualityItem.getNbt());
            
            // Применяем модификаторы качества
            ItemQualityHelper.applyQualityModifiers(stack);
        }
    }
    

}