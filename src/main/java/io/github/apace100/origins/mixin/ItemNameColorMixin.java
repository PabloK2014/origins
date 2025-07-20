package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для изменения цвета названия предмета в зависимости от качества
 */
@Mixin(ItemStack.class)
public class ItemNameColorMixin {
    
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void modifyItemNameColor(CallbackInfoReturnable<Text> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        
        BlacksmithQualityCraftingPower.ItemQuality quality = 
            BlacksmithQualityCraftingPower.getItemQuality(stack);
        
        if (quality != BlacksmithQualityCraftingPower.ItemQuality.NORMAL) {
            Text originalName = cir.getReturnValue();
            
            // Создаем новое название с цветом качества
            MutableText coloredName = Text.empty()
                .append(originalName)
                .formatted(quality.getColor());
            
            cir.setReturnValue(coloredName);
        }
    }
}