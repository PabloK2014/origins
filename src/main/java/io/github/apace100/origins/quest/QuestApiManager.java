package io.github.apace100.origins.quest;

import io.github.apace100.origins.Origins;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер для управления обновлениями квестов через API
 */
public class QuestApiManager {
    private static final QuestApiManager INSTANCE = new QuestApiManager();
    
    // Интервал обновления в тиках (30 минут = 30 * 60 * 20 = 36000 тиков)
    private static final long UPDATE_INTERVAL_TICKS = 36000L;
    
    // Кэш квестов для каждого класса
    private final Map<String, List<Quest>> questCache = new ConcurrentHashMap<>();
    
    // Время последнего обновления для каждого класса
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    
    // Флаг доступности API
    private boolean apiAvailable = false;
    private long lastApiCheck = 0;
    private static final long API_CHECK_INTERVAL = 12000L; // Проверяем API каждые 10 минут (12000 тиков)
    private boolean isCheckingApi = false; // Флаг для предотвращения множественных проверок
    private boolean isLoadingQuests = false; // Флаг для предотвращения множественных загрузок квестов
    private long lastQuestLoadAttempt = 0; // Время последней попытки загрузки квестов
    private static final long MIN_LOAD_INTERVAL = 1200L; // Минимальный интервал между попытками загрузки (1 минута)
    
    // Классы игроков
    private static final String[] PLAYER_CLASSES = {
        "cook", "courier", "brewer", "blacksmith", "miner", "warrior"
    };
    
    private QuestApiManager() {}
    
    public static QuestApiManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Инициализирует менеджер и запускает первоначальную загрузку квестов
     */
    public void initialize(ServerWorld world) {
        Origins.LOGGER.info("Initializing Quest API Manager...");
        
        checkApiAvailability(world);
    }
    
    /**
     * Обновляет конкретную доску объявлений квестами ТОЛЬКО из системы накопления
     */
    public void updateBoard(ClassBountyBoardBlockEntity board) {
        String boardClass = board.getBoardClass();
        
        // Получаем накопленные квесты ТОЛЬКО из системы накопления
        List<Quest> accumulatedQuests = QuestAccumulation.getInstance().getAccumulatedQuests(boardClass);
        
        // ВСЕГДА очищаем доску сначала
        board.getBounties().clear();
        
        if (!accumulatedQuests.isEmpty()) {
            // Отображаем до 21 квеста (размер доски)
            int questsToShow = Math.min(accumulatedQuests.size(), 21);
            
            Origins.LOGGER.info("🔄 Updating board for class: " + boardClass + " with " + questsToShow + " quests from accumulation");
            
            for (int i = 0; i < questsToShow; i++) {
                Quest quest = accumulatedQuests.get(i);
                ItemStack questTicket = QuestTicketItem.createQuestTicket(quest);
                if (!questTicket.isEmpty()) {
                    board.getBounties().setStack(i, questTicket);
                    Origins.LOGGER.info("  Added quest to slot " + i + ": " + quest.getTitle() + " (ID: " + quest.getId() + ")");
                }
            }
            
            board.markDirty();
            
            // Получаем статистику накопления
            int requestCount = QuestAccumulation.getInstance().getRequestCount(boardClass);
            int maxRequests = QuestAccumulation.getInstance().getMaxRequests();
            
            Origins.LOGGER.info("✅ Board updated for class: " + boardClass + " with " + questsToShow + " quests " +
                "(накоплено: " + accumulatedQuests.size() + ", запрос " + requestCount + "/" + maxRequests + ")");
        } else {
            // Доска остается пустой до получения квестов от API
            Origins.LOGGER.info("📋 Board for class " + boardClass + " remains empty - no accumulated quests");
        }
    }
    
