package io.github.apace100.origins.quest;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Обработчик событий для отслеживания прогресса квестов в стиле Bountiful
 */
public class BountifulQuestEventHandler {
    
    public static void registerEvents() {
        // Отслеживание добычи блоков
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity) {
                updateMiningProgress((ServerPlayerEntity) player, state.getBlock().asItem().getDefaultStack());
            }
        });
        
        // Отслеживание крафта (через использование предметов)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayerEntity) {
                ItemStack stack = player.getStackInHand(hand);
                updateCraftingProgress((ServerPlayerEntity) player, stack);
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
    }
    
    /**
     * Обновляет прогресс добычи блоков
     */
    private static void updateMiningProgress(ServerPlayerEntity player, ItemStack minedItem) {
        updateQuestProgress(player, BountifulQuestObjectiveType.MINE, minedItem.getItem().toString());
    }
    
    /**
     * Обновляет прогресс крафта
     */
    private static void updateCraftingProgress(ServerPlayerEntity player, ItemStack craftedItem) {
        updateQuestProgress(player, BountifulQuestObjectiveType.CRAFT, craftedItem.getItem().toString());
    }
    
    /**
     * Обновляет прогресс убийства мобов
     */
    public static void updateKillProgress(ServerPlayerEntity player, LivingEntity killedEntity) {
        String entityType = killedEntity.getType().toString();
        updateQuestProgress(player, BountifulQuestObjectiveType.KILL, entityType);
    }
    
    /**
     * Основной метод обновления прогресса квестов
     */
    private static void updateQuestProgress(ServerPlayerEntity player, BountifulQuestObjectiveType objectiveType, String target) {
        // Проходим по всем предметам в инвентаре игрока
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            
            // Проверяем, является ли предмет квестом Bountiful
            if (stack.getItem() instanceof BountifulQuestItem) {
                updateSingleQuestProgress(player, stack, objectiveType, target);
            }
        }
    }
    
    /**
     * Обновляет прогресс одного квеста
     */
    private static void updateSingleQuestProgress(ServerPlayerEntity player, ItemStack questStack, 
                                                 BountifulQuestObjectiveType objectiveType, String target) {
        BountifulQuestData questData = BountifulQuestData.get(questStack);
        boolean updated = false;
        
        // Проверяем все цели квеста
        for (BountifulQuestEntry objective : questData.getObjectives()) {
            if (objective.getObjectiveType() == objectiveType && 
                objective.getContent().equals(target)) {
                
                // Увеличиваем прогресс
                objective.setCurrent(objective.getCurrent() + 1);
                updated = true;
            }
        }
        
        if (updated) {
            // Сохраняем обновленные данные
            BountifulQuestData.set(questStack, questData);
            
            // Проверяем завершение квеста
            questData.checkForCompletionAndAlert(player, questStack);
        }
    }
    
    /**
     * Проверяет, может ли игрок взять квест (по профессии)
     */
    public static boolean canPlayerTakeQuest(PlayerEntity player, ItemStack questStack) {
        if (!(questStack.getItem() instanceof BountifulQuestItem)) {
            return true; // Старые квесты можно брать всем
        }
        
        BountifulQuestInfo questInfo = BountifulQuestInfo.get(questStack);
        String questProfession = questInfo.getProfession();
        
        // Если квест для любой профессии, можно брать всем
        if ("any".equals(questProfession)) {
            return true;
        }
        
        // Получаем профессию игрока
        try {
            io.github.apace100.origins.component.OriginComponent originComponent = 
                io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            
            if (originComponent != null) {
                // Упрощенная проверка профессии
                // В реальной реализации нужно будет улучшить это
                return true; // Пока разрешаем всем
            }
        } catch (Exception e) {
            // Если не удалось получить профессию, разрешаем взять квест
            return true;
        }
        
        return false;
    }
    
    /**
     * Пытается сдать квест
     */
    public static boolean tryCompleteQuest(PlayerEntity player, ItemStack questStack) {
        if (!(questStack.getItem() instanceof BountifulQuestItem)) {
            return false;
        }
        
        BountifulQuestData questData = BountifulQuestData.get(questStack);
        return questData.tryCashIn(player, questStack);
    }
}