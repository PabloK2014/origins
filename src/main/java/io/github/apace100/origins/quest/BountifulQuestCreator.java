package io.github.apace100.origins.quest;

import io.github.apace100.origins.component.PlayerOriginComponent;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Создатель квестов, аналогичный BountyCreator из Bountiful
 */
public class BountifulQuestCreator {
    private final ServerWorld world;
    private final BlockPos pos;
    private final String profession;
    private final int playerLevel;
    private final long startTime;
    
    private BountifulQuestData questData;
    private BountifulQuestInfo questInfo;
    
    public BountifulQuestCreator(ServerWorld world, BlockPos pos, String profession, int playerLevel, long startTime) {
        this.world = world;
        this.pos = pos;
        this.profession = profession;
        this.playerLevel = playerLevel;
        this.startTime = startTime;
    }
    
    /**
     * Создает квест-предмет
     */
    public ItemStack createQuestItem() {
        questData = new BountifulQuestData();
        questInfo = new BountifulQuestInfo();
        
        // Генерируем награды
        List<BountifulQuestEntry> rewards = generateRewards();
        if (rewards.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        questData.getRewards().addAll(rewards);
        
        // Определяем редкость квеста по максимальной редкости наград
        Quest.QuestRarity maxRarity = rewards.stream()
            .map(BountifulQuestEntry::getRarity)
            .max((r1, r2) -> Integer.compare(r1.getLevel(), r2.getLevel()))
            .orElse(Quest.QuestRarity.COMMON);
        
        questInfo.setRarity(maxRarity);
        questInfo.setProfession(profession);
        
        // Генерируем цели
        List<BountifulQuestEntry> objectives = generateObjectives(rewards);
        questData.getObjectives().addAll(objectives);
        
        // Устанавливаем время
        questInfo = new BountifulQuestInfo(
            maxRarity,
            startTime,
            calculateTimeLimit(maxRarity, objectives.size()),
            profession
        );
        
        return BountifulQuestItem.createQuestItem(questData, questInfo);
    }
    
    /**
     * Генерирует награды для квеста
     */
    private List<BountifulQuestEntry> generateRewards() {
        List<BountifulQuestEntry> rewards = new ArrayList<>();
        Random random = new Random();
        
        // Количество наград зависит от уровня игрока
        int numRewards = Math.min(3, 1 + playerLevel / 5);
        
        for (int i = 0; i < numRewards; i++) {
            BountifulQuestEntry reward = generateRewardForProfession(profession, playerLevel, random);
            if (reward != null) {
                rewards.add(reward);
            }
        }
        
        return rewards;
    }
    
    /**
     * Генерирует награду для конкретной профессии
     */
    private BountifulQuestEntry generateRewardForProfession(String profession, int level, Random random) {
        Quest.QuestRarity rarity = Quest.QuestRarity.fromLevel(Math.min(4, 1 + level / 10));
        
        BountifulQuestEntry reward = new BountifulQuestEntry();
        reward.setRarity(rarity);
        reward.setRewardType(BountifulQuestRewardType.ITEM);
        
        // Выбираем награду в зависимости от профессии
        switch (profession) {
            case "blacksmith":
                String[] smithItems = {"minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:coal"};
                reward.setContent(smithItems[random.nextInt(smithItems.length)]);
                reward.setAmount(random.nextInt(5) + 1);
                break;
                
            case "cook":
                String[] cookItems = {"minecraft:bread", "minecraft:cooked_beef", "minecraft:golden_apple", "minecraft:cake"};
                reward.setContent(cookItems[random.nextInt(cookItems.length)]);
                reward.setAmount(random.nextInt(3) + 1);
                break;
                
            case "miner":
                String[] minerItems = {"minecraft:coal", "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:diamond_ore"};
                reward.setContent(minerItems[random.nextInt(minerItems.length)]);
                reward.setAmount(random.nextInt(8) + 1);
                break;
                
            case "warrior":
                String[] warriorItems = {"minecraft:iron_sword", "minecraft:shield", "minecraft:arrow", "minecraft:bow"};
                reward.setContent(warriorItems[random.nextInt(warriorItems.length)]);
                reward.setAmount(1);
                break;
                
            case "courier":
                String[] courierItems = {"minecraft:leather_boots", "minecraft:map", "minecraft:compass", "minecraft:ender_pearl"};
                reward.setContent(courierItems[random.nextInt(courierItems.length)]);
                reward.setAmount(random.nextInt(2) + 1);
                break;
                
            case "brewer":
                String[] brewerItems = {"minecraft:potion", "minecraft:brewing_stand", "minecraft:blaze_powder", "minecraft:nether_wart"};
                reward.setContent(brewerItems[random.nextInt(brewerItems.length)]);
                reward.setAmount(random.nextInt(3) + 1);
                break;
                
            default:
                // Универсальные награды
                reward.setRewardType(BountifulQuestRewardType.EXPERIENCE);
                reward.setAmount(50 + random.nextInt(100));
                break;
        }
        
        reward.setId("reward_" + profession + "_" + random.nextInt(1000));
        return reward;
    }
    
    /**
     * Генерирует цели для квеста
     */
    private List<BountifulQuestEntry> generateObjectives(List<BountifulQuestEntry> rewards) {
        List<BountifulQuestEntry> objectives = new ArrayList<>();
        Random random = new Random();
        
        // Обычно 1-2 цели
        int numObjectives = random.nextInt(2) + 1;
        
        for (int i = 0; i < numObjectives; i++) {
            BountifulQuestEntry objective = generateObjectiveForProfession(profession, playerLevel, random);
            if (objective != null) {
                objectives.add(objective);
            }
        }
        
        return objectives;
    }
    
    /**
     * Генерирует цель для конкретной профессии
     */
    private BountifulQuestEntry generateObjectiveForProfession(String profession, int level, Random random) {
        Quest.QuestRarity rarity = Quest.QuestRarity.fromLevel(Math.min(4, 1 + level / 10));
        
        BountifulQuestEntry objective = new BountifulQuestEntry();
        objective.setRarity(rarity);
        
        // Выбираем цель в зависимости от профессии
        switch (profession) {
            case "blacksmith":
                objective.setObjectiveType(random.nextBoolean() ? 
                    BountifulQuestObjectiveType.COLLECT : BountifulQuestObjectiveType.CRAFT);
                String[] smithTargets = {"minecraft:iron_ore", "minecraft:coal", "minecraft:iron_ingot", "minecraft:iron_sword"};
                objective.setContent(smithTargets[random.nextInt(smithTargets.length)]);
                objective.setAmount(random.nextInt(10) + 5);
                break;
                
            case "cook":
                objective.setObjectiveType(random.nextBoolean() ? 
                    BountifulQuestObjectiveType.COLLECT : BountifulQuestObjectiveType.CRAFT);
                String[] cookTargets = {"minecraft:wheat", "minecraft:raw_beef", "minecraft:bread", "minecraft:cooked_beef"};
                objective.setContent(cookTargets[random.nextInt(cookTargets.length)]);
                objective.setAmount(random.nextInt(8) + 3);
                break;
                
            case "miner":
                objective.setObjectiveType(random.nextBoolean() ? 
                    BountifulQuestObjectiveType.MINE : BountifulQuestObjectiveType.COLLECT);
                String[] minerTargets = {"minecraft:stone", "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:cobblestone"};
                objective.setContent(minerTargets[random.nextInt(minerTargets.length)]);
                objective.setAmount(random.nextInt(20) + 10);
                break;
                
            case "warrior":
                objective.setObjectiveType(BountifulQuestObjectiveType.KILL);
                String[] warriorTargets = {"minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper"};
                objective.setContent(warriorTargets[random.nextInt(warriorTargets.length)]);
                objective.setAmount(random.nextInt(5) + 3);
                break;
                
            case "courier":
                objective.setObjectiveType(BountifulQuestObjectiveType.COLLECT);
                String[] courierTargets = {"minecraft:leather", "minecraft:string", "minecraft:feather", "minecraft:paper"};
                objective.setContent(courierTargets[random.nextInt(courierTargets.length)]);
                objective.setAmount(random.nextInt(15) + 5);
                break;
                
            case "brewer":
                objective.setObjectiveType(random.nextBoolean() ? 
                    BountifulQuestObjectiveType.COLLECT : BountifulQuestObjectiveType.CRAFT);
                String[] brewerTargets = {"minecraft:nether_wart", "minecraft:blaze_powder", "minecraft:potion", "minecraft:glass_bottle"};
                objective.setContent(brewerTargets[random.nextInt(brewerTargets.length)]);
                objective.setAmount(random.nextInt(6) + 2);
                break;
                
            default:
                // Универсальные цели
                objective.setObjectiveType(BountifulQuestObjectiveType.COLLECT);
                objective.setContent("minecraft:dirt");
                objective.setAmount(random.nextInt(10) + 5);
                break;
        }
        
        objective.setId("objective_" + profession + "_" + random.nextInt(1000));
        return objective;
    }
    
    /**
     * Вычисляет лимит времени для квеста
     */
    private long calculateTimeLimit(Quest.QuestRarity rarity, int numObjectives) {
        // Базовое время: 15 минут (18000 тиков)
        long baseTime = 18000L;
        
        // Добавляем время в зависимости от редкости
        long rarityBonus = rarity.getLevel() * 6000L; // +5 минут за уровень редкости
        
        // Добавляем время за количество целей
        long objectiveBonus = numObjectives * 3600L; // +3 минуты за цель
        
        return baseTime + rarityBonus + objectiveBonus;
    }
    
    /**
     * Создает квест для игрока
     */
    public static ItemStack createQuestForPlayer(ServerWorld world, BlockPos pos, PlayerEntity player) {
        // Получаем профессию игрока
        String profession = "any"; // По умолчанию
        
        try {
            io.github.apace100.origins.component.OriginComponent originComponent = 
                ModComponents.ORIGIN.get(player);
            
            if (originComponent != null) {
                // Упрощенная версия определения профессии
                // В реальной реализации нужно будет улучшить это
                profession = "any";
            }
        } catch (Exception e) {
            // Если не удалось получить профессию, используем "any"
            profession = "any";
        }
        
        // Определяем уровень игрока (пока что используем уровень опыта)
        int playerLevel = player.experienceLevel;
        
        BountifulQuestCreator creator = new BountifulQuestCreator(
            world, pos, profession, playerLevel, world.getTime()
        );
        
        return creator.createQuestItem();
    }
}