package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для начисления опыта пивовару за варку зелий
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewerExperienceMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private static void giveExperienceForBrewing(World world, BlockPos pos, net.minecraft.block.BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        
        // Проверяем, завершилась ли варка зелья
        // Используем рефлексию для доступа к приватному полю brewTime
        try {
            java.lang.reflect.Field brewTimeField = BrewingStandBlockEntity.class.getDeclaredField("brewTime");
            brewTimeField.setAccessible(true);
            int brewTime = brewTimeField.getInt(blockEntity);
            
            if (brewTime == 1) { // Варка завершается, когда время = 1
                // Находим ближайшего игрока-пивовара
                PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
                
                if (nearestPlayer instanceof ServerPlayerEntity serverPlayer && isPlayerBrewer(serverPlayer)) {
                    // Проверяем, сколько зелий в варочной стойке
                    int potionsBrewedCount = 0;
                    
                    for (int i = 0; i < 3; i++) {
                        ItemStack currentStack = blockEntity.getStack(i);
                        
                        // Проверяем, что это зелье
                        if (isBrewedPotion(currentStack)) {
                            potionsBrewedCount++;
                        }
                    }
                    
                    // Начисляем опыт за каждое сваренное зелье
                    if (potionsBrewedCount > 0) {
                        ProfessionComponent component = ProfessionComponent.KEY.get(serverPlayer);
                        component.addExperience(potionsBrewedCount * 8); // 8 опыта за каждое зелье
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки рефлексии
        }
    }
    
    /**
     * Проверяет, является ли игрок пивоваром
     */
    private static boolean isPlayerBrewer(ServerPlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(
                io.github.apace100.origins.Origins.identifier("origin")));
                
            return origin != null && origin.getIdentifier().toString().equals("origins:brewer");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Проверяет, является ли предмет зельем
     */
    private static boolean isBrewedPotion(ItemStack stack) {
        return stack.getItem() == Items.POTION || 
               stack.getItem() == Items.SPLASH_POTION || 
               stack.getItem() == Items.LINGERING_POTION;
    }
}