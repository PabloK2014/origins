package io.github.apace100.origins.mixin;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin для добавления информации о качестве кузнеца в tooltip предмета
 */
@Mixin(ItemStack.class)
public class BlacksmithQualityTooltipInfoMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void addQualityTooltip(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> tooltip = cir.getReturnValue();
        ItemStack stack = (ItemStack) (Object) this;
        
        if (stack.hasNbt()) {
            NbtCompound nbt = stack.getNbt();
            
            if (nbt != null && nbt.contains("BlacksmithQuality")) {
                String quality = nbt.getString("BlacksmithQuality");
                
                // Добавляем пустую строку для разделения
                tooltip.add(Text.literal(""));
                
                // Добавляем информацию о качестве
                switch (quality.toLowerCase()) {
                    case "плохое":
                    case "poor":
                        tooltip.add(Text.literal("Качество: ").formatted(Formatting.GRAY)
                                .append(Text.literal("Плохое").formatted(Formatting.RED)));
                        tooltip.add(Text.literal("• Прочность: -25%").formatted(Formatting.RED));
                        break;
                    case "обычное":
                    case "normal":
                        tooltip.add(Text.literal("Качество: ").formatted(Formatting.GRAY)
                                .append(Text.literal("Обычное").formatted(Formatting.GRAY)));
                        tooltip.add(Text.literal("• Стандартные характеристики").formatted(Formatting.GRAY));
                        break;
                    case "хорошее":
                    case "good":
                        tooltip.add(Text.literal("Качество: ").formatted(Formatting.GRAY)
                                .append(Text.literal("Хорошее").formatted(Formatting.YELLOW)));
                        tooltip.add(Text.literal("• Прочность: +50%").formatted(Formatting.GREEN));
                        break;
                    case "легендарное":
                    case "legendary":
                        tooltip.add(Text.literal("Качество: ").formatted(Formatting.GRAY)
                                .append(Text.literal("Легендарное").formatted(Formatting.GOLD)));
                        tooltip.add(Text.literal("• Прочность: +100%").formatted(Formatting.GREEN));
                        tooltip.add(Text.literal("• Особые свойства").formatted(Formatting.LIGHT_PURPLE));
                        break;
                }
                
                // Показываем текущую и максимальную прочность, если есть улучшенная прочность
                if (nbt.contains("EnhancedMaxDamage")) {
                    int maxDamage = nbt.getInt("EnhancedMaxDamage");
                    int currentDamage = stack.getDamage();
                    int durability = maxDamage - currentDamage;
                    
                    tooltip.add(Text.literal("Прочность: " + durability + "/" + maxDamage)
                            .formatted(Formatting.BLUE));
                }
            }
        }
    }
}