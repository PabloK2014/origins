package io.github.apace100.origins.quest;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Представляет награду за выполнение квеста
 */
public class QuestReward {
    private final RewardType type;
    private final int tier;
    private final int experience;
    
    public QuestReward(RewardType type, int tier, int experience) {
        this.type = type;
        this.tier = tier;
        this.experience = experience;
    }
    
    // Геттеры
    public RewardType getType() { return type; }
    public int getTier() { return tier; }
    public int getExperience() { return experience; }
    
    /**
     * Выдает награду игроку
     */
    public void giveReward(ServerPlayerEntity player) {
        switch (type) {
            case SKILL_POINT_TOKEN:
                giveSkillPointToken(player);
                break;
            case EXPERIENCE:
                giveExperience(player);
                break;
            case ITEM:
                // Для будущего расширения
                break;
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
        
        return new QuestReward(type, tier, experience);
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
}