package io.github.apace100.origins.mixin;

import io.github.apace100.origins.progression.OriginProgressionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для обработки получения опыта за крафт
 */
@Mixin(CraftingScreenHandler.class)
public class CraftingExperienceMixin {

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void giveExperienceForCrafting(CallbackInfo ci) {
        CraftingScreenHandler handler = (CraftingScreenHandler) (Object) this;
        
        // Получаем игрока
        PlayerEntity player = null;
        for (int i = 0; i < handler.slots.size(); i++) {
            if (handler.slots.get(i).inventory instanceof net.minecraft.entity.player.PlayerInventory playerInventory) {
                player = playerInventory.player;
                break;
            }
        }
        
        if (player == null || player.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Проверяем, что в слоте результата есть предмет
        ItemStack resultStack = handler.getSlot(0).getStack();
        if (resultStack.isEmpty()) {
            return;
        }
        
        // Получаем компонент прогрессии
        OriginProgressionComponent component = OriginProgressionComponent.KEY.get(serverPlayer);
        
        // Проверяем, есть ли у игрока активное происхождение
        if (component.getCurrentProgression() != null) {
            // Даем небольшое количество опыта за любой крафт
            component.addExperience(1);
        }
    }
}