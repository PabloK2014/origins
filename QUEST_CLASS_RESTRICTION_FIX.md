# Исправление системы ограничений по классам для квестов

## Проблема
Игроки могли брать квесты для любых классов. Например, воин мог взять квест для повара, что нарушало игровую логику и баланс.

## Решение

### 1. Улучшена система проверки совместимости классов
- Убрана возможность брать квесты для "human" всеми классами
- Добавлена нормализация названий классов (убираются префиксы "origins:")
- Теперь каждый класс может брать только свои квесты

### 2. Создан enum QuestAcceptanceError
- Добавлены понятные сообщения об ошибках на русском языке
- Включены все типы ошибок принятия квестов
- Поддержка локализации сообщений

### 3. Улучшена обработка ошибок
- Игроки получают понятные сообщения о том, почему квест нельзя взять
- Добавлено логирование для отладки
- Показывается требуемый класс для квеста

## Технические изменения

### Файлы изменений:

#### QuestTicketAcceptanceHandler.java
- Обновлен метод `isClassCompatible()`:
  - Убрана логика "квесты для human могут брать все"
  - Добавлена нормализация названий классов
  - Добавлено подробное логирование
- Добавлен метод `normalizeClassName()` для обработки префиксов

#### QuestAcceptanceError.java (новый файл)
- Создан enum с типами ошибок принятия квестов
- Добавлены локализованные сообщения на русском языке
- Поддержка всех типов ошибок системы

### Логика проверки классов:

```java
private boolean isClassCompatible(String playerClass, String questClass) {
    // Нормализуем названия классов (убираем префиксы)
    String normalizedPlayerClass = normalizeClassName(playerClass);
    String normalizedQuestClass = normalizeClassName(questClass);
    
    // Точное совпадение нормализованных классов
    return normalizedPlayerClass.equals(normalizedQuestClass);
}
```

### Нормализация классов:
- Убирается префикс "origins:" если присутствует
- Приведение к нижнему регистру для единообразия
- Fallback к "human" для null значений

## Поведение системы

### До исправления:
- ✗ Воин мог взять квест для повара
- ✗ Любой класс мог брать квесты для "human"
- ✗ Неясные сообщения об ошибках

### После исправления:
- ✅ Воин может брать только квесты для воина
- ✅ Повар может брать только квесты для повара
- ✅ Понятные сообщения об ошибках на русском языке

## Сообщения об ошибках

### Типы ошибок:
- `PROFESSION_MISMATCH`: "Этот квест предназначен для класса: [Класс]"
- `ALREADY_HAS_QUEST`: "У вас уже есть этот квест!"
- `QUEST_LIMIT_REACHED`: "Достигнут лимит активных квестов! (X/Y)"
- `INVENTORY_FULL`: "Нет места в инвентаре для билета квеста!"
- `PLAYER_LEVEL_TOO_LOW`: "Недостаточный уровень для этого квеста!"

### Локализация классов:
- `warrior` → "Воин"
- `cook` → "Повар"
- `miner` → "Шахтер"
- `blacksmith` → "Кузнец"
- `courier` → "Курьер"
- `brewer` → "Пивовар"

## Логирование

Для отладки добавлены подробные логи:
```
[Origins] Проверяем совместимость: игрок 'origins:warrior' -> 'warrior', квест 'cook' -> 'cook'
[Origins] Результат проверки совместимости: false
[Origins] Класс игрока warrior не подходит для квеста класса cook
```

## Тестирование

### Сценарии тестирования:
1. **Воин пытается взять квест повара** → Ошибка "Этот квест предназначен для класса: Повар"
2. **Повар берет квест повара** → Успешно
3. **Игрок с классом "human" берет любой квест** → Только квесты для "human"

### Команды для тестирования:
- Создайте игроков разных классов
- Попробуйте взять квесты для других классов
- Проверьте сообщения об ошибках

## Совместимость
- ✅ Работает с существующей системой квестов
- ✅ Совместимо с системой подсветки
- ✅ Не влияет на производительность
- ✅ Обратная совместимость с существующими квестами

Теперь система квестов корректно ограничивает доступ к квестам по классам игроков, обеспечивая правильный игровой баланс и логику.