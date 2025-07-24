# Дизайн системы досок объявлений с квестами

## Обзор

Система досок объявлений представляет собой интегрированную с Origins систему квестов, которая позволяет игрокам получать задания соответствующие их классу и получать опыт для прокачки навыков.

## Архитектура

### Основные компоненты

1. **BountyBoard** - блок доски объявлений
2. **BountyBoardBlockEntity** - блок-сущность для хранения состояния
3. **BountyBoardScreen** - GUI интерфейс
4. **QuestSystem** - система управления квестами
5. **SkillPointToken** - предмет-награда с опытом
6. **QuestDataLoader** - загрузчик квестов из JSON

### Структура данных

#### Quest (Квест)
```java
public class Quest {
    private String id;
    private String playerClass;    // origins:warrior, origins:cook, etc.
    private int level;            // 1, 2, 3
    private String title;         // Название квеста
    private String description;   // Описание
    private QuestObjective objective; // Цель квеста
    private int timeLimit;        // Время в минутах
    private QuestReward reward;   // Награда
}
```

#### QuestObjective (Цель квеста)
```java
public class QuestObjective {
    private String type;          // "collect", "kill", "craft"
    private String target;        // "minecraft:bone", "minecraft:zombie"
    private int amount;           // Количество
    private boolean completed;    // Выполнено ли
    private int progress;         // Текущий прогресс
}
```

#### QuestReward (Награда)
```java
public class QuestReward {
    private String type;          // "skill_point_token"
    private int tier;             // 1, 2, 3
    private int experience;       // 500, 1000, 1500
}
```

## Компоненты и интерфейсы

### 1. Регистрация блоков и предметов

```java
public class QuestRegistry {
    public static final Block BOUNTY_BOARD = new BountyBoard(FabricBlockSettings.of(Material.STONE));
    public static final BlockEntityType<BountyBoardBlockEntity> BOUNTY_BOARD_ENTITY;
    public static final Item SKILL_POINT_TOKEN_TIER1;
    public static final Item SKILL_POINT_TOKEN_TIER2;
    public static final Item SKILL_POINT_TOKEN_TIER3;
}
```

### 2. Система квестов

```java
public class QuestManager {
    private Map<String, List<Quest>> questsByClass;
    private Map<UUID, ActiveQuest> activeQuests;
    
    public List<Quest> getRandomQuestsForClass(String playerClass, int count);
    public boolean canTakeQuest(PlayerEntity player, Quest quest);
    public void startQuest(PlayerEntity player, Quest quest);
    public void completeQuest(PlayerEntity player, Quest quest);
}
```

### 3. JSON структура квестов

```json
{
  "quests": [
    {
      "id": "warrior_bones_1",
      "playerClass": "origins:warrior",
      "level": 1,
      "title": "Сбор костей",
      "description": "Соберите кости для тренировки",
      "objective": {
        "type": "collect",
        "target": "minecraft:bone",
        "amount": 10
      },
      "timeLimit": 30,
      "reward": {
        "type": "skill_point_token",
        "tier": 1,
        "experience": 500
      }
    }
  ]
}
```

### 4. GUI интерфейс

Интерфейс будет состоять из:
- **Левая панель**: Список доступных квестов (4 случайных)
- **Правая панель**: Детали выбранного квеста
- **Кнопки**: "Взять квест", "Сдать квест", "Обновить список"

### 5. Интеграция с системой классов

```java
public class PlayerClassDetector {
    public static String getPlayerClass(PlayerEntity player) {
        OriginComponent component = ModComponents.ORIGIN.get(player);
        Origin origin = component.getOrigin(OriginLayers.getLayer(Origins.identifier("origin")));
        return origin != null ? origin.getIdentifier().toString() : "origins:human";
    }
}
```

## Логика работы

### Процесс взятия квеста:
1. Игрок открывает доску объявлений
2. Система определяет класс игрока
3. Загружаются 4 случайных квеста для этого класса
4. Игрок выбирает квест и нажимает "Взять"
5. Квест добавляется в активные квесты игрока
6. Начинается отсчет времени

### Процесс выполнения квеста:
1. Система отслеживает прогресс через события (сбор предметов, убийство мобов)
2. Обновляется прогресс в интерфейсе
3. При достижении цели квест помечается как выполненный

### Процесс сдачи квеста:
1. Игрок возвращается к доске объявлений
2. Нажимает "Сдать квест"
3. Система проверяет выполнение
4. Выдается награда (SkillPointToken)
5. Квест удаляется из активных

## Файловая структура

```
src/main/java/io/github/apace100/origins/quest/
├── BountyBoard.java
├── BountyBoardBlockEntity.java
├── BountyBoardScreen.java
├── BountyBoardScreenHandler.java
├── Quest.java
├── QuestObjective.java
├── QuestReward.java
├── QuestManager.java
├── QuestDataLoader.java
├── ActiveQuest.java
├── PlayerClassDetector.java
├── QuestRegistry.java
└── SkillPointToken.java

src/main/resources/data/origins/quests/
├── warrior_quests.json
├── cook_quests.json
├── blacksmith_quests.json
├── miner_quests.json
├── courier_quests.json
└── brewer_quests.json
```

## Интеграция с существующими системами

### С системой навыков:
- SkillPointToken при использовании добавляет опыт через ProfessionComponent
- Опыт добавляется к текущему классу игрока

### С системой Origins:
- Определение класса игрока через OriginComponent
- Проверка доступности квестов по классу

### С системой переводов:
- Все тексты квестов поддерживают локализацию
- Переводы хранятся в lang файлах Origins