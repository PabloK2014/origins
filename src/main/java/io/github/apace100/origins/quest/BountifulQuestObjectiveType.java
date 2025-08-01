package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Типы целей квестов, аналогичные IBountyObjective из Bountiful
 */
public enum BountifulQuestObjectiveType {
    COLLECT("collect") {
        @Override
        public QuestProgress getProgress(BountifulQuestEntry entry, PlayerEntity player) {
            int currentAmount = getCurrentItemCount(entry, player);
            return new QuestProgress(currentAmount, entry.getAmount());
        }
        
        @Override
        public boolean tryFinishObjective(BountifulQuestEntry entry, PlayerEntity player) {
            int needed = entry.getAmount();
            int currentAmount = getCurrentItemCount(entry, player);
            
            if (currentAmount >= needed) {
                // Удаляем предметы из инвентаря
                removeItemsFromInventory(entry, player, needed);
                return true;
            }
            return false;
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            QuestProgress progress = getProgress(entry, player);
            Item item = getItemFromContent(entry.getContent());
            MutableText itemName = item.getName().copy();
            
            return itemName.formatted(progress.getColor())
                .append(progress.getProgressText().formatted(Formatting.WHITE));
        }
        
        private int getCurrentItemCount(BountifulQuestEntry entry, PlayerEntity player) {
            Item targetItem = getItemFromContent(entry.getContent());
            int count = 0;
            
            for (ItemStack stack : player.getInventory().main) {
                if (stack.getItem() == targetItem) {
                    count += stack.getCount();
                }
            }
            
            return count;
        }
        
        private void removeItemsFromInventory(BountifulQuestEntry entry, PlayerEntity player, int amount) {
            Item targetItem = getItemFromContent(entry.getContent());
            int remaining = amount;
            
            for (ItemStack stack : player.getInventory().main) {
                if (stack.getItem() == targetItem && remaining > 0) {
                    int toRemove = Math.min(stack.getCount(), remaining);
                    stack.decrement(toRemove);
                    remaining -= toRemove;
                    
                    if (remaining <= 0) break;
                }
            }
        }
    },
    
    CRAFT("craft") {
        @Override
        public QuestProgress getProgress(BountifulQuestEntry entry, PlayerEntity player) {
            return new QuestProgress(entry.getCurrent(), entry.getAmount());
        }
        
        @Override
        public boolean tryFinishObjective(BountifulQuestEntry entry, PlayerEntity player) {
            return entry.getCurrent() >= entry.getAmount();
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            QuestProgress progress = getProgress(entry, player);
            Item item = getItemFromContent(entry.getContent());
            MutableText itemName = item.getName().copy();
            
            return Text.translatable("origins.quest.craft")
                .append(" ")
                .append(itemName.formatted(progress.getColor()))
                .append(progress.getProgressText().formatted(Formatting.WHITE));
        }
    },
    
    KILL("kill") {
        @Override
        public QuestProgress getProgress(BountifulQuestEntry entry, PlayerEntity player) {
            return new QuestProgress(entry.getCurrent(), entry.getAmount());
        }
        
        @Override
        public boolean tryFinishObjective(BountifulQuestEntry entry, PlayerEntity player) {
            return entry.getCurrent() >= entry.getAmount();
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            QuestProgress progress = getProgress(entry, player);
            
            return Text.translatable("origins.quest.kill")
                .append(" ")
                .append(Text.translatable("entity." + entry.getContent().replace(":", "."))
                    .formatted(progress.getColor()))
                .append(progress.getProgressText().formatted(Formatting.WHITE));
        }
    },
    
    MINE("mine") {
        @Override
        public QuestProgress getProgress(BountifulQuestEntry entry, PlayerEntity player) {
            return new QuestProgress(entry.getCurrent(), entry.getAmount());
        }
        
        @Override
        public boolean tryFinishObjective(BountifulQuestEntry entry, PlayerEntity player) {
            return entry.getCurrent() >= entry.getAmount();
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            QuestProgress progress = getProgress(entry, player);
            Item item = getItemFromContent(entry.getContent());
            MutableText itemName = item.getName().copy();
            
            return Text.translatable("origins.quest.mine")
                .append(" ")
                .append(itemName.formatted(progress.getColor()))
                .append(progress.getProgressText().formatted(Formatting.WHITE));
        }
    };
    
    private final String name;
    
    BountifulQuestObjectiveType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Получает прогресс выполнения цели
     */
    public abstract QuestProgress getProgress(BountifulQuestEntry entry, PlayerEntity player);
    
    /**
     * Пытается завершить цель квеста
     */
    public abstract boolean tryFinishObjective(BountifulQuestEntry entry, PlayerEntity player);
    
    /**
     * Получает текстовое описание цели
     */
    public abstract MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player);
    
    /**
     * Получает тип цели по имени
     */
    public static BountifulQuestObjectiveType fromName(String name) {
        for (BountifulQuestObjectiveType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return COLLECT; // По умолчанию
    }
    
    /**
     * Получает предмет из строки контента
     */
    protected static Item getItemFromContent(String content) {
        try {
            Identifier id = new Identifier(content);
            return Registries.ITEM.get(id);
        } catch (Exception e) {
            return net.minecraft.item.Items.DIRT; // Fallback
        }
    }
}