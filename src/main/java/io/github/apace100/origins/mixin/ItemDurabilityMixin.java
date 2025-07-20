package io.github.apace100.origins.mixin;

import io.github.apace100.origins.util.ItemQualityHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для модификации прочности предметов в зависимости от качества
 */
@Mixin(ItemStack.class)
public class ItemDurabilityMixin {
    
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void modifyMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        int modifiedDamage = ItemQualityHelper.getModifiedMaxDamage(stack);
        
        if (modifiedDamage != cir.getReturnValue()) {
            cir.setReturnValue(modifiedDamage);
        }
    }
}