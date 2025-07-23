package io.github.apace100.origins.mixin;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.skill.BlacksmithSkillHandler;
import io.github.apace100.origins.skill.PlayerSkillComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для обработки навыков кузнеца при крафте
 */
@Mixin(CraftingScreenHandler.class)
public class BlacksmithCraftingSkillMixin {
    
    @Inject(method = "onContentChanged", at = @At("HEAD"))
    private void origins$handleBlacksmithCrafting(CraftingInventory inventory, CallbackInfo ci) {
        try {
            // В Fabric 1.20.1 нет прямого доступа к игроку из ScreenHandler
            // Этот миксин временно отключен - навыки кузнеца будут работать через другие миксины
            return;
        } catch (Exception e) {
            Origins.LOGGER.error("Ошибка в BlacksmithCraftingSkillMixin: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет, является ли предмет инструментом или броней
     */
    private boolean isToolOrArmor(ItemStack stack) {
        String itemName = stack.getItem().toString().toLowerCase();
        return itemName.contains("sword") || itemName.contains("pickaxe") || 
               itemName.contains("axe") || itemName.contains("shovel") || 
               itemName.contains("hoe") || itemName.contains("helmet") || 
               itemName.contains("chestplate") || itemName.contains("leggings") || 
               itemName.contains("boots");
    }
}