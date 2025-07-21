package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void origins$giveCookExp(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getWorld().isClient) return;
        PlayerEntity player = context.getPlayer();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ProfessionComponent comp = ProfessionComponent.KEY.get(serverPlayer);
            if (comp != null && "origins:cook".equals(String.valueOf(comp.getCurrentProfessionId()))) {
                if (cir.getReturnValue() == net.minecraft.util.ActionResult.SUCCESS || cir.getReturnValue().isAccepted()) {
                    comp.addExperience(2);
                    serverPlayer.sendMessage(net.minecraft.text.Text.literal("[DEBUG] +2 опыта за посадку семян (повар)").formatted(net.minecraft.util.Formatting.YELLOW), false);
                }
            }
        }
    }
} 