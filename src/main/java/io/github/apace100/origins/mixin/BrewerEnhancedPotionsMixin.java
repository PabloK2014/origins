package io.github.apace100.origins.mixin;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.origins.power.BrewerEnhancedPotionsPower;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для улучшения зелий пивовара
 * Увеличивает длительность зелий, сваренных игроком с происхождением пивовара
 */
@Mixin(BrewingStandBlockEntity.class)
public class BrewerEnhancedPotionsMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private static void enhancePotionDuration(net.minecraft.world.World world, BlockPos pos, net.minecraft.block.BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        if (!world.isClient) {
            // Ищем ближайшего игрока с силой пивовара
            PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
            
            if (nearestPlayer instanceof ServerPlayerEntity serverPlayer) {
                // Проверяем, есть ли у игрока сила пивовара
                PowerHolderComponent.getPowers(serverPlayer, BrewerEnhancedPotionsPower.class).forEach(power -> {
                    if (power.shouldEnhancePotion()) {
                        // Проходим по слотам с зельями (0, 1, 2)
                        for (int i = 0; i < 3; i++) {
                            ItemStack stack = blockEntity.getStack(i);
                            
                            if (isPotionItem(stack)) {
                                enhancePotionStack(stack, power.getDurationMultiplier());
                            }
                        }
                    }
                });
            }
        }
    }
    
    private static boolean isPotionItem(ItemStack stack) {
        return stack.getItem() == Items.POTION || 
               stack.getItem() == Items.SPLASH_POTION || 
               stack.getItem() == Items.LINGERING_POTION;
    }
    
    private static void enhancePotionStack(ItemStack stack, float multiplier) {
        // Проверяем, не было ли зелье уже улучшено
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getBoolean("BrewerEnhanced")) {
            return; // Уже улучшено
        }
        
        if (nbt.contains("CustomPotionEffects")) {
            NbtList effects = nbt.getList("CustomPotionEffects", 10);
            
            for (int i = 0; i < effects.size(); i++) {
                NbtCompound effect = effects.getCompound(i);
                if (effect.contains("Duration")) {
                    int currentDuration = effect.getInt("Duration");
                    int newDuration = Math.round(currentDuration * multiplier);
                    effect.putInt("Duration", newDuration);
                }
            }
        } else {
            // Для стандартных зелий используем PotionUtil
            var potionEffects = PotionUtil.getPotionEffects(stack);
            if (!potionEffects.isEmpty()) {
                var enhancedEffects = potionEffects.stream()
                    .map(effect -> new net.minecraft.entity.effect.StatusEffectInstance(
                        effect.getEffectType(),
                        Math.round(effect.getDuration() * multiplier),
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.shouldShowParticles(),
                        effect.shouldShowIcon()
                    ))
                    .collect(java.util.stream.Collectors.toList());
                PotionUtil.setCustomPotionEffects(stack, enhancedEffects);
            }
        }
        
        // Помечаем зелье как улучшенное
        nbt.putBoolean("BrewerEnhanced", true);
    }
}