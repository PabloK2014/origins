package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.QuestEventHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для отслеживания крафта предметов в квестах
 */
@Mixin(CraftingScreenHandler.class)
public class QuestCraftingMixin {
    
    @Inject(method = "quickMove", at = @At("HEAD"))
    private void onQuickCraft(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (player == null || player.getWorld().isClient) {
            return;
        }
        
        CraftingScreenHandler handler = (CraftingScreenHandler) (Object) this;
        
        // Проверяем, что это слот результата крафта (индекс 0)
        if (index == 0) {
            Slot resultSlot = handler.getSlot(0);
            ItemStack resultStack = resultSlot.getStack();
            
            if (!resultStack.isEmpty()) {
                // Отслеживаем крафт предмета для квестов
                QuestEventHandlers.onItemCraft(player, resultStack.copy());
            }
        }
    }
    

}