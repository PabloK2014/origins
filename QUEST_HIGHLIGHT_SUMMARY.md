# Резюме: Система подсветки квестов

## Что реализовано

### 1. Подсветка квестов в центральной части доски объявлений
- При клике на квест в левой панели соответствующий квест подсвечивается в центральной сетке 3x7
- Золотая рамка с пульсирующим эффектом длительностью 3 секунды
- Автоматическая очистка при закрытии интерфейса

### 2. Существующая подсветка билетов в инвентаре (уже была)
- При клике на квест в левой панели соответствующий билет подсвечивается в инвентаре игрока
- Работает параллельно с новой системой подсветки центральных квестов

## Технические детали

### Измененные файлы:
- `src/main/java/io/github/apace100/origins/quest/BountyBoardScreen.java`

### Добавленные переменные:
- `highlightedCenterSlot` - индекс подсвеченного слота в центре
- `highlightStartTime` - время начала подсветки

### Добавленные методы:
- `highlightQuestInCenter()` - запуск подсветки квеста в центре
- `drawCenterQuestHighlights()` - отрисовка подсветки
- `drawCenterSlotHighlight()` - отрисовка конкретного слота
- `getCenterHighlightIntensity()` - расчет интенсивности пульсации
- `clearCenterQuestHighlight()` - очистка подсветки

### Обновленные методы:
- `drawForeground()` - добавлен вызов отрисовки подсветки
- `mouseClicked()` - добавлен вызов подсветки при клике
- `close()` - добавлена очистка подсветки при закрытии

## Алгоритм работы

1. **Клик на квест в левой панели**
2. **Поиск соответствующего квеста в центральной части по ID**
3. **Установка индекса слота и времени начала подсветки**
4. **Отрисовка золотой рамки с пульсацией в каждом кадре**
5. **Автоматическое затухание через 3 секунды**

## Визуальные эффекты

- **Цвет**: Золотой (#FFD700)
- **Толщина рамки**: 2 пикселя
- **Фон**: Полупрозрачный желтый
- **Пульсация**: Период 1 секунда, интенсивность 30%-70%
- **Длительность**: 3 секунды с плавным затуханием

## Совместимость

✅ Работает с существующей системой подсветки билетов
✅ Совместимо с drag-and-drop функциональностью  
✅ Не конфликтует с системой маскировки квестов
✅ Не влияет на производительность

## Использование

1. Откройте доску объявлений
2. Кликните на любой квест в левой панели
3. Квест будет подсвечен как в центральной части, так и в инвентаре (если есть билет)
4. Подсветка автоматически исчезнет через 3 секунды

Система успешно портирована и готова к использованию!