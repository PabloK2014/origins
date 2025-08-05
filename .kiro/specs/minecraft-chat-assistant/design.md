# Design Document

## Overview

Данный дизайн описывает реализацию интеллектуального чат-помощника для Minecraft и исправления системы квестов. Система включает в себя новую команду `/ask`, которая позволяет игрокам задавать вопросы о Minecraft и получать ответы от AI, а также исправления для корректной работы системы квестов с API.

## Architecture

### Компоненты системы

1. **MinecraftChatAssistant** - основной класс для обработки команды `/ask`
2. **ChatAssistantApiClient** - клиент для взаимодействия с AI API
3. **ChatMessageAnimator** - класс для анимации сообщений в чате
4. **QuestSystemFixes** - исправления для системы квестов
5. **FastAPI Chat Endpoint** - новый эндпоинт для чат-помощника

### Архитектурная диаграмма

```mermaid
graph TB
    Player[Игрок] --> AskCommand[/ask команда]
    AskCommand --> MinecraftChatAssistant[MinecraftChatAssistant]
    MinecraftChatAssistant --> ChatAssistantApiClient[ChatAssistantApiClient]
    ChatAssistantApiClient --> FastAPI[FastAPI /chat endpoint]
    FastAPI --> OpenRouter[OpenRouter AI API]
    OpenRouter --> FastAPI
    FastAPI --> ChatAssistantApiClient
    ChatAssistantApiClient --> ChatMessageAnimator[ChatMessageAnimator]
    ChatMessageAnimator --> MinecraftChat[Minecraft Chat]
    
    QuestApiManager[QuestApiManager] --> QuestSystemFixes[QuestSystemFixes]
    QuestSystemFixes --> QuestApiClient[QuestApiClient]
    QuestApiClient --> FastAPIQuests[FastAPI /quests/all]
    FastAPIQuests --> QuestValidation[Quest Validation]
    QuestValidation --> QuestCache[Quest Cache]
    QuestCache --> BountyBoard[Bounty Board]
```

## Components and Interfaces

### 1. MinecraftChatAssistant

**Назначение:** Основной класс для обработки команды `/ask` и координации работы чат-помощника.

**Интерфейс:**
```java
public class MinecraftChatAssistant {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess);
    private static int handleAskCommand(CommandContext<ServerCommandSource> context, String question);
    private static void sendTypingAnimation(ServerCommandSource source);
    private static void sendFormattedResponse(ServerCommandSource source, String response);
}
```

### 2. ChatAssistantApiClient

**Назначение:** HTTP клиент для отправки запросов к AI API и получения ответов.

**Интерфейс:**
```java
public class ChatAssistantApiClient {
    public static CompletableFuture<String> askQuestion(String question, String minecraftVersion);
    private static String buildPrompt(String question, String minecraftVersion);
    private static boolean isApiAvailable();
}
```

### 3. ChatMessageAnimator

**Назначение:** Создание анимированных сообщений в чате с эффектами печатания и цветовым форматированием.

**Интерфейс:**
```java
public class ChatMessageAnimator {
    public static void sendAnimatedMessage(ServerCommandSource source, String message, Formatting color);
    public static void sendTypingIndicator(ServerCommandSource source, String indicator);
    public static void sendMultipartMessage(ServerCommandSource source, List<String> parts, int delayTicks);
    private static Text formatCraftingRecipe(String recipe);
    private static Text formatGameMechanic(String mechanic);
}
```

### 4. QuestSystemFixes

**Назначение:** Исправления для корректной работы системы квестов.

**Интерфейс:**
```java
public class QuestSystemFixes {
    public static void applyFixes(QuestApiManager manager);
    public static void fixQuestAccumulation(ClassBountyBoardBlockEntity board, List<Quest> newQuests);
    public static void fixTimingIssues(QuestApiManager manager);
    public static void addQuestNotifications(ServerWorld world);
}
```

### 5. FastAPI Chat Endpoint

**Назначение:** Новый эндпоинт в FastAPI для обработки вопросов от чат-помощника.

**Интерфейс:**
```python
@app.post("/chat/ask")
async def ask_question(request: ChatRequest) -> ChatResponse:
    # Обработка вопроса и генерация ответа
    pass

class ChatRequest(BaseModel):
    question: str
    minecraft_version: str = "1.20.1"

class ChatResponse(BaseModel):
    answer: str
    success: bool
    error_message: Optional[str] = None
```

## Data Models

### ChatRequest
```python
class ChatRequest(BaseModel):
    question: str
    minecraft_version: str = "1.20.1"
    context: Optional[str] = None
```

