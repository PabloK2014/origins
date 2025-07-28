package io.github.apace100.origins.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit тесты для QuestInventoryManager
 */
@ExtendWith(MockitoExtension.class)
public class QuestInventoryManagerTest {
    
    @Mock
    private PlayerEntity mockPlayer;
    
    @Mock
    private PlayerInventory mockInventory;
    
    @Mock
    private Quest mockQuest;
    
    @Mock
    private ItemStack mockItemStack;
    
    private QuestInventoryManager manager;
    
    @BeforeEach
    void setUp() {
        manager = QuestInventoryManager.getInstance();
        when(mockPlayer.getInventory()).thenReturn(mockInventory);
    }
    
    @Test
    void testGetInstance_ReturnsSameInstance() {
        // Arrange & Act
        QuestInventoryManager instance1 = QuestInventoryManager.getInstance();
        QuestInventoryManager instance2 = QuestInventoryManager.getInstance();
        
        // Assert
        assertSame(instance1, instance2, "getInstance должен возвращать один и тот же экземпляр");
    }
    
    @Test
    void testFindQuestTickets_WithNullPlayer_ReturnsEmptyList() {
        // Arrange & Act
        List<ItemStack> result = manager.findQuestTickets(null);
        
        // Assert
        assertNotNull(result, "findQuestTickets не должен возвращать null");
        assertTrue(result.isEmpty(), "findQuestTickets должен возвращать пустой список для null игрока");
    }
    
    @Test
    void testGetActiveQuestTicket_WithNullPlayer_ReturnsEmpty() {
        // Arrange & Act
        ItemStack result = manager.getActiveQuestTicket(null, "test_quest");
        
        // Assert
        assertTrue(result.isEmpty(), "getActiveQuestTicket должен возвращать пустой ItemStack для null игрока");
    }
    
    @Test
    void testGetActiveQuestTicket_WithNullQuestId_ReturnsEmpty() {
        // Arrange & Act
        ItemStack result = manager.getActiveQuestTicket(mockPlayer, null);
        
        // Assert
        assertTrue(result.isEmpty(), "getActiveQuestTicket должен возвращать пустой ItemStack для null questId");
    }
    
    @Test
    void testHasReachedQuestLimit_WithNullPlayer_ReturnsTrue() {
        // Arrange & Act
        boolean result = manager.hasReachedQuestLimit(null);
        
        // Assert
        assertTrue(result, "hasReachedQuestLimit должен возвращать true для null игрока");
    }
    
    @Test
    void testAddQuestTicketToInventory_WithNullPlayer_ReturnsFalse() {
        // Arrange & Act
        boolean result = manager.addQuestTicketToInventory(null, mockQuest);
        
        // Assert
        assertFalse(result, "addQuestTicketToInventory должен возвращать false для null игрока");
    }
    
    @Test
    void testAddQuestTicketToInventory_WithNullQuest_ReturnsFalse() {
        // Arrange & Act
        boolean result = manager.addQuestTicketToInventory(mockPlayer, null);
        
        // Assert
        assertFalse(result, "addQuestTicketToInventory должен возвращать false для null квеста");
    }
    
    @Test
    void testRemoveQuestTicketFromInventory_WithNullPlayer_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            manager.removeQuestTicketFromInventory(null, "test_quest");
        }, "removeQuestTicketFromInventory не должен выбрасывать исключения для null игрока");
    }
    
    @Test
    void testRemoveQuestTicketFromInventory_WithNullQuestId_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            manager.removeQuestTicketFromInventory(mockPlayer, null);
        }, "removeQuestTicketFromInventory не должен выбрасывать исключения для null questId");
    }
    
    @Test
    void testGetActiveQuestCount_WithNullPlayer_ReturnsZero() {
        // Arrange & Act
        int result = manager.getActiveQuestCount(null);
        
        // Assert
        assertEquals(0, result, "getActiveQuestCount должен возвращать 0 для null игрока");
    }
    
    @Test
    void testHasQuestTicket_WithNullPlayer_ReturnsFalse() {
        // Arrange & Act
        boolean result = manager.hasQuestTicket(null, "test_quest");
        
        // Assert
        assertFalse(result, "hasQuestTicket должен возвращать false для null игрока");
    }
    
    @Test
    void testHasQuestTicket_WithNullQuestId_ReturnsFalse() {
        // Arrange & Act
        boolean result = manager.hasQuestTicket(mockPlayer, null);
        
        // Assert
        assertFalse(result, "hasQuestTicket должен возвращать false для null questId");
    }
    
    @Test
    void testClearAllQuestTickets_WithNullPlayer_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            manager.clearAllQuestTickets(null);
        }, "clearAllQuestTickets не должен выбрасывать исключения для null игрока");
    }
    
    @Test
    void testGetMaxActiveQuests_ReturnsPositiveValue() {
        // Arrange & Act
        int result = manager.getMaxActiveQuests();
        
        // Assert
        assertTrue(result > 0, "getMaxActiveQuests должен возвращать положительное значение");
    }
}