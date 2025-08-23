package io.github.apace100.origins.mixin;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.power.CookEnhancedFoodPower;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для улучшения еды, приготовленной поваром
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public class CookEnhancedFoodMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private static void enhanceCookedFood(net.minecraft.world.World world, BlockPos pos, net.minecraft.block.BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!world.isClient) {
            // Проверяем, есть ли что-то в слоте результата
            ItemStack resultStack = blockEntity.getStack(2);
            if (!resultStack.isEmpty() && resultStack.getItem().isFood()) {
                // Ищем ближайшего игрока с силой повара
                PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
                
                if (nearestPlayer instanceof ServerPlayerEntity serverPlayer) {
                    // Проверяем, есть ли у игрока сила повара
                    PowerHolderComponent.getPowers(serverPlayer, CookEnhancedFoodPower.class).forEach(power -> {
                        if (power.shouldEnhanceFood()) {
                            enhanceFoodStack(resultStack, power, serverPlayer);
                        }
                    });
                }
            }
        }
    }
    
    private static void enhanceFoodStack(ItemStack stack, CookEnhancedFoodPower power, PlayerEntity cook) {
        // Проверяем, не была ли еда уже улучшена
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getBoolean("CookEnhanced")) {
            return; // Уже улучшена
        }
        
        // Помечаем еду как приготовленную поваром, но БЕЗ имени повара для стакования
        nbt.putBoolean("CookEnhanced", true);
        // Убираем CookName чтобы еда могла стакаться
        // nbt.putString("CookName", cook.getName().getString());
        nbt.putFloat("NutritionMultiplier", power.getNutritionMultiplier());
        nbt.putFloat("SaturationMultiplier", power.getSaturationMultiplier());
        
          }
}