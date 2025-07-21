package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public class FurnaceExpMixin {
    @Inject(method = "craftRecipe", at = @At("RETURN"))
    private void origins$giveCookExp(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        System.out.println("[DEBUG] FurnaceExpMixin.craftRecipe called");
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (stack != null && stack.isFood()) {
            ProfessionComponent comp = ProfessionComponent.KEY.get(serverPlayer);
            if (comp != null && "origins:cook".equals(String.valueOf(comp.getCurrentProfessionId()))) {
                comp.addExperience(5);
                serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +5 опыта за приготовление еды (повар) [FurnaceExpMixin]").formatted(net.minecraft.util.Formatting.YELLOW), false);
            }
        }
    }

    @Inject(method = "onCraft", at = @At("RETURN"))
    private void origins$giveCookExp(ItemStack stack, CallbackInfo ci) {
        System.out.println("[DEBUG] FurnaceExpMixin.onCraft called");
        if (stack != null && stack.isFood()) {
            // Здесь можно реализовать начисление опыта, если debug появится
        }
    }

    @Inject(method = "removeStack", at = @At("RETURN"))
    private void origins$giveCookExp(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
        System.out.println("[DEBUG] FurnaceExpMixin.removeStack called");
        ItemStack stack = cir.getReturnValue();
        if (stack != null && stack.isFood()) {
            // Здесь можно реализовать начисление опыта, если debug появится
        }
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void origins$giveCookExpOnTake(PlayerEntity player, ItemStack stack, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        System.out.println("[DEBUG] FurnaceExpMixin.onTake called");
        if (stack != null && stack.isFood()) {
            // Здесь можно реализовать начисление опыта, если debug появится
        }
    }
} 