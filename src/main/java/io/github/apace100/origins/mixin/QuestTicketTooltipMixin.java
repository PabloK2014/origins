package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.QuestTicketItem;
import io.github.apace100.origins.quest.Quest;
import io.github.apace100.origins.quest.QuestItem;
import io.github.apace100.origins.quest.QuestTicketState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin для принудительного обновления тултипов билетов квестов в реальном времени
 */
@Mixin(QuestTicketItem.class)
public class QuestTicketTooltipMixin {
    
    @Inject(method = "appendTooltip", at = @At("HEAD"))
    private void forceTimeUpdate(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        // Принудительно обновляем время каждый раз при отображении тултипа
        if (world != null && world.isClient) {
            updateClientSideTime(stack);
        }
    }
    
    /**
     * Обновляет время на стороне клиента для корректного отображения в тултипе
     */
    private static void updateClientSideTime(ItemStack stack) {
        if (!QuestTicketItem.isQuestTicket(stack)) {
            return;
        }
        
        Quest quest = QuestItem.getQuestFromStack(stack);
        if (quest == null || quest.getTimeLimit() <= 0) {
            return;
        }
        
        // Проверяем, что билет находится в инвентаре игрока и тултип отображается в правильном контексте
        if (!io.github.apace100.origins.util.InventoryHelper.isStackInClientPlayerInventory(stack) ||
            !io.github.apace100.origins.util.InventoryHelper.isTooltipInPlayerInventoryContext()) {
            return; // Не инициализируем время для билетов не в инвентаре игрока или в GUI доски объявлений
        }
        
        QuestTicketState state = QuestTicketItem.getTicketState(stack);
        long currentTime = System.currentTimeMillis();
        net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        
        // Если билет доступен, но время не инициализировано, инициализируем его
        if (state == QuestTicketState.AVAILABLE) {
            if (!nbt.contains("accept_time") || nbt.getLong("accept_time") == 0) {
                nbt.putLong("accept_time", currentTime);
                nbt.putString("quest_state", QuestTicketState.ACCEPTED.getName());
                state = QuestTicketState.ACCEPTED; // Обновляем локальную переменную
            }
        }
        
        // Проверяем активные квесты
        if (state.isActive()) {
            long acceptTime = nbt.getLong("accept_time");
            if (acceptTime > 0) {
                long elapsedSeconds = (currentTime - acceptTime) / 1000;
                long totalLimitSeconds = quest.getTimeLimit() * 60;
                
                if (elapsedSeconds >= totalLimitSeconds) {
                    // Время истекло, помечаем квест как проваленный на клиенте
                    nbt.putString("quest_state", QuestTicketState.FAILED.getName());
                    nbt.putBoolean("completion_ready", false);
                }
            }
        }
        
        // Обновляем метку времени для принудительного обновления
        nbt.putLong("client_tooltip_time", currentTime);
    }
}