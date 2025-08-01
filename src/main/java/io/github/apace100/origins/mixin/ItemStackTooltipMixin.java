package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.QuestTicketItem;
import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestItem;
import io.github.apace100.origins.quest.QuestTicketState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mixin для принудительного обновления тултипов ItemStack
 * Обеспечивает обновление времени в билетах квестов в реальном времени
 */
@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {
    
    @Inject(method = "getTooltip", at = @At("HEAD"))
    private void updateQuestTicketTime(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        
        // Обновляем время для билетов квестов перед отображением тултипа
        if (QuestTicketItem.isQuestTicket(stack) && player != null && player.getWorld().isClient) {
            updateQuestTicketTimeData(stack);
        }
    }
    
    /**
     * Обновляет временные данные билета квеста для корректного отображения в тултипе
     */
    private static void updateQuestTicketTimeData(ItemStack stack) {
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest == null || quest.getTimeLimit() <= 0) {
            return;
        }
        
        // Проверяем, что билет находится в инвентаре игрока и тултип отображается в правильном контексте
        if (!io.github.apace100.origins.util.InventoryHelper.isStackInClientPlayerInventory(stack) ||
            !io.github.apace100.origins.util.InventoryHelper.isTooltipInPlayerInventoryContext()) {
            return; // Не инициализируем время для билетов не в инвентаре игрока или в GUI доски объявлений
        }
        
        long currentTime = System.currentTimeMillis();
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        QuestTicketState state = QuestTicketItem.getTicketState(stack);
        
        // Если билет доступен, но время не инициализировано, инициализируем его немедленно
        if (state == QuestTicketState.AVAILABLE) {
            if (!nbt.contains("accept_time") || nbt.getLong("accept_time") == 0) {
                nbt.putLong("accept_time", currentTime);
                nbt.putString("quest_state", QuestTicketState.ACCEPTED.getName());
            }
        }
        
        // Принудительно обновляем метку времени для пересчета в тултипе
        nbt.putLong("tooltip_render_time", currentTime);
        
        // Также сохраняем текущее время для использования в расчетах
        nbt.putLong("client_time_cache", currentTime);
    }
}