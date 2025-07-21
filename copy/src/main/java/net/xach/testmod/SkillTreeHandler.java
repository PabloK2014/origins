package net.xach.testmod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTreeHandler {

    public static final Map<String, SkillTree> CLASS_SKILL_TREES = new HashMap<>();

    static {
        initializeSkillTrees();
    }

    public static void initializeSkillTrees() {
        // Класс Воин
        SkillTree warTree = new SkillTree();

        // Ветка "Берсерк"
        List<Skill> berserkBranch = Arrays.asList(
                new Skill("berserk_way", "Путь Берсерка", "Увеличивает урон при низком здоровье", SkillType.PASSIVE, 1, 3, null),
                new Skill("bloody_wound", "Кровавая Рана", "Шанс нанести кровотечение", SkillType.PASSIVE, 5, 3, "berserk_way"),
                new Skill("mad_boost", "Безумный Рывок", "Рывок с уроном по области", SkillType.ACTIVE, 10, 1, "bloody_wound"),
                new Skill("thirst_battle", "Жажда Битвы", "Восстановление здоровья при убийстве", SkillType.PASSIVE, 15, 1, "mad_boost"),
                new Skill("last_chance", "Последний Шанс", "Неуязвимость при смертельном ударе", SkillType.PASSIVE, 20, 1, "thirst_battle")
        );

        // Ветка "Защитник"
        List<Skill> defenderBranch = Arrays.asList(
                new Skill("iron", "Железная Стена", "Уменьшает входящий урон", SkillType.PASSIVE, 1, 3, null),
                new Skill("indestructibility", "Несокрушимость", "Активное сопротивление урону", SkillType.ACTIVE, 5, 1, "iron"),
                new Skill("fortress", "Крепость", "Защита при стоянии на месте", SkillType.PASSIVE, 10, 1, "indestructibility")
        );

        // Ветка "Таджик"
        List<Skill> tadjicBranch = Arrays.asList(
                new Skill("tadjic", "Монобровь", "Увеличивает регенерацию здоровья", SkillType.PASSIVE, 1, 5, null),
                new Skill("carry", "Армянская сила", "Увеличивает урон", SkillType.PASSIVE, 5, 5, "tadjic"),
                new Skill("dagestan", "Дагестанская братва", "Призывает союзников", SkillType.ACTIVE, 15, 1, "carry")
        );

        warTree.addBranch(berserkBranch);
        warTree.addBranch(defenderBranch);
        warTree.addBranch(tadjicBranch);
        CLASS_SKILL_TREES.put("war", warTree);

        // Класс Курьер Yandex.Go
        SkillTree yandexTree = new SkillTree();

// Ветка "Скорость"
        List<Skill> speedBranch = Arrays.asList(
                new Skill("speed_basic", "Базовая скорость", "Увеличивает скорость передвижения на 5% за уровень", SkillType.PASSIVE, 3, 1, null),
                new Skill("hunger_reduction", "Снижение голода", "Снижает расход голода на 10% за уровень", SkillType.PASSIVE, 3, 2, "speed_basic"),
                new Skill("sprint_boost", "Рывок", "Активирует временное ускорение на 10 секунд", SkillType.ACTIVE, 1, 3, "hunger_reduction"),
                new Skill("speed_surge", "Всплеск скорости", "Мощное ускорение на 30 секунд с восстановлением голода", SkillType.GLOBAL, 1, 5, "sprint_boost")
        );

// Ветка "Инвентарь"
        List<Skill> inventoryBranch = Arrays.asList(
                new Skill("inventory_slots_basic", "Базовые слоты", "Добавляет 1 слот инвентаря за уровень", SkillType.PASSIVE, 3, 1, null),
                new Skill("crafting_efficiency", "Стаки", "Увеличивает количество стаков в инвентаре (на 16 за уровень)", SkillType.PASSIVE, 4, 10, "inventory_slots_basic"),
                new Skill("inventory_surge", "Улыбка Курьера", "Скидка в 15% на все товары", SkillType.GLOBAL, 1, 4, "crafting_efficiency")
        );

// Ветка "Переноска"
        List<Skill> carryBranch = Arrays.asList(
                new Skill("carry_capacity_basic", "Ловушка", "На земле можно установить невидимый «триггер», который оглушает врагов", SkillType.PASSIVE, 3, 1, null),
                new Skill("shulker_carry", "Карта в голове", "Видит расположение всех деревень и торговцев", SkillType.PASSIVE, 1, 2, "carry_capacity_basic"),
                new Skill("carry_surge", "Граната с перцем", "Открывает крафт перцового баллона", SkillType.GLOBAL, 1, 4, "shulker_carry")
        );

        yandexTree.addBranch(speedBranch);
        yandexTree.addBranch(inventoryBranch);
        yandexTree.addBranch(carryBranch);
        CLASS_SKILL_TREES.put("yandex.go", yandexTree);

        // Класс Шахтёр
        SkillTree minerTree = new SkillTree();

        // Ветка "Добыча"
        List<Skill> miningBranch = Arrays.asList(
                new Skill("dig_area", "Широкое копание", "Ломает блоки в области", SkillType.PASSIVE, 1, 3, null),
                new Skill("ore_double", "Удвоение руды", "Шанс получить двойную руду", SkillType.PASSIVE, 5, 5, "dig_area"),
                new Skill("ore_highlight", "Подсветка руды", "Подсвечивает руду через стены", SkillType.ACTIVE, 10, 1, "ore_double"),
                new Skill("vein_miner", "Жилокопатель", "Добывает всю рудную жилу", SkillType.ACTIVE, 15, 1, "ore_highlight")
        );

        // Ветка "Исследование"
        List<Skill> explorationBranch = Arrays.asList(
                new Skill("night_vision", "Ночное зрение", "Активирует ночное зрение", SkillType.ACTIVE, 1, 1, null),
                new Skill("ore_drop_chance", "Случайная находка", "Шанс найти редкие предметы", SkillType.PASSIVE, 5, 3, "night_vision"),
                new Skill("deep_miner", "Глубинный шахтёр", "Бонус к добыче на глубине", SkillType.PASSIVE, 10, 3, "ore_drop_chance")
        );

        // Ветка "Выживание"
        List<Skill> survivalBranch = Arrays.asList(
                new Skill("blast_resistance", "Устойчивость к взрывам", "Уменьшает урон от взрывов на 15% за уровень", SkillType.PASSIVE, 3, 1, null),
                new Skill("torch_range", "Свет факела", "Увеличивает радиус освещения факелов на 2 блока за уровень", SkillType.PASSIVE, 3, 2, "blast_resistance"),
                new Skill("deep_miner", "Глубинный шахтёр", "Увеличивает добычу ресурсов в глубоких шахтах на 25%", SkillType.GLOBAL, 1, 5, "torch_range")
        );

        minerTree.addBranch(miningBranch);
        minerTree.addBranch(explorationBranch);
        minerTree.addBranch(survivalBranch);
        CLASS_SKILL_TREES.put("miner", minerTree);

        // Класс Пивовар
        SkillTree pivoTree = new SkillTree();

        // Ветка "Мастер ферментации"
        List<Skill> fermentationBranch = Arrays.asList(
                new Skill("brewing_efficiency", "Эффективность варки", "Ускоряет варку зелий на 20% за уровень", SkillType.PASSIVE, 1, 5, null),
                new Skill("potion_duration", "Длительность зелий", "Увеличивает время действия зелий на 25% за уровень", SkillType.PASSIVE, 5, 3, "brewing_efficiency"),
                new Skill("double_brew", "Двойная варка", "15% шанс получить дополнительное зелье", SkillType.PASSIVE, 10, 3, "potion_duration"),
                new Skill("master_brewer", "Мастер-пивовар", "Создаёт уникальные алкогольные напитки", SkillType.ACTIVE, 15, 1, "double_brew")
        );

        // Ветка "Пьяный мастер"
        List<Skill> drunkMasterBranch = Arrays.asList(
                new Skill("alcohol_resistance", "Устойчивость к алкоголю", "Сопротивление негативным эффектам", SkillType.PASSIVE, 1, 3, null),
                new Skill("drunk_strength", "Пьяная сила", "Урон увеличивается при низком здоровье", SkillType.PASSIVE, 5, 5, "alcohol_resistance"),
                new Skill("bottle_throw", "Метание бутылок", "Бросает взрывные бутылки", SkillType.ACTIVE, 10, 1, "drunk_strength"),
                new Skill("berserker_drink", "Напиток берсерка", "Временная неуязвимость и ярость", SkillType.ACTIVE, 20, 1, "bottle_throw")
        );

        // Ветка "Вечеринка в таверне"
        List<Skill> tavernPartyBranch = Arrays.asList(
                new Skill("group_buff", "Групповой бафф", "Даёт эффекты ближайшим союзникам", SkillType.PASSIVE, 1, 3, null),
                new Skill("healing_ale", "Лечебный эль", "Восстанавливает здоровье союзникам", SkillType.ACTIVE, 8, 1, "group_buff"),
                new Skill("party_time", "Время вечеринки", "Массовые баффы для всей команды", SkillType.ACTIVE, 15, 1, "healing_ale")
        );

        pivoTree.addBranch(fermentationBranch);
        pivoTree.addBranch(drunkMasterBranch);
        pivoTree.addBranch(tavernPartyBranch);
        CLASS_SKILL_TREES.put("pivo", pivoTree);

        // Класс Повар
        SkillTree cookTree = new SkillTree();

        // Ветка "Гурман"
        List<Skill> gourmetBranch = Arrays.asList(
                new Skill("fresh_product", "Свежий продукт", "Еда восстанавливает на 2% сытости больше за уровень", SkillType.PASSIVE, 1, 5, null),
                new Skill("fast_cooking", "Быстрое приготовление", "Время готовки в печи уменьшено на 10% за уровень", SkillType.PASSIVE, 5, 3, "fresh_product"),
                new Skill("hearty_meal", "Сытный обед", "Эффект сытости длится в 2 раза дольше", SkillType.PASSIVE, 10, 1, "fast_cooking"),
                new Skill("chef_master", "Шеф-повар", "Приготовленная еда даёт случайные позитивные эффекты", SkillType.PASSIVE, 15, 1, "hearty_meal")
        );

        // Ветка "Огненная кухня"
        List<Skill> fieryKitchenBranch = Arrays.asList(
                new Skill("smoke_screen", "Дымовая завеса", "При ударе ниже 30% HP даёт невидимость на 3 сек", SkillType.ACTIVE, 1, 1, null),
                new Skill("flambe", "Фламбе", "При убийстве подожжённого врага происходит взрыв", SkillType.PASSIVE, 8, 3, "smoke_screen"),
                new Skill("fire_immunity", "Огнестойкость", "Иммунитет к огню и лаве", SkillType.PASSIVE, 15, 1, "flambe")
        );

        // Ветка "Ресторанный критик"
        List<Skill> criticBranch = Arrays.asList(
                new Skill("ready", "Готово!", "Приготовление еды даёт бафф 'Вдохновение' (+10% урона)", SkillType.PASSIVE, 1, 3, null),
                new Skill("quick_snack", "Быстрый перекус", "Можно есть на бегу без замедления", SkillType.PASSIVE, 5, 1, "ready"),
                new Skill("feast_world", "Пир на весь мир", "Разделение еды с союзниками лечит их на +2 сердца", SkillType.PASSIVE, 10, 1, "quick_snack"),
                new Skill("banquet", "Банкет", "Все союзники получают регенерацию и сопротивление", SkillType.ACTIVE, 20, 1, "feast_world")
        );

        cookTree.addBranch(gourmetBranch);
        cookTree.addBranch(fieryKitchenBranch);
        cookTree.addBranch(criticBranch);
        CLASS_SKILL_TREES.put("cook", cookTree);

        // Класс Кузнец
        SkillTree smithTree = new SkillTree();

        // Ветка "Крафт"
        List<Skill> craftingBranch = Arrays.asList(
                new Skill("extra_durability", "Повышенная прочность", "2% за уровень шанс создать предмет с +10% прочности", SkillType.PASSIVE, 5, 1, null),
                new Skill("resource_efficiency", "Ресурсная экономия", "2% за уровень шанс не потратить ресурсы при крафте", SkillType.PASSIVE, 5, 2, "extra_durability"),
                new Skill("double_ingot", "Удвоение слитков", "5% за уровень шанс получить x2 слитка при плавке", SkillType.PASSIVE, 5, 3, "resource_efficiency")
        );

        // Ветка "Ремонт"
        List<Skill> repairBranch = Arrays.asList(
                new Skill("auto_repair", "Авторемонт", "Каждые 30 сек чинит случайный предмет в инвентаре на 5% прочности", SkillType.PASSIVE, 3, 2, null),
                new Skill("instant_repair", "Мгновенный ремонт", "Чинит все предметы в инвентаре на 50% прочности (перезарядка 300 сек)", SkillType.ACTIVE, 1, 4, "auto_repair")
        );

        // Ветка "Огненное мастерство"
        List<Skill> fireMasteryBranch = Arrays.asList(
                new Skill("fire_immunity", "Огненный иммунитет", "Полный иммунитет к урону от огня и лавы", SkillType.PASSIVE, 1, 2, null),
                new Skill("hot_strike", "Раскалённый удар", "Следующая атака поджигает цель на 5 сек (перезарядка 30 сек)", SkillType.ACTIVE, 1, 3, "fire_immunity"),
                new Skill("forge_master", "Мастер горна", "Ускоряет плавку в печах на 50% и удваивает шанс успеха чар", SkillType.GLOBAL, 1, 5, "hot_strike")
        );

        smithTree.addBranch(craftingBranch);
        smithTree.addBranch(repairBranch);
        smithTree.addBranch(fireMasteryBranch);
        CLASS_SKILL_TREES.put("smith", smithTree);
    }

    public static SkillTree getSkillTree(String className) {
        return CLASS_SKILL_TREES.get(className);
    }

    public enum SkillType {
        ACTIVE,
        PASSIVE,
        GLOBAL
    }

    public static class Skill {
        private final String id;
        private final String name;
        private final String description;
        private final SkillType type;
        private final int requiredLevel;
        private final int maxLevel;
        private final String parentId;

        public Skill(String id, String name, String description, SkillType type, int requiredLevel, int maxLevel, String parentId) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.requiredLevel = requiredLevel;
            this.maxLevel = maxLevel;
            this.parentId = parentId;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public SkillType getType() { return type; }
        public int getRequiredLevel() { return requiredLevel; }
        public int getMaxLevel() { return maxLevel; }
        public String getParentId() { return parentId; }
    }

    public static class SkillTree {
        private final List<List<Skill>> branches = new ArrayList<>();

        public void addBranch(List<Skill> branch) {
            this.branches.add(branch);
        }

        public List<List<Skill>> getBranches() {
            return branches;
        }

        public List<Skill> getAllSkills() {
            List<Skill> allSkills = new ArrayList<>();
            for (List<Skill> branch : branches) {
                allSkills.addAll(branch);
            }
            return allSkills;
        }
    }
}
