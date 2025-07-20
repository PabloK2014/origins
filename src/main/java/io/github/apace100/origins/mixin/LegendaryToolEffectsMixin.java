package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.BlacksmithQualityCraftingPower;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для легендарных эффектов инструментов
 * Применяется к методу canHarvest в Block классе
 */
@Mixin(Block.class)
public class LegendaryToolEffectsMixin {
    
    @Inject(method = "onBreak", at = @At("HEAD"))
    private void applyLegendaryToolEffects(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if (world.isClient || player == null) {
            return;
        }
        
        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty() || !BlacksmithQualityCraftingPower.isLegendary(tool)) {
            return;
        }
        
        // Применяем эффекты в зависимости от типа инструмента
        if (tool.getItem() instanceof PickaxeItem) {
            applyLegendaryPickaxeEffects(world, pos, state, player, tool);
        } else if (tool.getItem() instanceof AxeItem) {
            applyLegendaryAxeEffects(world, pos, state, player, tool);
        } else if (tool.getItem() instanceof ShovelItem) {
            applyLegendaryShovelEffects(world, pos, state, player, tool);
        }
    }
    
    private void applyLegendaryPickaxeEffects(World world, BlockPos pos, BlockState state, 
                                            PlayerEntity player, ItemStack tool) {
        
        // Копание 3x3
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Пропускаем центральный блок
                    
                    BlockPos targetPos = pos.add(x, y, z);
                    BlockState targetState = world.getBlockState(targetPos);
                    
                    // Проверяем, может ли кирка добыть этот блок
                    if (tool.isSuitableFor(targetState) && targetState.getHardness(world, targetPos) >= 0) {
                        // Ломаем блок
                        world.breakBlock(targetPos, true, player);
                        
                        // Изнашиваем инструмент
                        tool.damage(1, player, (p) -> p.sendToolBreakStatus(player.getActiveHand()));
                        
                        if (tool.getDamage() >= tool.getMaxDamage()) {
                            return; // Прекращаем, если инструмент сломался
                        }
                    }
                }
            }
        }
    }
    
    private void applyLegendaryAxeEffects(World world, BlockPos pos, BlockState state, 
                                        PlayerEntity player, ItemStack tool) {
        
        // Удвоенный дроп древесины
        if (state.isIn(BlockTags.LOGS)) {
            // Создаем дополнительный дроп
            ItemStack logDrop = new ItemStack(state.getBlock().asItem());
            
            // Спавним дополнительный предмет в мире
            ItemEntity itemEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, logDrop);
            itemEntity.setToDefaultPickupDelay();
            world.spawnEntity(itemEntity);
        }
    }
    
    private void applyLegendaryShovelEffects(World world, BlockPos pos, BlockState state, 
                                           PlayerEntity player, ItemStack tool) {
        Block block = state.getBlock();
        
        // 20% шанс удвоенного дропа с мягких блоков
        if (isSoftBlock(block) && player.getRandom().nextFloat() < 0.20f) {
            // Создаем дополнительный дроп
            ItemStack blockDrop = new ItemStack(block.asItem());
            
            // Спавним дополнительный предмет в мире
            ItemEntity itemEntity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, blockDrop);
            itemEntity.setToDefaultPickupDelay();
            world.spawnEntity(itemEntity);
        }
    }
    
    private boolean isSoftBlock(Block block) {
        return block == Blocks.SAND || 
               block == Blocks.RED_SAND || 
               block == Blocks.GRAVEL || 
               block == Blocks.DIRT || 
               block == Blocks.GRASS_BLOCK || 
               block == Blocks.COARSE_DIRT || 
               block == Blocks.PODZOL || 
               block == Blocks.MYCELIUM ||
               block == Blocks.SOUL_SAND ||
               block == Blocks.SOUL_SOIL ||
               block == Blocks.CLAY;
    }
}