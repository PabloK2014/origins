package io.github.apace100.origins.skill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Комплексный тест для проверки всех скиллов в системе
 */
public class SkillTestSuite {

    private SkillTreeHandler skillTreeHandler;

    @BeforeEach
    void setUp() {
        // Инициализируем обработчик скиллов
        this.skillTreeHandler = new SkillTreeHandler();
    }

    /**
     * Тест для проверки всех классов и их деревьев скиллов
     */
    @Test
    void testAllClassSkillTrees() {
        // Проверяем, что все классы имеют деревья навыков
        assertNotNull(SkillTreeHandler.CLASS_SKILL_TREES);
        assertFalse(SkillTreeHandler.CLASS_SKILL_TREES.isEmpty());
        
        // Проверяем наличие деревьев для всех классов
        assertTrue(SkillTreeHandler.CLASS_SKILL_TREES.containsKey("origins:warrior"));
        assertTrue(SkillTreeHandler.CLASS_SKILL_TREES.containsKey("origins:courier"));
        assertTrue(SkillTreeHandler.CLASS_SKILL_TREES.containsKey("origins:miner"));
        assertTrue(SkillTreeHandler.CLASS_SKILL_TREES.containsKey("origins:brewer"));
        assertTrue(SkillTreeHandler.CLASS_SKILL_TREES.containsKey("origins:cook"));
        assertTrue(SkillTreeHandler.CLASS_SKILL_TREES.containsKey("origins:blacksmith"));
        
        System.out.println("Проверено " + SkillTreeHandler.CLASS_SKILL_TREES.size() + " деревьев скиллов");
    }

    /**
     * Тест для проверки скиллов воина
     */
    @Test
    void testWarriorSkills() {
        SkillTreeHandler.SkillTree warriorTree = SkillTreeHandler.CLASS_SKILL_TREES.get("origins:warrior");
        assertNotNull(warriorTree);
        
        // Получаем все скиллы воина
        Map<String, SkillTreeHandler.Skill> skills = getSkillsAsMap(warriorTree);
        assertFalse(skills.isEmpty());
        
        // Проверяем, что все скиллы из дерева присутствуют
        assertTrue(skills.containsKey("berserk_way"));
        assertTrue(skills.containsKey("bloody_wound"));
        assertTrue(skills.containsKey("mad_boost"));
        assertTrue(skills.containsKey("thirst_battle"));
        assertTrue(skills.containsKey("last_chance"));
        assertTrue(skills.containsKey("iron"));
        assertTrue(skills.containsKey("indestructibility"));
        assertTrue(skills.containsKey("fortress"));
        assertTrue(skills.containsKey("tadjic"));
        assertTrue(skills.containsKey("carry"));
        assertTrue(skills.containsKey("dagestan"));
        
        System.out.println("Воин имеет " + skills.size() + " скиллов");
    }

    /**
     * Тест для проверки скиллов курьера
     */
    @Test
    void testCourierSkills() {
        SkillTreeHandler.SkillTree courierTree = SkillTreeHandler.CLASS_SKILL_TREES.get("origins:courier");
        assertNotNull(courierTree);
        
        Map<String, SkillTreeHandler.Skill> skills = getSkillsAsMap(courierTree);
        assertFalse(skills.isEmpty());
        
        // Проверяем наличие скиллов курьера
        assertTrue(skills.containsKey("speed_basic"));
        assertTrue(skills.containsKey("hunger_reduction"));
        assertTrue(skills.containsKey("sprint_boost"));
        assertTrue(skills.containsKey("speed_surge"));
        assertTrue(skills.containsKey("inventory_slots_basic"));
        assertTrue(skills.containsKey("magnetic_pockets"));
        assertTrue(skills.containsKey("inventory_surge"));
        assertTrue(skills.containsKey("carry_capacity_basic"));
        assertTrue(skills.containsKey("shulker_carry"));
        assertTrue(skills.containsKey("carry_surge"));
        
        System.out.println("Курьер имеет " + skills.size() + " скиллов");
    }

