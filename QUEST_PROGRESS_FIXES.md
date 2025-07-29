# Исправления системы прогресса квестов

## Проблема
Система квестов не обновляла прогресс в билетах при выполнении действий (крафт, добыча и т.д.).

## Исправления

### 1. Добавлен метод `updateQuestProgress` в QuestTicketItem
**Файл**: `src/main/java/io/github/apace100/origins/quest/QuestTicketItem.java`

```java
public static boolean updateQuestProgress(ItemStack stack, String action, String target, int amount)
```

**Функциональность**:
- Проверяет, является ли ItemStack билетом квеста
- Поддерживает как новую систему с одной целью (`objective`), так и старую с множественными целями (`objectives`)
- Обновляет прогресс цели, если действие и цель совпадают
- Автоматически отмечает цель как завершенную при достижении требуемого количества
- Проверяет общий статус завершения квеста
- Обновляет отображаемое имя билета

### 2. Добавлен метод `isQuestCompleted` в QuestTicketItem
**Файл**: `src/main/java/io/github/apace100/origins/quest/QuestTicketItem.java`

```java
public static boolean isQuestCompleted(ItemStack stack)
```

**Функциональность**:
- Проверяет флаг `completion_ready` в NBT билета

### 3. Обновлен метод `saveQuestToStack` в QuestTicketItem
**Файл**: `src/main/java/io/github/apace100/origins/quest/QuestTicketItem.java`

**Изменения**:
- Поддержка новой системы с одной целью (`quest.getObjective()`)
- Fallback к старой системе с множественными целями
- Улучшенное логирование для отладки

### 4. Исправлен метод `updateTicketProgress` в QuestProgressTracker
**Файл**: `src/main/java/io/github/apace100/origins/quest/QuestProgressTracker.java`

**Изменения**:
- Заменен вызов `QuestTicketItem.updateProgress()` на `QuestTicketItem.updateQuestProgress()`
- Теперь использует новый унифицированный метод обновления прогресса

### 5. Добавлена команда для тестирования
**Файл**: `src/main/java/io/github/apace100/origins/command/TestProgressCommand.java`

**Использование**:
```
/testprogress craft minecraft:bread 1
```

**Функциональность**:
- Тестирует обновление прогресса напрямую через `QuestTicketItem.updateQuestProgress()`
- Также тестирует через `QuestProgressTracker.trackPlayerAction()`
- Показывает результаты в чате

## Поток выполнения

1. **Игрок крафтит предмет** → `QuestCraftingMixin.onQuickCraft()`
2. **Миксин вызывает** → `QuestEventHandlers.onItemCraft()`
3. **Обработчик вызывает** → `QuestProgressTracker.trackPlayerAction()`
4. **Трекер находит билеты** → `QuestProgressTracker.updateTicketProgress()`
5. **Обновляется прогресс** → `QuestTicketItem.updateQuestProgress()`
6. **Проверяется завершение** → `QuestTicketItem.checkAndUpdateCompletionStatus()`

## Поддерживаемые форматы NBT

### Новая система (одна цель):
```json
{
  "objective": {
    "type": "craft",
    "target": "minecraft:bread",
    "amount": 5,
    "progress": 0,
    "completed": false
  }
}
```

### Старая система (множественные цели):
```json
{
  "objectives": {
    "objective_0": {
      "type": "craft",
      "target": "minecraft:bread",
      "amount": 5,
      "progress": 0,
      "completed": false
    }
  },
  "objectives_count": 1
}
```

## Тестирование

1. Запустите игру
2. Создайте доску объявлений
3. Примите квест "Создать хлеб"
4. Используйте команду: `/testprogress craft minecraft:bread 1`
5. Проверьте, что прогресс обновился в билете
6. Скрафтите хлеб обычным способом
7. Проверьте, что прогресс также обновляется

## Логирование

Все ключевые операции логируются с префиксом Origins:
- Обновление прогресса
- Поиск и сопоставление целей
- Завершение квестов
- Ошибки обработки

Проверьте логи для диагностики проблем.