    /**
     * Обновляет квесты если прошло достаточно времени
     */
    public void tick(ServerWorld world) {
        long currentTime = world.getTime();
        
        if (currentTime - lastApiCheck > API_CHECK_INTERVAL) {
            checkApiAvailability(world);
            lastApiCheck = currentTime;
        }
        
        if (!apiAvailable) {
            return;
        }
        
        // Проверяем время до следующего обновления
        Long firstClassLastUpdate = lastUpdateTime.get(PLAYER_CLASSES[0]);
        if (firstClassLastUpdate != null) {
            long timeSinceLastUpdate = currentTime - firstClassLastUpdate;
            long timeUntilNextUpdate = UPDATE_INTERVAL_TICKS - timeSinceLastUpdate;
            
            // Обратный отсчет до следующего запроса (каждые 5 минут)
            if (currentTime % (5 * 60 * 20) == 0) { // Каждые 5 минут
                int minutesLeft = (int) (timeUntilNextUpdate / (20 * 60));
                if (minutesLeft > 0) {
                    QuestApiChatLogger.logCountdownToNextRequest(world.getServer(), minutesLeft);
                }
            }
            
            // Уведомление за 1 минуту (1200 тиков) до обновления
            if (timeUntilNextUpdate <= 1200 && timeUntilNextUpdate > 1180) {
                QuestApiChatLogger.logQuestUpdateWarning(world.getServer(), 1);
            }
            
            // Время для обновления
            if (timeSinceLastUpdate >= UPDATE_INTERVAL_TICKS) {
                Origins.LOGGER.info("🔄 Time to update all quests via optimized API...");
                loadAllQuests(world);
            }
        } else {
            // Первый запуск - загружаем квесты сразу
            Origins.LOGGER.info("🚀 First time loading quests...");
            loadAllQuests(world);
        }
    }
    
    /**
     * Загружает квесты для всех классов через отдельные асинхронные запросы
     */
    private void loadAllQuests(ServerWorld world) {
        long currentTime = world.getTime();
        
        // Проверяем, не загружаем ли мы уже квесты
        if (isLoadingQuests) {
            Origins.LOGGER.info("⏳ Already loading quests, skipping...");
            return;
        }
        
        // Проверяем минимальный интервал между попытками загрузки
        if (currentTime - lastQuestLoadAttempt < MIN_LOAD_INTERVAL) {
            Origins.LOGGER.info("⏳ Too soon to load quests again, waiting...");
            return;
        }
        
        isLoadingQuests = true;
        lastQuestLoadAttempt = currentTime;
        
        Origins.LOGGER.info("🚀 Loading quests for ALL classes with separate async requests...");
        
        QuestApiChatLogger.logApiRequest(world.getServer(), "ALL CLASSES (6 separate requests)", 30); // 6 классов * 5 квестов
        
        // Используем новый метод с отдельными запросами
        QuestApiClient.getAllQuestsSeparately(5, this)
            .thenCompose(allQuests -> {
                // Проверяем, есть ли классы без квестов, и повторяем запросы для них
                return QuestApiClient.retryMissingClasses(allQuests, 5);
            })
            .thenAccept(allQuests -> {
                try {
                    if (!allQuests.isEmpty()) {
                        long updateTime = world.getTime();
                        int totalQuests = 0;
                        int successfulClasses = 0;
                        
                        for (String playerClass : PLAYER_CLASSES) {
                            List<Quest> classQuests = allQuests.getOrDefault(playerClass, new ArrayList<>());
                            
                            if (!classQuests.isEmpty()) {
                                successfulClasses++;
                                
                                // Отправляем сообщение о генерации квестов для класса
                                QuestApiChatLogger.logClassQuestsGenerated(world.getServer(), playerClass, classQuests.size());
                                
                                // Используем ТОЛЬКО систему накопления квестов с уведомлениями
                                List<Quest> accumulatedQuests = QuestAccumulation.getInstance().addQuestsForClass(playerClass, classQuests, world.getServer());
                                
                                // НЕ обновляем кэш - используем только систему накопления
                                lastUpdateTime.put(playerClass, updateTime);
                                totalQuests += accumulatedQuests.size();
                                
                                Origins.LOGGER.info("📋 Loaded " + classQuests.size() + " new quests for class: " + playerClass);
                                Origins.LOGGER.info("📊 Total accumulated quests for " + playerClass + ": " + accumulatedQuests.size());
                                
                                // Детальный лог новых квестов
                                for (int i = 0; i < classQuests.size(); i++) {
                                    Quest quest = classQuests.get(i);
                                    Origins.LOGGER.info("  New Quest " + (i+1) + ": " + quest.getTitle() + " (ID: " + quest.getId() + ")");
                                }
                                
                                updateBoardsForClass(playerClass, world);
                            } else {
                                Origins.LOGGER.warn("❌ No quests received for class: " + playerClass);
                            }
                        }
                        
                        Origins.LOGGER.info("🎯 SEPARATE REQUESTS COMPLETED: " + totalQuests + " total quests loaded for " + successfulClasses + "/" + PLAYER_CLASSES.length + " classes");
                        
                        // Отправляем сообщение о результатах отдельных запросов
                        QuestApiChatLogger.logSeparateRequestsResult(world.getServer(), successfulClasses, PLAYER_CLASSES.length, totalQuests);
                        
                        if (totalQuests > 0) {
                            // Отправляем специальное сообщение о том, что квесты появились
                            QuestApiChatLogger.logQuestsAppeared(world.getServer(), totalQuests);
                        } else {
                            QuestApiChatLogger.logApiError(world.getServer(), "ALL CLASSES", "Не получено квестов ни для одного класса");
                        }
                    } else {
                        Origins.LOGGER.warn("❌ No quests received from separate API requests");
                        QuestApiChatLogger.logApiError(world.getServer(), "ALL CLASSES", "Не получено квестов");
                    }
                } finally {
                    isLoadingQuests = false; // Сбрасываем флаг в любом случае
                }
            })
            .exceptionally(throwable -> {
                try {
                    Origins.LOGGER.error("🔥 Failed to load quests from separate API requests", throwable);
                    QuestApiChatLogger.logDetailedApiError(world.getServer(), "ALL CLASSES", throwable.getMessage(), true);
                    
                    // При ошибке не обновляем время последнего успешного запроса
                    // Это позволит системе повторить запрос через MIN_LOAD_INTERVAL
                    Origins.LOGGER.info("🔄 Will retry quest loading after minimum interval");
                    
                } finally {
                    isLoadingQuests = false; // Сбрасываем флаг в случае ошибки
                }
                return null;
            });
    }

