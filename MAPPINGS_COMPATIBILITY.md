# Совместимость с Mappings в Minecraft 1.20.1

## Проблемы с mappings

В Minecraft 1.20.1 некоторые API изменились и теперь возвращают `RegistryEntry<T>` (Holder) вместо прямых объектов.

## Решения

### Звуки
В Minecraft 1.20.1 звуки работают напрямую:
```java
// Правильно для 1.20.1:
player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

// Неправильно (устаревший способ):
player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP.value(), 1.0f, 1.0f);

// Для удобства можно использовать SoundHelper:
SoundHelper.playLevelUpSound(player);
```

### Общие рекомендации
1. Всегда проверяйте тип объекта перед использованием
2. Используйте try-catch для обработки исключений mappings
3. Создавайте утилитарные классы для проблемных API

## Тестирование
Тестируйте мод с разными версиями mappings для обеспечения совместимости.