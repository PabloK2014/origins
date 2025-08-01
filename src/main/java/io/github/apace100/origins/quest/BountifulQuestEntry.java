package io.github.apace100.origins.quest;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Запись квеста (цель или награда), аналогичная BountyDataEntry из Bountiful
 */
public class BountifulQuestEntry {
    private String id;
    private Identifier logicId;
    private String content;
    private int amount;
    private String name;
    private Quest.QuestRarity rarity = Quest.QuestRarity.COMMON;
    private int current = 0; // Текущий прогресс
    
    // Типы для целей и наград
    private BountifulQuestObjectiveType objectiveType;
    private BountifulQuestRewardType rewardType;
    
    public BountifulQuestEntry() {}
    
    public BountifulQuestEntry(String id, String content, int amount, Quest.QuestRarity rarity) {
        this.id = id;
        this.content = content;
        this.amount = amount;
        this.rarity = rarity;
    }
    
    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Quest.QuestRarity getRarity() { return rarity; }
    public void setRarity(Quest.QuestRarity rarity) { this.rarity = rarity; }
    
    public int getCurrent() { return current; }
    public void setCurrent(int current) { this.current = current; }
    
    public BountifulQuestObjectiveType getObjectiveType() { return objectiveType; }
    public void setObjectiveType(BountifulQuestObjectiveType objectiveType) { this.objectiveType = objectiveType; }
    
    public BountifulQuestRewardType getRewardType() { return rewardType; }
    public void setRewardType(BountifulQuestRewardType rewardType) { this.rewardType = rewardType; }
    
    /**
     * Получает текстовое описание для отображения
     */
    public MutableText getTextSummary(PlayerEntity player, boolean isObjective) {
        if (isObjective && objectiveType != null) {
            return objectiveType.getTextSummary(this, player);
        } else if (!isObjective && rewardType != null) {
            return rewardType.getTextSummary(this, player);
        }
        
        // Fallback
        String displayName = name != null ? name : content;
        MutableText text = Text.literal(displayName);
        
        if (isObjective) {
            QuestProgress progress = new QuestProgress(current, amount);
            text = text.formatted(progress.isComplete() ? Formatting.GREEN : Formatting.RED);
            text.append(Text.literal(" (" + current + "/" + amount + ")").formatted(Formatting.WHITE));
        } else {
            text = Text.literal(amount + "x ").append(text.formatted(rarity.getColor()));
        }
        
        return text;
    }
    
    /**
     * Создает запись из NBT
     */
    public static BountifulQuestEntry fromNbt(NbtCompound nbt) {
        BountifulQuestEntry entry = new BountifulQuestEntry();
        
        entry.id = nbt.getString("id");
        entry.content = nbt.getString("content");
        entry.amount = nbt.getInt("amount");
        entry.name = nbt.contains("name") ? nbt.getString("name") : null;
        entry.rarity = Quest.QuestRarity.fromName(nbt.getString("rarity"));
        entry.current = nbt.getInt("current");
        
        // Загружаем типы
        if (nbt.contains("objectiveType")) {
            String objectiveTypeName = nbt.getString("objectiveType");
            entry.objectiveType = BountifulQuestObjectiveType.fromName(objectiveTypeName);
        }
        
        if (nbt.contains("rewardType")) {
            String rewardTypeName = nbt.getString("rewardType");
            entry.rewardType = BountifulQuestRewardType.fromName(rewardTypeName);
        }
        
        return entry;
    }
    
    /**
     * Сохраняет запись в NBT
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        
        nbt.putString("id", id != null ? id : "");
        nbt.putString("content", content != null ? content : "");
        nbt.putInt("amount", amount);
        if (name != null) {
            nbt.putString("name", name);
        }
        nbt.putString("rarity", rarity.getName());
        nbt.putInt("current", current);
        
        if (objectiveType != null) {
            nbt.putString("objectiveType", objectiveType.getName());
        }
        
        if (rewardType != null) {
            nbt.putString("rewardType", rewardType.getName());
        }
        
        return nbt;
    }
}