package io.github.apace100.origins.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit тесты для QuestProgressTracker
 */
@ExtendWith(MockitoExtension.class)
public class QuestProgressTrackerTest {
    
    @Mock
    private PlayerEntity mockPlayer;
    
    @Mock
    private ItemStack mockItemStack;
    
    @Mock
    private QuestObjective mockObjective;
    
    private QuestProgressTracker tracker;
    
    @BeforeEach
    void setUp() {
        tracker = QuestProgressTracker.getInstance();
    }
    
    @Test
    void testGetInstance_ReturnsSameInstance() {
        // Arrange & Act
        QuestProgressTracker instance1 = QuestProgressTracker.getInstance();
        QuestProgressTracker instance2 = QuestProgressTracker.getInstance();
        
        // Assert
        assertSame(instance1, instance2, "getInstance должен возвращать один и тот же экземпляр");
    }
    
    @Test
    void testTrackPlayerAction_WithNullPlayer_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            tracker.trackPlayerAction(null, "collect", "minecraft:dirt", 1);
        }, "trackPlayerAction не должен выбрасывать исключения для null игрока");
    }
    
    @Test
    void testTrackPlayerAction_WithNullAction_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            tracker.trackPlayerAction(mockPlayer, null, "minecraft:dirt", 1);
        }, "trackPlayerAction не должен выбрасывать исключения для null действия");
    }
    
    @Test
    void testUpdateTicketProgress_WithEmptyStack_DoesNotThrow() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            tracker.updateTicketProgress(mockPlayer, mockItemStack);
        }, "updateTicketProgress не должен выбрасывать исключения для пустого ItemStack");
    }
    
    @Test
    void testUpdateTicketProgress_WithNullPlayer_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            tracker.updateTicketProgress(null, mockItemStack);
        }, "updateTicketProgress не должен выбрасывать исключения для null игрока");
    }
    
    @Test
    void testCheckObjectiveCompletion_WithNullObjective_ReturnsFalse() {
        // Arrange & Act
        boolean result = tracker.checkObjectiveCompletion(null, mockPlayer);
        
        // Assert
        assertFalse(result, "checkObjectiveCompletion должен возвращать false для null цели");
    }
    
    @Test
    void testCheckObjectiveCompletion_WithNullPlayer_ReturnsFalse() {
        // Arrange & Act
        boolean result = tracker.checkObjectiveCompletion(mockObjective, null);
        
        // Assert
        assertFalse(result, "checkObjectiveCompletion должен возвращать false для null игрока");
    }
    
    @Test
    void testCheckObjectiveCompletion_WithCompletedObjective_ReturnsFalse() {
        // Arrange
        when(mockObjective.isCompleted()).thenReturn(true);
        
        // Act
        boolean result = tracker.checkObjectiveCompletion(mockObjective, mockPlayer);
        
        // Assert
        assertFalse(result, "checkObjectiveCompletion должен возвращать false для уже выполненной цели");
    }
    
    @Test
    void testSyncProgressToClient_WithNullPlayer_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            tracker.syncProgressToClient(null);
        }, "syncProgressToClient не должен выбрасывать исключения для null игрока");
    }
}