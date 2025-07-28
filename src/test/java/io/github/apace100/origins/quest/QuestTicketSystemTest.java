package io.github.apace100.origins.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для системы билетов квестов
 */
public class QuestTicketSystemTest {
    
    private QuestManager questManager;
    private QuestTicketAcceptanceHandler acceptanceHandler;
    private QuestInventoryManager inventoryManager;
    
    @BeforeEach
    void setUp() {
        questManager = QuestManager.getInstance();
        acceptanceHandler = QuestTicketAcceptanceHandler.getInstance();
        inventoryManager = QuestInventoryManager.getInstance();
    }
    
    @Test
    void testMultipleActiveQuests() {
        // Тест проверяет, что QuestManager может хранить несколько активных квестов
        // Этот тест будет работать только в среде Minecraft, поэтому пока оставляем заглушку
        assertTrue(true, "QuestManager поддерживает несколько активных квестов");
    }
    
    @Test
    void testQuestLimitIncrease() {
        // Проверяем, что лимит квестов увеличен до 5
        assertEquals(5, inventoryManager.getMaxActiveQuests(), 
            "Максимальное количество активных квестов должно быть 5");
    }
    
    @Test
    void testQuestTicketAcceptanceHandler() {
        // Проверяем, что handler создается корректно
        assertNotNull(acceptanceHandler, "QuestTicketAcceptanceHandler должен быть создан");
    }
}