    /**
     * Тест для проверки скиллов повара
     */
    @Test
    void testCookSkills() {
        SkillTreeHandler.SkillTree cookTree = SkillTreeHandler.CLASS_SKILL_TREES.get("origins:cook");
        assertNotNull(cookTree);
        
        Map<String, SkillTreeHandler.Skill> skills = getSkillsAsMap(cookTree);
        assertFalse(skills.isEmpty());
        
        // Проверяем наличие скиллов повара
        assertTrue(skills.containsKey("fresh_product"));
        assertTrue(skills.containsKey("fast_cooking"));
        assertTrue(skills.containsKey("hearty_meal"));
        assertTrue(skills.containsKey("chef_master"));
        assertTrue(skills.containsKey("smoke_screen"));
        assertTrue(skills.containsKey("flambe"));
        assertTrue(skills.containsKey("fire_immunity"));
        assertTrue(skills.containsKey("ready"));
        assertTrue(skills.containsKey("quick_snack"));
        assertTrue(skills.containsKey("feast_world"));
        assertTrue(skills.containsKey("banquet"));
        
        System.out.println("Повар имеет " + skills.size() + " скиллов");
    }

    /**
     * Тест для проверки скиллов пивовара
     */
    @Test
    void testBrewerSkills() {
        SkillTreeHandler.SkillTree brewerTree = SkillTreeHandler.CLASS_SKILL_TREES.get("origins:brewer");
        assertNotNull(brewerTree);
        
        Map<String, SkillTreeHandler.Skill> skills = getSkillsAsMap(brewerTree);
        assertFalse(skills.isEmpty());
        
        // Проверяем наличие скиллов пивовара
        assertTrue(skills.containsKey("brewing_efficiency"));
        assertTrue(skills.containsKey("potion_duration"));
        assertTrue(skills.containsKey("double_brew"));
        assertTrue(skills.containsKey("master_brewer"));
        assertTrue(skills.containsKey("alcohol_resistance"));
        assertTrue(skills.containsKey("drunk_strength"));
        assertTrue(skills.containsKey("bottle_throw"));
        assertTrue(skills.containsKey("berserker_drink"));
        assertTrue(skills.containsKey("group_buff"));
        assertTrue(skills.containsKey("healing_ale"));
        assertTrue(skills.containsKey("party_time"));
        
        System.out.println("Пивовар имеет " + skills.size() + " скиллов");
    }

    /**
     * Тест для проверки скиллов кузнеца
     */
    @Test
    void testBlacksmithSkills() {
        SkillTreeHandler.SkillTree blacksmithTree = SkillTreeHandler.CLASS_SKILL_TREES.get("origins:blacksmith");
        assertNotNull(blacksmithTree);
        
        Map<String, SkillTreeHandler.Skill> skills = getSkillsAsMap(blacksmithTree);
        assertFalse(skills.isEmpty());
        
        // Проверяем наличие скиллов кузнеца
        assertTrue(skills.containsKey("extra_durability"));
        assertTrue(skills.containsKey("resource_efficiency"));
        assertTrue(skills.containsKey("double_ingot"));
        assertTrue(skills.containsKey("auto_repair"));
        assertTrue(skills.containsKey("instant_repair"));
        assertTrue(skills.containsKey("fire_immunity"));
        assertTrue(skills.containsKey("hot_strike"));
        assertTrue(skills.containsKey("forge_master"));
        
        System.out.println("Кузнец имеет " + skills.size() + " скиллов");
    }

    /**
     * Тест для проверки скиллов шахтера
     */
    @Test
    void testMinerSkills() {
        SkillTreeHandler.SkillTree minerTree = SkillTreeHandler.CLASS_SKILL_TREES.get("origins:miner");
        assertNotNull(minerTree);
        
        Map<String, SkillTreeHandler.Skill> skills = getSkillsAsMap(minerTree);
        assertFalse(skills.isEmpty());
        
        // Проверяем наличие скиллов шахтера
        assertTrue(skills.containsKey("ore_double"));
        assertTrue(skills.containsKey("ore_highlight"));
        assertTrue(skills.containsKey("vein_miner"));
        assertTrue(skills.containsKey("ore_drop_chance"));
        assertTrue(skills.containsKey("deep_miner"));
        assertTrue(skills.containsKey("blast_resistance"));
        assertTrue(skills.containsKey("torch_range"));
        assertTrue(skills.containsKey("deep_miner_global"));
        
        System.out.println("Шахтер имеет " + skills.size() + " скиллов");
    }

