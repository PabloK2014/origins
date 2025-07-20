package io.github.apace100.origins.mixin;

import io.github.apace100.origins.util.BlacksmithQualityHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для обработки прочности предметов с качеством кузнеца
 */
@Mixin(ItemStack.class)
public class BlacksmithQualityDurabilityMixin {

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void getEnhancedMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        
        if (BlacksmithQualityHelper.hasQuality(stack)) {
            int enhancedMaxDamage = BlacksmithQualityHelper.getEnhancedMaxDamage(stack);
            cir.setReturnValue(enhancedMaxDamage);
        }
    }
    
    @Inject(method = "isDamageable", at = @At("RETURN"), cancellable = true)
    private void checkDamageableWithQuality(CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        
        if (BlacksmithQualityHelper.hasQuality(stack)) {
            // Если предмет имеет качество кузнеца, он всегда считается повреждаемым
            // если его базовая максимальная прочность больше 0
            cir.setReturnValue(stack.getItem().getMaxDamage() > 0);
        }
    }
}