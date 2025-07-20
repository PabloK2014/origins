package io.github.apace100.origins.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для изменения цвета названия предметов в зависимости от качества кузнеца
 * Плохое - красный, обычное - серый, хорошее - желтый, легендарное - оранжевый
 */
@Mixin(ItemStack.class)
public class BlacksmithQualityTooltipMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void applyQualityColor(CallbackInfoReturnable<Text> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        
        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();
            
            if (nbt != null && nbt.contains("BlacksmithQuality")) {
                String quality = nbt.getString("BlacksmithQuality");
                Text originalName = cir.getReturnValue();
                
                // Определяем цвет на основе качества
                Formatting color = getColorForQuality(quality);
                
                // Создаем новый текст с правильным цветом
                MutableText coloredName;
                if (originalName instanceof MutableText) {
                    coloredName = ((MutableText) originalName).copy();
                } else {
                    coloredName = Text.literal(originalName.getString());
                }
                
                // Применяем цвет (правильно сохраняем результат)
                coloredName = coloredName.formatted(color);
                cir.setReturnValue(coloredName);
            }
        }
    }
    
    private Formatting getColorForQuality(String quality) {
        return switch (quality.toLowerCase()) {
            case "poor", "плохое" -> Formatting.RED;
            case "normal", "обычное" -> Formatting.GRAY;
            case "good", "хорошее" -> Formatting.YELLOW;
            case "legendary", "легендарное" -> Formatting.GOLD;
            default -> Formatting.WHITE;
        };
    }
}