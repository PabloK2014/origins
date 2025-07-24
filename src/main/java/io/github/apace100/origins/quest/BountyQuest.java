package io.github.apace100.origins.quest;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BountyQuest {
    private final String profession; // Класс, для которого предназначено задание
    private final int level; // Уровень задания (1-3)
    private final Item requiredItem; // Предмет, который нужно принести
    private final int requiredAmount; // Количество предметов
    private final int rewardExp; // Количество опыта за выполнение

    public BountyQuest(String profession, int level, Item requiredItem, int requiredAmount) {
        this.profession = profession;
        this.level = level;
        this.requiredItem = requiredItem;
        this.requiredAmount = requiredAmount;
        // Устанавливаем награду в зависимости от уровня задания
        this.rewardExp = switch (level) {
            case 1 -> 500;
            case 2 -> 1000;
            case 3 -> 1500;
            default -> 500;
        };
    }

    public String getProfession() {
        return profession;
    }

    public int getLevel() {
        return level;
    }

    public Item getRequiredItem() {
        return requiredItem;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public int getRewardExp() {
        return rewardExp;
    }

    public boolean canComplete(ItemStack stack) {
        return stack.getItem() == requiredItem && stack.getCount() >= requiredAmount;
    }

    public void writeToNbt(NbtCompound nbt) {
        nbt.putString("Profession", profession);
        nbt.putInt("Level", level);
        nbt.putString("RequiredItem", Registries.ITEM.getId(requiredItem).toString());
        nbt.putInt("RequiredAmount", requiredAmount);
        nbt.putInt("RewardExp", rewardExp);
    }

    public static BountyQuest fromNbt(NbtCompound nbt) {
        String profession = nbt.getString("Profession");
        int level = nbt.getInt("Level");
        Item requiredItem = Registries.ITEM.get(new Identifier(nbt.getString("RequiredItem")));
        int requiredAmount = nbt.getInt("RequiredAmount");
        
        return new BountyQuest(profession, level, requiredItem, requiredAmount);
    }
} 