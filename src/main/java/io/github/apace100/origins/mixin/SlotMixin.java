package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public class SlotMixin {
    @Inject(method = "onTakeItem", at = @At("RETURN"))
    private void origins$giveCookExp(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        Slot slot = (Slot) (Object) this;
        String invClass = slot.inventory != null ? slot.inventory.getClass().getName() : "null";
        int slotIdx = slot.getIndex();
        boolean isFood = stack != null && stack.isFood();
        System.out.println("[DEBUG] SlotMixin: onTakeItem called | inv=" + invClass + ", idx=" + slotIdx + ", isFood=" + isFood + ", stack=" + stack);
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (isFood) {
            if (slot.inventory != null && invClass.contains("Furnace") && slotIdx == 2) {
                System.out.println("[DEBUG] SlotMixin: опыт за еду из печи!");
                ProfessionComponent comp = ProfessionComponent.KEY.get(serverPlayer);
                if (comp != null && "origins:cook".equals(String.valueOf(comp.getCurrentProfessionId()))) {
                    comp.addExperience(stack.getCount());
                    serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +" + stack.getCount() + " опыта за приготовление еды (повар) [SlotMixin]").formatted(net.minecraft.util.Formatting.YELLOW), false);
                }
            }
        }
    }
} 