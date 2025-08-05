package io.github.apace100.origins.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Представляет награду за выполнение квеста
 */
public class QuestReward {
    private final RewardType type;
    private final int tier;
    private final int experience;
    private final List<RewardItem> items;
    
    public QuestReward(RewardType type, int tier, int experience) {
        this.type = type;
        this.tier = tier;
        this.experience = experience;
        this.items = new ArrayList<>();
    }
    
    public QuestReward(RewardType type, int tier, int experience, List<RewardItem> items) {
        this.type = type;
        this.tier = tier;
        this.experience = experience;
        this.items = items != null ? items : new ArrayList<>();
    }
    
    // Геттеры
    public RewardType getType() { return type; }
    public int getTier() { return tier; }
    public int getExperience() { return experience; }
    public List<RewardItem> getItems() { return items; }
    
    /**
     * Выдает награду игроку
     */
    public void giveReward(ServerPlayerEntity player) {
        switch (type) {
            case SKILL_POINT_TOKEN:
                giveSkillPointToken(player);
                giveRewardItems(player); // Также выдаем дополнительные предметы
                break;
            case EXPERIENCE:
                giveExperience(player);
                giveRewardItems(player); // Также выдаем дополнительные предметы
                break;
            case ITEM:
                giveRewardItems(player);
                break;
        }
    }
    
    /**
     * Выдает дополнительные предметы из награды
     */
    private void giveRewardItems(ServerPlayerEntity player) {
        for (RewardItem rewardItem : items) {
            ItemStack itemStack = rewardItem.createItemStack();
            if (itemStack != null && !itemStack.isEmpty()) {
                // Пытаемся добавить в инвентарь
                if (!player.getInventory().insertStack(itemStack)) {
                    // Если инвентарь полный, дропаем рядом с игроком
                    player.dropItem(itemStack, false);
                }
            }
        }
    }
    
    /**
     * Выдает SkillPointToken игроку
     */
    private void giveSkillPointToken(ServerPlayerEntity player) {
        ItemStack token = createSkillPointToken();
        
        // Пытаемся добавить в инвентарь
        if (!player.getInventory().insertStack(token)) {
            // Если инвентарь полный, дропаем рядом с игроком
            player.dropItem(token, false);
        }
        
        // Отправляем сообщение игроку
        player.sendMessage(
            Text.translatable("quest.origins.reward.received", 
                Text.translatable("item.origins.skill_point_token_tier" + tier),
                experience),
            false
        );
    }
    
    /**
     * Выдает опыт напрямую
     */
    private void giveExperience(ServerPlayerEntity player) {
        // Интегрируем с системой профессий Origins
        try {
            io.github.apace100.origins.profession.ProfessionComponent component = 
                io.github.apace100.origins.profession.ProfessionComponent.KEY.get(player);
            
            if (component != null) {
                component.addExperience(experience);
                
                player.sendMessage(
                    Text.translatable("quest.origins.reward.experience", experience),
                    false
                );
            }
        } catch (Exception e) {
            // Fallback: выдаем SkillPointToken
            giveSkillPointToken(player);
        }
    }
    
    /**
     * Создает SkillPointToken соответствующего уровня
     */
    private ItemStack createSkillPointToken() {
        // Получаем соответствующий предмет из регистра
        switch (tier) {
            case 1:
                return new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER1);
            case 2:
                return new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER2);
            case 3:
                return new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER3);
            default:
                return new ItemStack(QuestRegistry.SKILL_POINT_TOKEN_TIER1);
        }
    }
    
    /**
     * Получает отображаемое описание награды
     */
    public Text getDisplayText() {
        switch (type) {
            case SKILL_POINT_TOKEN:
                return Text.translatable("quest.origins.reward.skill_point_token", 
                    Text.translatable("item.origins.skill_point_token_tier" + tier),
                    experience);
            case EXPERIENCE:
                return Text.translatable("quest.origins.reward.experience", experience);
            case ITEM:
                return Text.translatable("quest.origins.reward.item");
            default:
                return Text.literal("Неизвестная награда");
        }
    }
    
    /**
     * Создает награду из JSON объекта
     */
    public static QuestReward fromJson(JsonObject json) {
        RewardType type = RewardType.valueOf(json.get("type").getAsString().toUpperCase());
        int tier = json.has("tier") ? json.get("tier").getAsInt() : 1;
        int experience = json.has("experience") ? json.get("experience").getAsInt() : 
                        getDefaultExperience(tier);
        
        List<RewardItem> items = new ArrayList<>();
        if (json.has("items") && json.get("items").isJsonArray()) {
            JsonArray itemsArray = json.getAsJsonArray("items");
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject itemObj = itemsArray.get(i).getAsJsonObject();
                RewardItem rewardItem = RewardItem.fromJson(itemObj);
                if (rewardItem != null) {
                    items.add(rewardItem);
                }
            }
        }
        
        return new QuestReward(type, tier, experience, items);
    }
    
    /**
     * Получает стандартное количество опыта для уровня
     */
    private static int getDefaultExperience(int tier) {
        return switch (tier) {
            case 1 -> 500;
            case 2 -> 1000;
            case 3 -> 1500;
            default -> 500;
        };
    }
    
    /**
     * Типы наград
     */
    public enum RewardType {
        SKILL_POINT_TOKEN("skill_point_token"),
        EXPERIENCE("experience"),
        ITEM("item");
        
        private final String name;
        
        RewardType(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * Представляет предмет в награде
     */
    public static class RewardItem {
        private final String itemId;
        private final int amount;
        
        public RewardItem(String itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }
        
        public String getItemId() { return itemId; }
        public int getAmount() { return amount; }
        
        /**
         * Создает ItemStack из данного предмета
         */
        public ItemStack createItemStack() {
            try {
                Identifier id = new Identifier(itemId);
                var item = Registries.ITEM.get(id);
                if (item != Items.AIR) {
                    return new ItemStack(item, amount);
                }
            } catch (Exception e) {
                // Логируем ошибку, но не падаем
                System.err.println("Failed to create ItemStack for: " + itemId);
            }
            return ItemStack.EMPTY;
        }
        
        /**
         * Создает RewardItem из JSON объекта
         */
        public static RewardItem fromJson(JsonObject json) {
            try {
                String itemId = json.get("item").getAsString();
                int amount = json.get("amount").getAsInt();
                return new RewardItem(itemId, amount);
            } catch (Exception e) {
                System.err.println("Failed to parse RewardItem from JSON: " + json);
                return null;
            }
        }
    }
}