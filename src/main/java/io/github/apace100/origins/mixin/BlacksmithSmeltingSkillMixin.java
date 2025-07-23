package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.BlacksmithSkillHandler;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для обработки навыков кузнеца при плавке
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public class BlacksmithSmeltingSkillMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private static void origins$handleBlacksmithSmelting(World world, BlockPos pos, 
            net.minecraft.block.BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        
        if (world.isClient) {
            return;
        }
        
        try {
            // Ищем ближайшего кузнеца
            PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 16.0, false);
            
            if (!(nearestPlayer instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }
            
            if (!BlacksmithSkillHandler.isBlacksmith(serverPlayer)) {
                return;
            }
            
            PlayerSkillComponent skillComponent = PlayerSkillComponent.KEY.get(serverPlayer);
            if (skillComponent == null) {
                return;
            }
            
            // Проверяем навык удвоения слитков
            int doubleIngotLevel = skillComponent.getSkillLevel("double_ingot");
            if (doubleIngotLevel > 0) {
                ItemStack result = blockEntity.getStack(2); // Слот результата
                if (!result.isEmpty()) {
                    ItemStack doubledResult = BlacksmithSkillHandler.handleDoubleIngot(serverPlayer, result, doubleIngotLevel);
                    if (doubledResult.getCount() > result.getCount()) {
                        blockEntity.setStack(2, doubledResult);
                    }
                }
            }
            
            // Проверяем навык мастера горна
            int forgeMasterLevel = skillComponent.getSkillLevel("forge_master");
            if (forgeMasterLevel > 0) {
                BlacksmithSkillHandler.handleForgeMaster(serverPlayer, forgeMasterLevel);
                
                // Ускоряем плавку на 50%
                var nbt = blockEntity.createNbt();
                if (nbt.getBoolean("ForgeMasterBoost")) {
                    // Уменьшаем время готовки (упрощенная реализация)
                    // В реальной реализации нужно изменить cookTime
                }
            }
            
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в BlacksmithSmeltingSkillMixin: " + e.getMessage(), e);
        }
    }
}