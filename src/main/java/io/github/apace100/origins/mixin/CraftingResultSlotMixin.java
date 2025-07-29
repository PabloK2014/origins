package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.QuestEventHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для отслеживания взятия предметов из слота результата крафта
 */
@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {
    
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void onCraftedItemTaken(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player == null || stack.isEmpty() || player.getWorld().isClient) {
            return;
        }
        
        // Отслеживаем крафт предмета для квестов
        QuestEventHandlers.onItemCraft(player, stack.copy());
    }
}