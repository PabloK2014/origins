package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Миксин для начисления опыта профессии за различные действия
 */
@Mixin(PlayerEntity.class)
public abstract class ProfessionExperienceMixin {
    
    /**
     * Опыт за разрушение блоков (для шахтера)
     */
    @Inject(method = "afterBreak", at = @At("HEAD"))
    private void onBlockBroken(World world, BlockState state, BlockPos pos, ItemStack stack, CallbackInfo ci) {
        if (world.isClient) return;
        
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity)) return;
        
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        Identifier currentProfessionId = component.getCurrentProfessionId();
        
        if (currentProfessionId == null) return;
        
        int exp = 0;
        String professionPath = currentProfessionId.getPath();
        
        switch (professionPath) {
            case "miner" -> {
                // Шахтер получает опыт за добычу блоков
                Block block = state.getBlock();
                String blockId = block.getTranslationKey();
                
                if (blockId.contains("ore")) {
                    exp = 15; // Больше опыта за руду
                } else if (blockId.contains("stone") || blockId.contains("deepslate")) {
                    exp = 5; // Меньше опыта за камень
                } else if (blockId.contains("dirt") || blockId.contains("gravel")) {
                    exp = 2; // Минимум за землю
                }
            }
            case "blacksmith" -> {
                // Кузнец получает небольшой опыт за добычу руды
                Block block = state.getBlock();
                String blockId = block.getTranslationKey();
                
                if (blockId.contains("ore")) {
                    exp = 5; // Небольшой опыт за руду
                }
            }
        }
        
        if (exp > 0) {
            component.addExperience(exp);
        }
    }
    
    /**
     * Опыт за убийство существ (для воина)
     */
    @Inject(method = "onKilledEntity", at = @At("HEAD"))
    private void onEntityKilled(LivingEntity entity, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity)) return;
        
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        Identifier currentProfessionId = component.getCurrentProfessionId();
        
        if (currentProfessionId == null) return;
        
        int exp = 0;
        String professionPath = currentProfessionId.getPath();
        
        if ("warrior".equals(professionPath)) {
            EntityType<?> entityType = entity.getType();
            
            if (entityType == EntityType.ZOMBIE || entityType == EntityType.SKELETON) {
                exp = 10; // Обычные мобы
            } else if (entityType == EntityType.CREEPER || entityType == EntityType.SPIDER) {
                exp = 15; // Более сложные мобы
            } else if (entityType == EntityType.ENDERMAN || entityType == EntityType.WITCH) {
                exp = 25; // Продвинутые мобы
            } else if (entity instanceof PlayerEntity) {
                exp = 50; // PvP
            } else {
                exp = 5; // Другие существа
            }
        }
        
        if (exp > 0) {
            component.addExperience(exp);
        }
    }
    
    /**
     * Опыт за употребление пищи (для повара)
     */
    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onFoodEaten(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity)) return;
        
        ProfessionComponent component = ProfessionComponent.KEY.get(player);
        Identifier currentProfessionId = component.getCurrentProfessionId();
        
        if (currentProfessionId == null) return;
        
        int exp = 0;
        String professionPath = currentProfessionId.getPath();
        
        if ("cook".equals(professionPath)) {
            // Повар получает опыт за употребление пищи
            String foodId = stack.getItem().toString();
            
            if (foodId.contains("cooked")) {
                exp = 10; // Больше опыта за приготовленную пищу
            } else if (stack.isFood()) {
                exp = 5; // Меньше за обычную еду
            }
        }
        
        if (exp > 0) {
            component.addExperience(exp);
        }
    }
}