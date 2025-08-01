package io.github.apace100.origins.quest;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Типы наград квестов, аналогичные IBountyReward из Bountiful
 */
public enum BountifulQuestRewardType {
    ITEM("item") {
        @Override
        public boolean giveReward(BountifulQuestEntry entry, PlayerEntity player) {
            Item item = getItemFromContent(entry.getContent());
            int amount = entry.getAmount();
            
            // Разбиваем на стаки по максимальному размеру стака
            while (amount > 0) {
                int stackSize = Math.min(amount, item.getMaxCount());
                ItemStack stack = new ItemStack(item, stackSize);
                
                // Пытаемся дать предмет игроку, иначе бросаем на землю
                if (!player.giveItemStack(stack)) {
                    ItemEntity itemEntity = new ItemEntity(
                        player.getWorld(), 
                        player.getX(), 
                        player.getY(), 
                        player.getZ(), 
                        stack
                    );
                    itemEntity.setPickupDelay(0);
                    player.getWorld().spawnEntity(itemEntity);
                }
                
                amount -= stackSize;
            }
            
            return true;
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            Item item = getItemFromContent(entry.getContent());
            QuestProgress progress = new QuestProgress(0, entry.getAmount());
            
            return progress.getAmountText()
                .append(item.getName().copy().formatted(entry.getRarity().getColor()));
        }
    },
    
    EXPERIENCE("experience") {
        @Override
        public boolean giveReward(BountifulQuestEntry entry, PlayerEntity player) {
            player.addExperience(entry.getAmount());
            return true;
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            return Text.literal(entry.getAmount() + " ")
                .append(Text.translatable("origins.quest.reward.experience")
                    .formatted(entry.getRarity().getColor()));
        }
    },
    
    SKILL_POINTS("skill_points") {
        @Override
        public boolean giveReward(BountifulQuestEntry entry, PlayerEntity player) {
            // TODO: Интеграция с системой навыков
            // Пока что просто даем опыт
            player.addExperience(entry.getAmount() * 10);
            return true;
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            return Text.literal(entry.getAmount() + " ")
                .append(Text.translatable("origins.quest.reward.skill_points")
                    .formatted(entry.getRarity().getColor()));
        }
    },
    
    MONEY("money") {
        @Override
        public boolean giveReward(BountifulQuestEntry entry, PlayerEntity player) {
            // TODO: Интеграция с экономической системой
            // Пока что даем изумруды
            Item emerald = net.minecraft.item.Items.EMERALD;
            int amount = entry.getAmount();
            
            while (amount > 0) {
                int stackSize = Math.min(amount, emerald.getMaxCount());
                ItemStack stack = new ItemStack(emerald, stackSize);
                
                if (!player.giveItemStack(stack)) {
                    ItemEntity itemEntity = new ItemEntity(
                        player.getWorld(), 
                        player.getX(), 
                        player.getY(), 
                        player.getZ(), 
                        stack
                    );
                    itemEntity.setPickupDelay(0);
                    player.getWorld().spawnEntity(itemEntity);
                }
                
                amount -= stackSize;
            }
            
            return true;
        }
        
        @Override
        public MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player) {
            return Text.literal(entry.getAmount() + " ")
                .append(Text.translatable("origins.quest.reward.money")
                    .formatted(entry.getRarity().getColor()));
        }
    };
    
    private final String name;
    
    BountifulQuestRewardType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Выдает награду игроку
     */
    public abstract boolean giveReward(BountifulQuestEntry entry, PlayerEntity player);
    
    /**
     * Получает текстовое описание награды
     */
    public abstract MutableText getTextSummary(BountifulQuestEntry entry, PlayerEntity player);
    
    /**
     * Получает тип награды по имени
     */
    public static BountifulQuestRewardType fromName(String name) {
        for (BountifulQuestRewardType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return ITEM; // По умолчанию
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