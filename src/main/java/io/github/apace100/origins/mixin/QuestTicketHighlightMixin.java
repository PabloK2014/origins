package io.github.apace100.origins.mixin;

import io.github.apace100.origins.quest.BountyBoardScreen;
import io.github.apace100.origins.quest.QuestTicketItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для подсветки билетов квестов в инвентаре
 */
@Mixin(HandledScreen.class)
public class QuestTicketHighlightMixin {
    
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void highlightQuestTicket(DrawContext context, Slot slot, CallbackInfo ci) {
        if (slot == null) {
            return;
        }
        
        ItemStack stack = slot.getStack();
        if (stack.isEmpty() || !QuestTicketItem.isQuestTicket(stack)) {
            return;
        }
        
        // Проверяем, должен ли билет быть подсвечен
        if (!BountyBoardScreen.shouldHighlightTicket(stack)) {
            return;
        }
        
        // Получаем интенсивность подсветки
        float intensity = BountyBoardScreen.getHighlightIntensity(stack);
        
        // Отрисовываем подсветку
        int x = slot.x;
        int y = slot.y;
        int color = (int)(255 * intensity) << 24 | 0xFFD700; // Золотой цвет с альфа-каналом
        
        // Рамка подсветки
        context.fill(x - 1, y - 1, x + 17, y, color); // Верх
        context.fill(x - 1, y + 16, x + 17, y + 17, color); // Низ
        context.fill(x - 1, y, x, y + 16, color); // Лево
        context.fill(x + 16, y, x + 17, y + 16, color); // Право
        
        // Легкая подсветка фона
        int bgColor = (int)(64 * intensity) << 24 | 0xFFD700;
        context.fill(x, y, x + 16, y + 16, bgColor);
    }
}