package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.util.CookHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для начисления опыта повару при быстром перемещении еды (Shift+Click)
 */
@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    
    @Inject(method = "quickMove", at = @At("HEAD"))
    private void origins$giveCookExpOnQuickMove(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        // Быстрая проверка - только для серверной стороны
        if (player.getWorld().isClient) {
            return;
        }
        
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ScreenHandler handler = (ScreenHandler) (Object) this;
            
            // Проверяем, что слот существует
            if (index >= 0 && index < handler.slots.size()) {
                Slot slot = handler.slots.get(index);
                ItemStack stack = slot.getStack();
                
                CookHelper.processCookingExperience(serverPlayer, slot, stack, "Shift+ЛКМ");
            }
        }
    }

}