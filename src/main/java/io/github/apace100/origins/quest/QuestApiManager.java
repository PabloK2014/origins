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
     * Обновляет конкретную доску объявлений квестами из кэша
     */
    public void updateBoard(ClassBountyBoardBlockEntity board) {
        String boardClass = board.getBoardClass();
        List<Quest> quests = questCache.get(boardClass);
        
        if (quests != null && !quests.isEmpty()) {
            board.getBounties().clear();
            for (int i = 0; i < Math.min(quests.size(), 21); i++) {
                Quest quest = quests.get(i);
                ItemStack questTicket = QuestTicketItem.createQuestTicket(quest);
                if (!questTicket.isEmpty()) {
                    board.getBounties().setStack(i, questTicket);
                }
            }
            board.markDirty();
            Origins.LOGGER.info("Updated board for class: " + boardClass + " with " + quests.size() + " quests");
        } else {
            board.getBounties().clear(); // Очищаем доску, если квестов нет
            Origins.LOGGER.info("No quests available for board class: " + boardClass);
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
        
        Long firstClassLastUpdate = lastUpdateTime.get(PLAYER_CLASSES[0]);
        if (firstClassLastUpdate == null || currentTime - firstClassLastUpdate >= UPDATE_INTERVAL_TICKS) {
            Origins.LOGGER.info("🔄 Time to update all quests via optimized API...");
            loadAllQuests(world);
        }
    }
    
    /**
     * Загружает квесты для всех классов одним оптимизированным запросом
     */
    private void loadAllQuests(ServerWorld world) {
        Origins.LOGGER.info("🚀 Loading quests for ALL classes with optimized API...");
        
        QuestApiChatLogger.logApiRequest(world.getServer(), "ALL CLASSES", 30); // 6 классов * 5 квестов
        
        QuestApiClient.getAllQuests()
            .thenAccept(allQuests -> {
                if (!allQuests.isEmpty()) {
                    long currentTime = world.getTime();
                    int totalQuests = 0;
                    
                    questCache.clear(); // Очищаем кэш перед обновлением
                    for (String playerClass : PLAYER_CLASSES) {
                        List<Quest> classQuests = allQuests.getOrDefault(playerClass, new ArrayList<>());
                        questCache.put(playerClass, classQuests);
                        lastUpdateTime.put(playerClass, currentTime);
                        totalQuests += classQuests.size();
                        
                        Origins.LOGGER.info("📋 Loaded " + classQuests.size() + " quests for class: " + playerClass);
                        QuestApiChatLogger.logApiSuccess(world.getServer(), playerClass, classQuests.size());
                        updateBoardsForClass(playerClass, world);
                    }
                    
                    Origins.LOGGER.info("🎯 TOTAL: Loaded " + totalQuests + " quests for all classes!");
                    QuestApiChatLogger.logApiSuccess(world.getServer(), "ALL CLASSES", totalQuests); // Сообщение о завершении
                } else {
                    Origins.LOGGER.warn("❌ No quests received from optimized API");
                    QuestApiChatLogger.logApiError(world.getServer(), "ALL CLASSES", "Не получено квестов");
                }
            })
            .exceptionally(throwable -> {
                Origins.LOGGER.error("🔥 Failed to load quests from optimized API", throwable);
                QuestApiChatLogger.logApiError(world.getServer(), "ALL CLASSES", throwable.getMessage());
                return null;
            });
    }

    /**
     * Обновляет все доски объявлений указанного класса
     */
    private void updateBoardsForClass(String playerClass, ServerWorld world) {
        List<Quest> quests = questCache.get(playerClass);
        if (quests == null || quests.isEmpty()) {
            return;
        }
        
        // Здесь должна быть логика поиска всех досок в мире
        Origins.LOGGER.info("Updated boards for class: " + playerClass + " with " + quests.size() + " quests");
        // Пример: предполагаем, что доски обновляются через событие или другой механизм
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
    public long getTimeUntilNextUpdate(String playerClass) {
        Long lastUpdate = lastUpdateTime.get(playerClass);
        if (lastUpdate == null) {
            return 0;
        }
        
        long timeSinceUpdate = System.currentTimeMillis() / 50 - lastUpdate;
        return Math.max(0, UPDATE_INTERVAL_TICKS - timeSinceUpdate);
    }
    
    /**
     * Получает время до следующего обновления в минутах
     */
    public int getMinutesUntilNextUpdate(String playerClass) {
        long ticksUntilUpdate = getTimeUntilNextUpdate(playerClass);
        return (int) (ticksUntilUpdate / (20 * 60));
    }
    
    /**
     * Принудительно обновляет квесты для указанного класса
     */
    public void forceUpdateClass(String playerClass, ServerWorld world) {
        Origins.LOGGER.info("Force updating quests for class: " + playerClass);
        loadAllQuests(world); // Используем один запрос для всех классов
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
}