    /**
     * Обновляет все доски объявлений указанного класса (используем ТОЛЬКО систему накопления)
     */
    private void updateBoardsForClass(String playerClass, ServerWorld world) {
        // Получаем квесты из системы накопления, а НЕ из кэша
        List<Quest> quests = QuestAccumulation.getInstance().getAccumulatedQuests(playerClass);
        if (quests == null || quests.isEmpty()) {
            Origins.LOGGER.warn("No accumulated quests to update boards for class: " + playerClass);
            return;
        }
        
        Origins.LOGGER.info("Updating boards for class: " + playerClass + " with " + quests.size() + " accumulated quests");
        
        // Доски будут автоматически обновлены при следующем обращении к ним
        // через метод updateBoard в ClassBountyBoardBlockEntity
        Origins.LOGGER.info("Quests updated for class " + playerClass + ". Boards will refresh automatically.");
        
        for (Quest quest : quests) {
            Origins.LOGGER.info("  Available quest: " + quest.getTitle() + " (ID: " + quest.getId() + ")");
        }
    }
    
    /**
     * Получает квесты для указанного класса из кэша
     */
    public List<Quest> getQuestsForClass(String playerClass) {
        return questCache.getOrDefault(playerClass, new ArrayList<>());
    }
    
    /**
     * Получает время до следующего обновления в тиках
     */
    public long getTimeUntilNextUpdate(String playerClass, ServerWorld world) {
        Long lastUpdate = lastUpdateTime.get(playerClass);
        if (lastUpdate == null) {
            return 0;
        }
        
        long currentTime = world.getTime();
        long timeSinceUpdate = currentTime - lastUpdate;
        return Math.max(0, UPDATE_INTERVAL_TICKS - timeSinceUpdate);
    }
    
    /**
     * Получает время до следующего обновления в минутах
     */
    public int getMinutesUntilNextUpdate(String playerClass, ServerWorld world) {
        long ticksUntilUpdate = getTimeUntilNextUpdate(playerClass, world);
        return (int) (ticksUntilUpdate / (20 * 60));
    }
    
