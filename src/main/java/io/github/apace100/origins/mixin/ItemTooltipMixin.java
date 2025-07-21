package io.github.apace100.origins.mixin;

import io.github.apace100.origins.util.ItemQualityHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Миксин для добавления информации о качестве в tooltip предметов
 */
@Mixin(ItemStack.class)
public class ItemTooltipMixin {
    
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void addQualityTooltip(PlayerEntity player, TooltipContext context, 
                                  CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        List<Text> tooltip = cir.getReturnValue();
        
        ItemQualityHelper.addQualityTooltip(stack, tooltip);
        // Добавляем подпись для улучшенной еды повара
        if (stack.isFood() && stack.hasNbt() && stack.getNbt().getBoolean("CookEnhanced")) {
            tooltip.add(net.minecraft.text.Text.literal("Сытость +50 процентов").formatted(net.minecraft.util.Formatting.GREEN));
        }
    }
}