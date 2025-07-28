package io.github.apace100.origins.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для QuestTicketState
 */
public class QuestTicketStateTest {
    
    @Test
    void testFromName_WithValidName_ReturnsCorrectState() {
        // Arrange & Act
        QuestTicketState result = QuestTicketState.fromName("accepted");
        
        // Assert
        assertEquals(QuestTicketState.ACCEPTED, result, "fromName должен возвращать правильное состояние");
    }
    
    @Test
    void testFromName_WithInvalidName_ReturnsAvailable() {
        // Arrange & Act
        QuestTicketState result = QuestTicketState.fromName("invalid_state");
        
        // Assert
        assertEquals(QuestTicketState.AVAILABLE, result, "fromName должен возвращать AVAILABLE для неизвестного состояния");
    }
    
    @Test
    void testFromName_WithNull_ReturnsAvailable() {
        // Arrange & Act
        QuestTicketState result = QuestTicketState.fromName(null);
        
        // Assert
        assertEquals(QuestTicketState.AVAILABLE, result, "fromName должен возвращать AVAILABLE для null");
    }
    
    @Test
    void testCanComplete_WithCompletedState_ReturnsTrue() {
        // Arrange & Act
        boolean result = QuestTicketState.COMPLETED.canComplete();
        
        // Assert
        assertTrue(result, "COMPLETED состояние должно позволять завершение квеста");
    }
    
    @Test
    void testCanComplete_WithAvailableState_ReturnsFalse() {
        // Arrange & Act
        boolean result = QuestTicketState.AVAILABLE.canComplete();
        
        // Assert
        assertFalse(result, "AVAILABLE состояние не должно позволять завершение квеста");
    }
    
    @Test
    void testIsActive_WithAcceptedState_ReturnsTrue() {
        // Arrange & Act
        boolean result = QuestTicketState.ACCEPTED.isActive();
        
        // Assert
        assertTrue(result, "ACCEPTED состояние должно быть активным");
    }
    
    @Test
    void testIsActive_WithInProgressState_ReturnsTrue() {
        // Arrange & Act
        boolean result = QuestTicketState.IN_PROGRESS.isActive();
        
        // Assert
        assertTrue(result, "IN_PROGRESS состояние должно быть активным");
    }
    
    @Test
    void testIsActive_WithCompletedState_ReturnsTrue() {
        // Arrange & Act
        boolean result = QuestTicketState.COMPLETED.isActive();
        
        // Assert
        assertTrue(result, "COMPLETED состояние должно быть активным");
    }
    
    @Test
    void testIsActive_WithAvailableState_ReturnsFalse() {
        // Arrange & Act
        boolean result = QuestTicketState.AVAILABLE.isActive();
        
        // Assert
        assertFalse(result, "AVAILABLE состояние не должно быть активным");
    }
    
    @Test
    void testIsActive_WithFinishedState_ReturnsFalse() {
        // Arrange & Act
        boolean result = QuestTicketState.FINISHED.isActive();
        
        // Assert
        assertFalse(result, "FINISHED состояние не должно быть активным");
    }
    
    @Test
    void testGetName_ReturnsCorrectName() {
        // Arrange & Act
        String result = QuestTicketState.ACCEPTED.getName();
        
        // Assert
        assertEquals("accepted", result, "getName должен возвращать правильное название");
    }
    
    @Test
    void testGetDisplayName_ReturnsCorrectDisplayName() {
        // Arrange & Act
        String result = QuestTicketState.ACCEPTED.getDisplayName();
        
        // Assert
        assertEquals("Принят", result, "getDisplayName должен возвращать правильное отображаемое название");
    }
    
    @Test
    void testAllStatesHaveUniqueNames() {
        // Arrange
        QuestTicketState[] states = QuestTicketState.values();
        
        // Act & Assert
        for (int i = 0; i < states.length; i++) {
            for (int j = i + 1; j < states.length; j++) {
                assertNotEquals(states[i].getName(), states[j].getName(), 
                    "Все состояния должны иметь уникальные названия");
            }
        }
    }
}