    /**
     * Принудительно обновляет квесты для указанного класса
     */
    public void forceUpdateClass(String playerClass, ServerWorld world) {
        if ("all".equals(playerClass)) {
            Origins.LOGGER.info("Force updating quests for ALL classes");
            loadAllQuests(world);
        } else {
            Origins.LOGGER.info("Force updating quests for single class: " + playerClass);
            loadQuestsForSingleClass(playerClass, world);
        }
    }
    
    /**
     * Загружает квесты только для одного класса
     */
    private void loadQuestsForSingleClass(String playerClass, ServerWorld world) {
        long currentTime = world.getTime();
        
        // Проверяем, не загружаем ли мы уже квесты
        if (isLoadingQuests) {
            Origins.LOGGER.info("⏳ Already loading quests, skipping single class request...");
            return;
        }
        
        // Проверяем минимальный интервал между попытками загрузки
        if (currentTime - lastQuestLoadAttempt < MIN_LOAD_INTERVAL) {
            Origins.LOGGER.info("⏳ Too soon to load quests again, waiting...");
            return;
        }
        
        isLoadingQuests = true;
        lastQuestLoadAttempt = currentTime;
        
        Origins.LOGGER.info("🚀 Loading quests for single class: " + playerClass);
        
        QuestApiChatLogger.logApiRequest(world.getServer(), "CLASS " + playerClass.toUpperCase(), 5);
        
        // Используем прямой запрос для одного класса
        QuestApiClient.getQuestsForSingleClass(playerClass, 5)
            .thenAccept(newQuests -> {
                try {
                    if (!newQuests.isEmpty()) {
                        long updateTime = world.getTime();
                        
                        // Отправляем сообщение о генерации квестов для класса
                        QuestApiChatLogger.logClassQuestsGenerated(world.getServer(), playerClass, newQuests.size());
                        
                        // Используем ТОЛЬКО систему накопления квестов с уведомлениями
                        List<Quest> accumulatedQuests = QuestAccumulation.getInstance().addQuestsForClass(playerClass, newQuests, world.getServer());
                        
                        // НЕ обновляем кэш - используем только систему накопления
                        lastUpdateTime.put(playerClass, updateTime);
                        
                        Origins.LOGGER.info("📋 Loaded " + newQuests.size() + " new quests for class: " + playerClass);
                        Origins.LOGGER.info("📊 Total accumulated quests for " + playerClass + ": " + accumulatedQuests.size());
                        
                        // Детальный лог новых квестов
                        for (int i = 0; i < newQuests.size(); i++) {
                            Quest quest = newQuests.get(i);
                            Origins.LOGGER.info("  New Quest " + (i+1) + ": " + quest.getTitle() + " (ID: " + quest.getId() + ")");
                        }
                        
                        updateBoardsForClass(playerClass, world);
                        
                        // Отправляем сообщение о том, что квесты появились
                        QuestApiChatLogger.logQuestsAppeared(world.getServer(), newQuests.size());
                    } else {
                        Origins.LOGGER.warn("❌ No quests received for class: " + playerClass);
                        QuestApiChatLogger.logApiError(world.getServer(), playerClass.toUpperCase(), "Не получено квестов");
                    }
                } finally {
                    isLoadingQuests = false; // Сбрасываем флаг в любом случае
                }
            })
            .exceptionally(throwable -> {
                try {
                    Origins.LOGGER.error("🔥 Failed to load quests for class " + playerClass, throwable);
                    QuestApiChatLogger.logDetailedApiError(world.getServer(), playerClass.toUpperCase(), throwable.getMessage(), true);
                    
                    Origins.LOGGER.info("🔄 Will retry quest loading after minimum interval");
                    
                } finally {
                    isLoadingQuests = false; // Сбрасываем флаг в случае ошибки
                }
                return null;
            });
    }
    
