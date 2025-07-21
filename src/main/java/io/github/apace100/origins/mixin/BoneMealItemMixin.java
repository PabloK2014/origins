package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {
    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void origins$giveCookExp(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient) return;
        PlayerEntity player = context.getPlayer();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ProfessionComponent comp = ProfessionComponent.KEY.get(serverPlayer);
            if (comp != null && "origins:cook".equals(String.valueOf(comp.getCurrentProfessionId()))) {
                if (cir.getReturnValue() == net.minecraft.util.ActionResult.SUCCESS || cir.getReturnValue().isAccepted()) {
                    comp.addExperience(1);
                    serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +1 опыт за использование костной муки (повар)").formatted(net.minecraft.util.Formatting.YELLOW), false);
                }
            }
        }
    }
} 