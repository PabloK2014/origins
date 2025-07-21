package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для начисления опыта шахтеру за добычу блоков
 */
@Mixin(Block.class)
public class MinerExperienceMixin {

    @Inject(method = "onBreak", at = @At("TAIL"))
    private void giveExperienceForMining(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Проверяем, что игрок имеет происхождение шахтера
        if (!isPlayerMiner(serverPlayer)) {
            return;
        }
        
        // Начисляем опыт в зависимости от типа блока
        int expAmount = calculateMiningExperience(state);
        if (expAmount > 0) {
            ProfessionComponent component = ProfessionComponent.KEY.get(serverPlayer);
            component.addExperience(expAmount);
        }
    }
    
    /**
     * Проверяет, является ли игрок шахтером
     */
    private boolean isPlayerMiner(ServerPlayerEntity player) {
        try {
            OriginComponent originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                
            return origin != null && origin.getIdentifier().toString().equals("origins:miner");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Рассчитывает количество опыта за добычу блока
     */
    private int calculateMiningExperience(BlockState state) {
        Block block = state.getBlock();
        
        // Руды дают больше опыта
        if (isOreBlock(block)) {
            return 5; // Базовый опыт за руду
        }
        
        // Камень и подобные блоки
        if (block.getHardness() >= 1.5f) {
            return 1; // Базовый опыт за камень
        }
        
        return 0; // Другие блоки не дают опыта
    }
    
    /**
     * Проверяет, является ли блок рудой
     * Использует блочные теги для лучшей совместимости с модами
     */
    private boolean isOreBlock(Block block) {
        BlockState defaultState = block.getDefaultState();
        
        // Используем блочные теги для определения руд
        if (defaultState.isIn(BlockTags.COAL_ORES) ||
            defaultState.isIn(BlockTags.IRON_ORES) ||
            defaultState.isIn(BlockTags.GOLD_ORES) ||
            defaultState.isIn(BlockTags.DIAMOND_ORES) ||
            defaultState.isIn(BlockTags.EMERALD_ORES) ||
            defaultState.isIn(BlockTags.LAPIS_ORES) ||
            defaultState.isIn(BlockTags.REDSTONE_ORES) ||
            defaultState.isIn(BlockTags.COPPER_ORES)) {
            return true;
        }
        
        // Дополнительные руды, которые могут не входить в стандартные теги
        return block == Blocks.NETHER_GOLD_ORE || 
               block == Blocks.NETHER_QUARTZ_ORE || 
               block == Blocks.ANCIENT_DEBRIS;
    }
}