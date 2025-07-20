package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для легендарных эффектов мотыги
 */
@Mixin(HoeItem.class)
public class LegendaryHoeMixin {
    
    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void applyLegendaryHoeEffect(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        if (world.isClient) {
            return;
        }
        
        PlayerEntity player = context.getPlayer();
        ItemStack hoe = context.getStack();
        
        if (player == null || !BlacksmithQualityCraftingPower.isLegendary(hoe)) {
            return;
        }
        
        // Проверяем, что действие было успешным (SUCCESS или CONSUME)
        ActionResult result = cir.getReturnValue();
        if (result != ActionResult.SUCCESS && result != ActionResult.CONSUME) {
            return;
        }
        
        // Возделывание 3x3
        BlockPos centerPos = context.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Пропускаем центральный блок (уже обработан)
                
                BlockPos targetPos = centerPos.add(x, 0, z);
                BlockState targetState = world.getBlockState(targetPos);
                
                // Проверяем, можно ли возделать этот блок
                if (canTillBlock(targetState)) {
                    // Возделываем блок
                    world.setBlockState(targetPos, Blocks.FARMLAND.getDefaultState());
                    
                    // Изнашиваем инструмент
                    hoe.damage(1, player, (p) -> p.sendToolBreakStatus(player.getActiveHand()));
                    
                    if (hoe.getDamage() >= hoe.getMaxDamage()) {
                        break; // Прекращаем, если инструмент сломался
                    }
                }
            }
        }
    }
    
    private boolean canTillBlock(BlockState state) {
        return state.isOf(Blocks.DIRT) || 
               state.isOf(Blocks.GRASS_BLOCK) || 
               state.isOf(Blocks.COARSE_DIRT) || 
               state.isOf(Blocks.PODZOL) ||
               state.isOf(Blocks.MYCELIUM);
    }
}