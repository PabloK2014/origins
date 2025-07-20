package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import io.github.apace100.origins.util.ItemQualityHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для модификации скорости добычи инструментов в зависимости от качества
 */
@Mixin(PlayerEntity.class)
public class MiningSpeedMixin {
    
    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void modifyMiningSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemStack tool = player.getMainHandStack();
        
        if (!tool.isEmpty() && tool.getItem() instanceof MiningToolItem) {
            float speedModifier = ItemQualityHelper.getMiningSpeedModifier(tool);
            
            if (speedModifier != 0.0f) {
                float originalSpeed = cir.getReturnValue();
                float modifiedSpeed = originalSpeed * (1.0f + speedModifier);
                cir.setReturnValue(modifiedSpeed);
            }
        }
    }
}