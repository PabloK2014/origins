package io.github.apace100.origins.mixin;

import io.github.apace100.origins.progression.OriginProgressionComponent;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
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
 * Миксин для начисления опыта за различные действия
 */
@Mixin(PlayerEntity.class)
public class ExperienceGainMixin {
    
    /**
     * Опыт за разрушение блоков (для шахтера)
     */
    @Inject(method = "onBlockBroken", at = @At("HEAD"))
    private void onBlockBroken(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (world.isClient) return;
        
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        
        OriginProgressionComponent component = OriginProgressionComponent.KEY.get(serverPlayer);
        String currentOrigin = getCurrentOriginId(serverPlayer);
        
        if (currentOrigin == null) return;
        
        int exp = 0;
        
        switch (currentOrigin) {
            case "origins:miner" -> {
                // Шахтер получает опыт за добычу блоков
                if (isOreBlock(state)) {
                    exp = 15; // Больше опыта за руду
                } else if (isStoneBlock(state)) {
                    exp = 5; // Меньше опыта за камень
                } else if (isDirtBlock(state)) {
                    exp = 2; // Минимум за землю
                }
            }
            case "origins:blacksmith" -> {
                // Кузнец получает опыт за добычу металлических руд
                if (isMetalOre(state)) {
                    exp = 20;
                } else if (isStoneBlock(state)) {
                    exp = 3;
                }
            }
            case "origins:cook" -> {
                // Повар получает опыт за сбор урожая
                if (isCropBlock(state)) {
                    exp = 10;
                } else if (isWoodBlock(state)) {
                    exp = 3;
                }
            }
        }
        
        if (exp > 0) {
            component.addExperience(exp);
        }
    }
    
    /**
     * Опыт за убийство мобов (для воина)
     */
    @Inject(method = "onKilledOther", at = @At("HEAD"))
    private void onKilledOther(ServerPlayerEntity serverPlayer, LivingEntity other, CallbackInfo ci) {
        OriginProgressionComponent component = OriginProgressionComponent.KEY.get(serverPlayer);
        String currentOrigin = getCurrentOriginId(serverPlayer);
        
        if (currentOrigin == null) return;
        
        int exp = 0;
        
        switch (currentOrigin) {
            case "origins:warrior" -> {
                // Воин получает опыт за убийство мобов
                if (isHostileMob(other)) {
                    exp = 25; // Больше опыта за враждебных мобов
                } else if (isNeutralMob(other)) {
                    exp = 15; // Меньше за нейтральных
                } else {
                    exp = 5; // Минимум за мирных
                }
            }
            case "origins:courier" -> {
                // Курьер получает опыт за убийство (меньше воина)
                if (isHostileMob(other)) {
                    exp = 15;
                } else {
                    exp = 5;
                }
            }
        }
        
        if (exp > 0) {
            component.addExperience(exp);
        }
    }
    
    // Вспомогательные методы для определения типов блоков
    private boolean isOreBlock(BlockState state) {
        String blockName = state.getBlock().getTranslationKey();
        return blockName.contains("ore") || blockName.contains("coal") || 
               blockName.contains("diamond") || blockName.contains("emerald");
    }
    
    private boolean isMetalOre(BlockState state) {
        String blockName = state.getBlock().getTranslationKey();
        return blockName.contains("iron_ore") || blockName.contains("gold_ore") || 
               blockName.contains("copper_ore");
    }
    
    private boolean isStoneBlock(BlockState state) {
        String blockName = state.getBlock().getTranslationKey();
        return blockName.contains("stone") || blockName.contains("cobblestone");
    }
    
    private boolean isDirtBlock(BlockState state) {
        String blockName = state.getBlock().getTranslationKey();
        return blockName.contains("dirt") || blockName.contains("grass");
    }
    
    private boolean isCropBlock(BlockState state) {
        String blockName = state.getBlock().getTranslationKey();
        return blockName.contains("wheat") || blockName.contains("carrot") || 
               blockName.contains("potato") || blockName.contains("beetroot");
    }
    
    private boolean isWoodBlock(BlockState state) {
        String blockName = state.getBlock().getTranslationKey();
        return blockName.contains("log") || blockName.contains("wood");
    }
    
    private boolean isHostileMob(LivingEntity entity) {
        String entityName = entity.getType().getTranslationKey();
        return entityName.contains("zombie") || entityName.contains("skeleton") || 
               entityName.contains("spider") || entityName.contains("creeper") ||
               entityName.contains("enderman") || entityName.contains("witch");
    }
    
    private boolean isNeutralMob(LivingEntity entity) {
        String entityName = entity.getType().getTranslationKey();
        return entityName.contains("iron_golem") || entityName.contains("wolf") ||
               entityName.contains("bee") || entityName.contains("dolphin");
    }
    
    /**
     * Получить ID текущего происхождения игрока
     */
    private String getCurrentOriginId(ServerPlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.component.OriginComponent.KEY.get(player);
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(
                io.github.apace100.origins.Origins.identifier("origin")));
            
            return origin != null ? origin.getIdentifier().toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}