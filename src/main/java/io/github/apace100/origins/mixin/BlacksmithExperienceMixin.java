package io.github.apace100.origins.mixin;

import io.github.apace100.origins.profession.ProfessionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для начисления опыта кузнецу за крафт инструментов и брони
 */
@Mixin(CraftingScreenHandler.class)
public class BlacksmithExperienceMixin {

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void giveExperienceForCrafting(CallbackInfo ci) {
        CraftingScreenHandler handler = (CraftingScreenHandler) (Object) this;
        
        // Получаем игрока через поиск PlayerInventory в слотах
        PlayerEntity player = null;
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory playerInventory) {
                player = playerInventory.player;
                break;
            }
        }
        
        if (player == null || player.getWorld().isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        // Проверяем, что игрок имеет происхождение кузнеца
        if (!isPlayerBlacksmith(serverPlayer)) {
            return;
        }
        
        // Проверяем слот результата (индекс 0)
        Slot resultSlot = handler.getSlot(0);
        ItemStack resultStack = resultSlot.getStack();
        
        if (!resultStack.isEmpty() && isCraftableItem(resultStack)) {
            // Начисляем опыт за крафт инструмента или брони
            int expAmount = calculateCraftingExperience(resultStack);
            if (expAmount > 0) {
                ProfessionComponent component = ProfessionComponent.KEY.get(serverPlayer);
                component.addExperience(expAmount);
            }
        }
    }
    
    /**
     * Проверяет, является ли игрок кузнецом
     */
    private boolean isPlayerBlacksmith(ServerPlayerEntity player) {
        try {
            var originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            var origin = originComponent.getOrigin(io.github.apace100.origins.origin.OriginLayers.getLayer(
                io.github.apace100.origins.Origins.identifier("origin")));
                
            return origin != null && origin.getIdentifier().toString().equals("origins:blacksmith");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Проверяет, является ли предмет инструментом или броней
     */
    private boolean isCraftableItem(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof ToolItem || 
               item instanceof ArmorItem || 
               item instanceof SwordItem ||
               item instanceof BowItem ||
               item instanceof CrossbowItem ||
               item instanceof TridentItem ||
               item instanceof ShieldItem;
    }
    
    /**
     * Рассчитывает количество опыта за крафт предмета
     */
    private int calculateCraftingExperience(ItemStack stack) {
        Item item = stack.getItem();
        
        // Броня дает больше опыта
        if (item instanceof ArmorItem) {
            ArmorItem armorItem = (ArmorItem) item;
            switch (armorItem.getSlotType()) {
                case HEAD:
                case CHEST:
                    return 10; // Шлем и нагрудник
                case LEGS:
                    return 8; // Поножи
                case FEET:
                    return 6; // Ботинки
            }
        }
        
        // Инструменты и оружие
        if (item instanceof ToolItem || item instanceof SwordItem) {
            // Алмазные и незеритовые предметы дают больше опыта
            if (item.toString().contains("diamond") || item.toString().contains("netherite")) {
                return 15;
            }
            // Железные предметы
            if (item.toString().contains("iron")) {
                return 10;
            }
            // Каменные и золотые предметы
            return 5;
        }
        
        // Луки, арбалеты и щиты
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof ShieldItem) {
            return 8;
        }
        
        return 0; // Другие предметы не дают опыта
    }
}