package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceScreenHandler.class)
public abstract class FurnaceScreenHandlerMixin {
    @Inject(method = "onTakeOutput", at = @At("TAIL"))
    private void origins$giveCookExp(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] FurnaceScreenHandlerMixin вызван").formatted(net.minecraft.util.Formatting.GRAY), false);
        // Проверяем, что предмет - еда
        if (!stack.isFood()) return;
        ProfessionComponent comp = ProfessionComponent.KEY.get(serverPlayer);
        if (comp != null && "origins:cook".equals(String.valueOf(comp.getCurrentProfessionId()))) {
            comp.addExperience(5);
            serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +5 опыта за приготовление еды (повар)").formatted(net.minecraft.util.Formatting.YELLOW), false);
        }
    }
} 