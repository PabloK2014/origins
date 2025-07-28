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

/**
 * Миксин для отслеживания крафта предметов в квестах
 */
@Mixin(CraftingScreenHandler.class)
public class QuestCraftingMixin {
    
    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void onCraftingContentChanged(CallbackInfo ci) {
        CraftingScreenHandler handler = (CraftingScreenHandler) (Object) this;
        
        // Получаем игрока через поиск PlayerInventory в слотах
        PlayerEntity player = null;
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory playerInventory) {
                player = playerInventory.player;
                break;
            }
        }
        
        if (player == null || player.getWorld().isClient) {
            return;
        }
        
        // Проверяем слот результата (индекс 0)
        Slot resultSlot = handler.getSlot(0);
        ItemStack resultStack = resultSlot.getStack();
        
        if (!resultStack.isEmpty()) {
            // Отслеживаем крафт предмета для квестов
            QuestEventHandlers.onItemCraft(player, resultStack);
        }
    }
}