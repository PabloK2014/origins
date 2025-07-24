package io.github.apace100.origins.quest;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;

/**
 * Генератор случайных квестов для доски объявлений
 */
public class QuestGenerator {
    
    // Квесты для каждой профессии по уровням
    private static final Map<String, Map<Integer, QuestTemplate[]>> QUEST_TEMPLATES = new HashMap<>();
    
    static {
        initializeQuestTemplates();
    }
    
    private static void initializeQuestTemplates() {
        // Квесты для воина
        Map<Integer, QuestTemplate[]> warriorQuests = new HashMap<>();
        warriorQuests.put(1, new QuestTemplate[]{
            new QuestTemplate(Items.BONE, 10),
            new QuestTemplate(Items.ROTTEN_FLESH, 15),
            new QuestTemplate(Items.STRING, 8)
        });
        warriorQuests.put(2, new QuestTemplate[]{
            new QuestTemplate(Items.GUNPOWDER, 5),
            new QuestTemplate(Items.SPIDER_EYE, 3),
            new QuestTemplate(Items.ARROW, 32)
        });
        warriorQuests.put(3, new QuestTemplate[]{
            new QuestTemplate(Items.WITHER_SKELETON_SKULL, 1),
            new QuestTemplate(Items.DIAMOND_SWORD, 1),
            new QuestTemplate(Items.ENCHANTED_BOOK, 2)
        });
        QUEST_TEMPLATES.put("warrior", warriorQuests);
        
        // Квесты для шахтера
        Map<Integer, QuestTemplate[]> minerQuests = new HashMap<>();
        minerQuests.put(1, new QuestTemplate[]{
            new QuestTemplate(Items.COAL, 32),
            new QuestTemplate(Items.COBBLESTONE, 64),
            new QuestTemplate(Items.STONE, 32)
        });
        minerQuests.put(2, new QuestTemplate[]{
            new QuestTemplate(Items.IRON_INGOT, 16),
            new QuestTemplate(Items.GOLD_INGOT, 8),
            new QuestTemplate(Items.REDSTONE, 32)
        });
        minerQuests.put(3, new QuestTemplate[]{
            new QuestTemplate(Items.DIAMOND, 8),
            new QuestTemplate(Items.EMERALD, 4),
            new QuestTemplate(Items.ANCIENT_DEBRIS, 2)
        });
        QUEST_TEMPLATES.put("miner", minerQuests);
        
        // Квесты для кузнеца
        Map<Integer, QuestTemplate[]> blacksmithQuests = new HashMap<>();
        blacksmithQuests.put(1, new QuestTemplate[]{
            new QuestTemplate(Items.IRON_SWORD, 1),
            new QuestTemplate(Items.IRON_PICKAXE, 1),
            new QuestTemplate(Items.IRON_INGOT, 8)
        });
        blacksmithQuests.put(2, new QuestTemplate[]{
            new QuestTemplate(Items.DIAMOND_PICKAXE, 1),
            new QuestTemplate(Items.DIAMOND_SWORD, 1),
            new QuestTemplate(Items.ANVIL, 1)
        });
        blacksmithQuests.put(3, new QuestTemplate[]{
            new QuestTemplate(Items.NETHERITE_INGOT, 1),
            new QuestTemplate(Items.NETHERITE_SWORD, 1),
            new QuestTemplate(Items.NETHERITE_PICKAXE, 1)
        });
        QUEST_TEMPLATES.put("blacksmith", blacksmithQuests);
        
        // Квесты для курьера
        Map<Integer, QuestTemplate[]> courierQuests = new HashMap<>();
        courierQuests.put(1, new QuestTemplate[]{
            new QuestTemplate(Items.LEATHER_BOOTS, 1),
            new QuestTemplate(Items.BREAD, 16),
            new QuestTemplate(Items.PAPER, 8)
        });
        courierQuests.put(2, new QuestTemplate[]{
            new QuestTemplate(Items.MAP, 3),
            new QuestTemplate(Items.ENDER_PEARL, 4),
            new QuestTemplate(Items.SADDLE, 1)
        });
        courierQuests.put(3, new QuestTemplate[]{
            new QuestTemplate(Items.COMPASS, 2),
            new QuestTemplate(Items.ELYTRA, 1),
            new QuestTemplate(Items.FIREWORK_ROCKET, 16)
        });
        QUEST_TEMPLATES.put("courier", courierQuests);
        
        // Квесты для пивовара
        Map<Integer, QuestTemplate[]> brewerQuests = new HashMap<>();
        brewerQuests.put(1, new QuestTemplate[]{
            new QuestTemplate(Items.NETHER_WART, 16),
            new QuestTemplate(Items.GLASS_BOTTLE, 8),
            new QuestTemplate(Items.SUGAR, 16)
        });
        brewerQuests.put(2, new QuestTemplate[]{
            new QuestTemplate(Items.BLAZE_POWDER, 8),
            new QuestTemplate(Items.SPIDER_EYE, 4),
            new QuestTemplate(Items.MAGMA_CREAM, 4)
        });
        brewerQuests.put(3, new QuestTemplate[]{
            new QuestTemplate(Items.GHAST_TEAR, 2),
            new QuestTemplate(Items.DRAGON_BREATH, 1),
            new QuestTemplate(Items.PHANTOM_MEMBRANE, 4)
        });
        QUEST_TEMPLATES.put("brewer", brewerQuests);
        
        // Квесты для повара
        Map<Integer, QuestTemplate[]> cookQuests = new HashMap<>();
        cookQuests.put(1, new QuestTemplate[]{
            new QuestTemplate(Items.COOKED_BEEF, 16),
            new QuestTemplate(Items.BREAD, 32),
            new QuestTemplate(Items.COOKED_CHICKEN, 12)
        });
        cookQuests.put(2, new QuestTemplate[]{
            new QuestTemplate(Items.CAKE, 3),
            new QuestTemplate(Items.GOLDEN_CARROT, 8),
            new QuestTemplate(Items.MUSHROOM_STEW, 6)
        });
        cookQuests.put(3, new QuestTemplate[]{
            new QuestTemplate(Items.GOLDEN_APPLE, 4),
            new QuestTemplate(Items.ENCHANTED_GOLDEN_APPLE, 1),
            new QuestTemplate(Items.SUSPICIOUS_STEW, 8)
        });
        QUEST_TEMPLATES.put("cook", cookQuests);
    }
    