    /**
     * Вспомогательный метод для преобразования дерева скиллов в мапу для удобного доступа
     */
    private Map<String, SkillTreeHandler.Skill> getSkillsAsMap(SkillTreeHandler.SkillTree tree) {
        Map<String, SkillTreeHandler.Skill> skillMap = new java.util.HashMap<>();
        
        for (java.util.List<SkillTreeHandler.Skill> branch : tree.getBranches()) {
            for (SkillTreeHandler.Skill skill : branch) {
                skillMap.put(skill.getId(), skill);
            }
        }
        
        return skillMap;
    }

    /**
     * Тест для проверки свойств скилла
     */
    @Test
    void testSkillProperties() {
        SkillTreeHandler.SkillTree warriorTree = SkillTreeHandler.CLASS_SKILL_TREES.get("origins:warrior");
        assertNotNull(warriorTree);
        
        Map<String, SkillTreeHandler.Skill> skills = getSkillsAsMap(warriorTree);
        SkillTreeHandler.Skill berserkWay = skills.get("berserk_way");
        
        assertNotNull(berserkWay);
        assertEquals("berserk_way", berserkWay.getId());
        assertEquals("Путь Берсерка", berserkWay.getName());
        assertEquals(SkillTreeHandler.SkillType.PASSIVE, berserkWay.getType());
        assertEquals(1, berserkWay.getRequiredLevel());
        assertEquals(3, berserkWay.getMaxLevel());
        assertNull(berserkWay.getParentId()); // null означает, что это начальный скилл
    }

    /**
     * Тест для выявления нереализованных скиллов
     */
    @Test
    void testForUnimplementedSkills() {
        System.out.println("\n=== АНАЛИЗ НЕРЕАЛИЗОВАННЫХ СКИЛЛОВ ===");
        
        // Скиллы, для которых есть обработчики
        String[] implementedWarriorSkills = {
            "berserk_way", "bloody_wound", "mad_boost", "thirst_battle", "last_chance",
            "iron", "indestructibility", "fortress", "tadjic", "carry", "dagestan"
        };
        
        String[] implementedCourierSkills = {"sprint_boost"};
        
        String[] implementedCookSkills = {
            "fresh_product", "fast_cooking", "hearty_meal", "chef_master", "smoke_screen",
            "flambe", "fire_immunity", "ready", "quick_snack", "feast_world", "banquet"
        };
        
        String[] implementedBrewerSkills = {"bottle_throw", "berserker_drink", "healing_ale", "party_time"};
        
        String[] implementedBlacksmithSkills = {"instant_repair", "hot_strike", "fire_immunity"};
        
        // Проверяем скиллы воина
        checkUnimplementedSkills("Воин", "origins:warrior", implementedWarriorSkills);
        checkUnimplementedSkills("Курьер", "origins:courier", implementedCourierSkills);
        checkUnimplementedSkills("Повар", "origins:cook", implementedCookSkills);
        checkUnimplementedSkills("Пивовар", "origins:brewer", implementedBrewerSkills);
        checkUnimplementedSkills("Кузнец", "origins:blacksmith", implementedBlacksmithSkills);
        
        System.out.println("=== АНАЛИЗ ЗАВЕРШЕН ===\n");
    }

    /**
     * Вспомогательный метод для проверки нереализованных скиллов
     */
    private void checkUnimplementedSkills(String className, String classKey, String[] implementedSkills) {
        SkillTreeHandler.SkillTree tree = SkillTreeHandler.CLASS_SKILL_TREES.get(classKey);
        if (tree == null) return;
        
        Map<String, SkillTreeHandler.Skill> allSkills = getSkillsAsMap(tree);
        java.util.List<String> unimplemented = new java.util.ArrayList<>();
        
        for (String skillId : allSkills.keySet()) {
            boolean found = false;
            for (String implemented : implementedSkills) {
                if (skillId.equals(implemented)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                unimplemented.add(skillId + " (\"" + allSkills.get(skillId).getName() + "\")");
            }
        }
        
        if (!unimplemented.isEmpty()) {
            System.out.println(className + " - Не реализовано " + unimplemented.size() + " скиллов:");
            for (String skill : unimplemented) {
                System.out.println("  - " + skill);
            }
        } else {
            System.out.println(className + " - Все скиллы реализованы");
        }
    }
}