package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.profession.ProfessionComponent;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для начисления опыта повару за сбор урожая и приготовление пищи
 */
@Mixin(Block.class)
public class CookExperienceMixin {

    @Inject(method = "onBreak", at = @At("TAIL"))
    private void giveExperienceForHarvesting(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Проверяем, что игрок имеет происхождение повара
        if (!isPlayerCook(serverPlayer)) {
            return;
        }
        
        // Проверяем, что это урожай и он созрел
        if (state.getBlock() instanceof CropBlock crop && crop.isMature(state)) {
            // Начисляем опыт за сбор созревшего урожая
            ProfessionComponent component = ProfessionComponent.KEY.get(serverPlayer);
            component.addExperience(3); // 3 опыта за сбор созревшего урожая
            serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +3 опыта за сбор урожая (повар)").formatted(net.minecraft.util.Formatting.YELLOW), false);
        }
    }
    
    /**
     * Проверяет, является ли игрок поваром
     */
    private boolean isPlayerCook(ServerPlayerEntity player) {
        try {
            OriginComponent originComponent = ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                
            return origin != null && origin.getIdentifier().toString().equals("origins:cook");
        } catch (Exception e) {
            return false;
        }
    }
}

/**
 * Mixin для начисления опыта повару за приготовление пищи
 */
@Mixin(AbstractCookingRecipe.class)
class CookingExperienceMixin {

    @Inject(method = "assemble", at = @At("RETURN"))
    private void giveExperienceForCooking(Inventory inventory, net.minecraft.registry.DynamicRegistryManager registryManager, CallbackInfoReturnable<ItemStack> cir) {
        if (inventory.getClass().getSimpleName().contains("FurnaceInventory")) {
            try {
                // Получаем игрока, который использует печь
                Object furnace = inventory;
                java.lang.reflect.Field worldField = furnace.getClass().getDeclaredField("world");
                worldField.setAccessible(true);
                World world = (World) worldField.get(furnace);
                
                if (world.isClient) return;
                
                // Находим ближайшего игрока-повара
                BlockPos pos = ((net.minecraft.block.entity.BlockEntity) furnace).getPos();
                PlayerEntity nearestPlayer = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8.0, false);
                
                if (nearestPlayer instanceof ServerPlayerEntity serverPlayer && isPlayerCook(serverPlayer)) {
                    // Проверяем, что результат - еда
                    ItemStack result = cir.getReturnValue();
                    if (result.isFood()) {
                        // Начисляем опыт за приготовление пищи
                        ProfessionComponent component = ProfessionComponent.KEY.get(serverPlayer);
                        component.addExperience(5); // 5 опыта за приготовление пищи
                        serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +5 опыта за готовку еды (повар)").formatted(net.minecraft.util.Formatting.YELLOW), false);
                    }
                }
            } catch (Exception e) {
                // Игнорируем ошибки
                io.github.apace100.origins.Origins.LOGGER.error("[DEBUG] Ошибка начисления опыта повару за готовку: " + e.getMessage());
            }
        }
    }
    
    /**
     * Проверяет, является ли игрок поваром
     */
    private boolean isPlayerCook(ServerPlayerEntity player) {
        try {
            OriginComponent originComponent = ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
                
            return origin != null && origin.getIdentifier().toString().equals("origins:cook");
        } catch (Exception e) {
            return false;
        }
    }
}