    /**
     * Проверяет доступность API
     */
    private void checkApiAvailability(ServerWorld world) {
        if (isCheckingApi) {
            Origins.LOGGER.info("⏳ API check already in progress, skipping...");
            return;
        }
        
        isCheckingApi = true;
        Origins.LOGGER.info("🔍 Starting API availability check...");
        
        QuestApiClient.isApiAvailable()
            .thenAccept(available -> {
                isCheckingApi = false;
                
                if (available != apiAvailable) {
                    apiAvailable = available;
                    if (available) {
                        Origins.LOGGER.info("✅ Quest API is now AVAILABLE!");
                        QuestApiChatLogger.logApiConnected(world.getServer());
                        QuestApiChatLogger.logQuestUpdate(world.getServer());
                        loadAllQuests(world);
                    } else {
                        Origins.LOGGER.warn("❌ Quest API is NOT AVAILABLE");
                        QuestApiChatLogger.logApiUnavailable(world.getServer());
                    }
                } else {
                    Origins.LOGGER.info("🔄 API status unchanged: " + (available ? "AVAILABLE" : "UNAVAILABLE"));
                }
            })
            .exceptionally(throwable -> {
                isCheckingApi = false;
                if (apiAvailable) {
                    Origins.LOGGER.warn("🔥 Quest API became unavailable due to exception: " + throwable.getMessage());
                    QuestApiChatLogger.logApiUnavailable(world.getServer());
                    apiAvailable = false;
                } else {
                    Origins.LOGGER.warn("🔥 API check failed: " + throwable.getMessage());
                }
                return null;
            });
    }
    
    /**
     * Проверяет доступность API синхронно
     */
    public boolean isApiAvailable() {
        return apiAvailable;
    }
    
    /**
     * Создает ItemStack билета квеста из Quest объекта
     */
    public ItemStack createQuestTicket(Quest quest) {
        return QuestTicketItem.createQuestTicket(quest);
    }
    
    /**
     * Получает все доступные классы
     */
    public String[] getAvailableClasses() {
        return PLAYER_CLASSES.clone();
    }
    
    /**
     * Немедленно обновляет класс при получении квестов (вызывается из QuestApiClient)
     */
    public void updateClassImmediately(String playerClass, List<Quest> newQuests) {
        if (newQuests == null || newQuests.isEmpty()) {
            Origins.LOGGER.warn("No quests to update immediately for class: " + playerClass);
            return;
        }
        
        Origins.LOGGER.info("🚀 IMMEDIATE UPDATE for class " + playerClass + ": " + newQuests.size() + " quests");
        
        // Используем ТОЛЬКО систему накопления квестов (убираем кэш)
        List<Quest> accumulatedQuests = QuestAccumulation.getInstance().addQuestsForClass(playerClass, newQuests, null);
        
        // Отправляем сообщение о генерации квестов для класса
        try {
            if (Origins.getServer() != null) {
                QuestApiChatLogger.logClassQuestsGenerated(Origins.getServer(), playerClass, newQuests.size());
            }
        } catch (Exception e) {
            Origins.LOGGER.warn("Could not send chat message for immediate update: " + e.getMessage());
        }
        
        Origins.LOGGER.info("✅ Class " + playerClass + " updated immediately with " + accumulatedQuests.size() + " total quests");
    }
    
    /**
     * Удаляет квест из системы накопления (вызывается при принятии квеста)
     */
    public void removeQuestFromAccumulation(String playerClass, String questId) {
        // Удаляем квест из системы накопления (НЕ из кэша)
        boolean removed = QuestAccumulation.getInstance().removeQuest(playerClass, questId);
        if (removed) {
            Origins.LOGGER.info("✅ Removed quest " + questId + " from accumulation for class " + playerClass);
            
            // Принудительно обновляем все доски этого класса
            refreshBoardsForClass(playerClass);
        } else {
            Origins.LOGGER.warn("❌ Failed to remove quest " + questId + " from accumulation for class " + playerClass);
        }
    }
    
    /**
     * Принудительно обновляет все доски указанного класса
     */
    private void refreshBoardsForClass(String playerClass) {
        Origins.LOGGER.info("🔄 Refreshing all boards for class: " + playerClass);
        // Этот метод будет вызван автоматически при следующем обращении к доске
        // через метод updateBoard в ClassBountyBoardBlockEntity
    }
}