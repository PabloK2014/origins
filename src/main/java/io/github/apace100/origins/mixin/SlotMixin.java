package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.util.CookHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для начисления опыта повару при взятии еды из устройств приготовления
 */
@Mixin(Slot.class)
public class SlotMixin {
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void origins$giveCookExp(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        // Быстрая проверка - только для серверной стороны
        if (player.getWorld().isClient) {
            return;
        }
        
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Slot slot = (Slot) (Object) this;
            
            // Дополнительное логирование для отладки Shift+ЛКМ
            Origins.LOGGER.info("=== SlotMixin: onTakeItem вызван ===");
            Origins.LOGGER.info("Игрок: {}", serverPlayer.getName().getString());
            Origins.LOGGER.info("Предмет: {}", stack.isEmpty() ? "пустой" : stack.getItem().toString());
            Origins.LOGGER.info("Количество: {}", stack.getCount());
            Origins.LOGGER.info("Слот: {}, Инвентарь: {}", slot.getIndex(), 
                slot.inventory != null ? slot.inventory.getClass().getSimpleName() : "null");
            
            CookHelper.processCookingExperience(serverPlayer, slot, stack, "взятие");
        }
    }
    


} 