    /**
     * Генерирует случайный квест
     */
    public static BountyQuest generateRandomQuest() {
        Random random = Random.create();
        
        // Выбираем случайную профессию
        String[] professions = {"warrior", "miner", "blacksmith", "courier", "brewer", "cook"};
        String profession = professions[random.nextInt(professions.length)];
        
        // Выбираем случайный уровень (1-3)
        int level = 1 + random.nextInt(3);
        
        // Получаем шаблоны квестов для этой профессии и уровня
        Map<Integer, QuestTemplate[]> professionQuests = QUEST_TEMPLATES.get(profession);
        if (professionQuests == null) {
            return null;
        }
        
        QuestTemplate[] templates = professionQuests.get(level);
        if (templates == null || templates.length == 0) {
            return null;
        }
        
        // Выбираем случайный шаблон
        QuestTemplate template = templates[random.nextInt(templates.length)];
        
        // Создаем квест на основе шаблона
        return new BountyQuest(profession, level, template.item, template.amount);
    }
    
    /**
     * Генерирует квест для конкретной профессии и уровня
     */
    public static BountyQuest generateQuestForProfession(String profession, int level) {
        Random random = Random.create();
        
        Map<Integer, QuestTemplate[]> professionQuests = QUEST_TEMPLATES.get(profession);
        if (professionQuests == null) {
            return null;
        }
        
        QuestTemplate[] templates = professionQuests.get(level);
        if (templates == null || templates.length == 0) {
            return null;
        }
        
        QuestTemplate template = templates[random.nextInt(templates.length)];
        return new BountyQuest(profession, level, template.item, template.amount);
    }
    
    /**
     * Шаблон квеста
     */
    private static class QuestTemplate {
        final Item item;
        final int amount;
        
        QuestTemplate(Item item, int amount) {
            this.item = item;
            this.amount = amount;
        }
    }
}