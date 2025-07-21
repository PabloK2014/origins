package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class PlayerInventoryFoodExpMixin {
    @Inject(method = "addStack", at = @At("RETURN"))
    private void origins$giveCookExp(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack != null && stack.isFood()) {
            PlayerInventory inv = (PlayerInventory) (Object) this;
            PlayerEntity player = inv.player;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ProfessionComponent comp = ProfessionComponent.KEY.get(serverPlayer);
                if (comp != null && "origins:cook".equals(String.valueOf(comp.getCurrentProfessionId()))) {
                    comp.addExperience(stack.getCount());
                    serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +"
                        + stack.getCount() + " опыта за еду в инвентаре (повар)").formatted(net.minecraft.util.Formatting.YELLOW), false);
                }
            }
        }
    }
} 