### ChatResponse
```python
class ChatResponse(BaseModel):
    answer: str
    success: bool
    error_message: Optional[str] = None
    response_type: str = "general"  # general, recipe, mechanic
```

### QuestAccumulation
```java
public class QuestAccumulation {
    private Map<String, List<Quest>> accumulatedQuests;
    private Map<String, Integer> requestCounts;
    private static final int MAX_REQUESTS = 3;
    private static final int QUESTS_PER_REQUEST = 5;
}
```

## Error Handling

### 1. JSON Parsing Errors
- **Проблема:** Ошибки парсинга JSON при получении квестов от API
- **Решение:** Валидация JSON перед парсингом, сохранение корректных квестов при ошибке в одном из них
- **Реализация:** Try-catch блоки для каждого квеста отдельно

### 2. API Connectivity Issues
- **Проблема:** Недоступность AI API или Quest API
- **Решение:** Graceful degradation с информативными сообщениями пользователю
- **Реализация:** Проверка доступности API перед запросами

### 3. Quest System Timing
- **Проблема:** Неправильное управление таймингом запросов к API
- **Решение:** Исправление логики таймеров и добавление уведомлений
- **Реализация:** Рефакторинг QuestApiManager

## Testing Strategy

### 1. Unit Tests
- Тестирование парсинга JSON с различными сценариями ошибок
- Тестирование валидации квестов
- Тестирование форматирования сообщений чата

### 2. Integration Tests
- Тестирование взаимодействия с AI API
- Тестирование команды `/ask` с различными вопросами
- Тестирование системы квестов с накоплением

### 3. Manual Testing
- Проверка анимаций в чате
- Проверка корректности ответов AI
- Проверка работы системы квестов в течение нескольких циклов

## Implementation Details

### Команда /ask
```java
dispatcher.register(CommandManager.literal("ask")
    .then(CommandManager.argument("question", StringArgumentType.greedyString())
        .executes(context -> handleAskCommand(context, 
            StringArgumentType.getString(context, "question")))));
```

### Анимация печатания
```java
public static void sendTypingIndicator(ServerCommandSource source, String indicator) {
    source.sendFeedback(() -> Text.literal("🤖 " + indicator + "...")
        .formatted(Formatting.GRAY), false);
}
```

### Накопление квестов
```java
public void addQuestsToBoard(ClassBountyBoardBlockEntity board, List<Quest> newQuests) {
    String boardClass = board.getBoardClass();
    List<Quest> accumulated = accumulatedQuests.computeIfAbsent(boardClass, k -> new ArrayList<>());
    
    accumulated.addAll(newQuests);
    
    // Проверяем, нужно ли очистить доску (после 3 запросов)
    int requestCount = requestCounts.getOrDefault(boardClass, 0) + 1;
    if (requestCount >= MAX_REQUESTS) {
        accumulated.clear();
        accumulated.addAll(newQuests);
        requestCounts.put(boardClass, 1);
    } else {
        requestCounts.put(boardClass, requestCount);
    }
    
    // Обновляем доску
    updateBoardWithQuests(board, accumulated);
}
```

### Промпт для AI
```python
def build_minecraft_prompt(question: str, version: str) -> str:
    return f"""
Ты - эксперт по Minecraft версии {version}. Отвечай на русском языке кратко и точно.
Если вопрос касается крафта, покажи рецепт в понятном формате.
Если вопрос о механиках игры, объясни просто и понятно.

Вопрос игрока: {question}

Ответ должен быть:
- На русском языке
- Кратким (не более 3-4 предложений)
- Точным для версии {version}
- С примерами если нужно
"""
```

### Валидация JSON квестов
```java
public static List<Quest> parseQuestsWithValidation(String jsonResponse) {
    List<Quest> validQuests = new ArrayList<>();
    
    try {
        JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
        
        for (String className : PLAYER_CLASSES) {
            if (responseObj.has(className)) {
                JsonArray questsArray = responseObj.getAsJsonArray(className);
                
                for (int i = 0; i < questsArray.size(); i++) {
                    try {
                        JsonObject questObj = questsArray.get(i).getAsJsonObject();
                        Quest quest = parseQuestFromJsonObject(questObj);
                        
                        if (quest != null && validateQuest(quest)) {
                            validQuests.add(quest);
                        }
                    } catch (Exception e) {
                        Origins.LOGGER.warn("Failed to parse quest " + i + " for class " + className + ": " + e.getMessage());
                        // Продолжаем обработку остальных квестов
                    }
                }
            }
        }
    } catch (Exception e) {
        Origins.LOGGER.error("Critical JSON parsing error", e);
    }
    
    return validQuests;
}
```