package io.github.apace100.origins.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit тесты для QuestTicketItem
 */
@ExtendWith(MockitoExtension.class)
public class QuestTicketItemTest {
    
    @Mock
    private ItemStack mockItemStack;
    
    @Mock
    private NbtCompound mockNbt;
    
    @BeforeEach
    void setUp() {
        when(mockItemStack.isEmpty()).thenReturn(false);
        when(mockItemStack.getNbt()).thenReturn(mockNbt);
        when(mockItemStack.getOrCreateNbt()).thenReturn(mockNbt);
    }
    
    @Test
    void testIsQuestTicket_WithEmptyStack_ReturnsFalse() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act
        boolean result = QuestTicketItem.isQuestTicket(mockItemStack);
        
        // Assert
        assertFalse(result, "isQuestTicket должен возвращать false для пустого ItemStack");
    }
    
    @Test
    void testIsAccepted_WithEmptyStack_ReturnsFalse() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act
        boolean result = QuestTicketItem.isAccepted(mockItemStack);
        
        // Assert
        assertFalse(result, "isAccepted должен возвращать false для пустого ItemStack");
    }
    
    @Test
    void testIsAccepted_WithNullNbt_ReturnsFalse() {
        // Arrange
        when(mockItemStack.getNbt()).thenReturn(null);
        
        // Act
        boolean result = QuestTicketItem.isAccepted(mockItemStack);
        
        // Assert
        assertFalse(result, "isAccepted должен возвращать false для null NBT");
    }
    
    @Test
    void testMarkAsAccepted_WithEmptyStack_DoesNotThrow() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            QuestTicketItem.markAsAccepted(mockItemStack, System.currentTimeMillis());
        }, "markAsAccepted не должен выбрасывать исключения для пустого ItemStack");
    }
    
    @Test
    void testMarkAsAccepted_WithValidStack_SetsCorrectState() {
        // Arrange
        long acceptTime = System.currentTimeMillis();
        
        // Act
        QuestTicketItem.markAsAccepted(mockItemStack, acceptTime);
        
        // Assert
        verify(mockNbt).putString("quest_state", QuestTicketState.ACCEPTED.getName());
        verify(mockNbt).putLong("accept_time", acceptTime);
    }
    
    @Test
    void testUpdateProgress_WithEmptyStack_DoesNotThrow() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        QuestObjective mockObjective = mock(QuestObjective.class);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            QuestTicketItem.updateProgress(mockItemStack, mockObjective);
        }, "updateProgress не должен выбрасывать исключения для пустого ItemStack");
    }
    
    @Test
    void testUpdateProgress_WithNullObjective_DoesNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            QuestTicketItem.updateProgress(mockItemStack, null);
        }, "updateProgress не должен выбрасывать исключения для null objective");
    }
    
    @Test
    void testIsReadyForCompletion_WithEmptyStack_ReturnsFalse() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act
        boolean result = QuestTicketItem.isReadyForCompletion(mockItemStack);
        
        // Assert
        assertFalse(result, "isReadyForCompletion должен возвращать false для пустого ItemStack");
    }
    
    @Test
    void testIsReadyForCompletion_WithNullNbt_ReturnsFalse() {
        // Arrange
        when(mockItemStack.getNbt()).thenReturn(null);
        
        // Act
        boolean result = QuestTicketItem.isReadyForCompletion(mockItemStack);
        
        // Assert
        assertFalse(result, "isReadyForCompletion должен возвращать false для null NBT");
    }
    
    @Test
    void testGetTicketState_WithEmptyStack_ReturnsAvailable() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act
        QuestTicketState result = QuestTicketItem.getTicketState(mockItemStack);
        
        // Assert
        assertEquals(QuestTicketState.AVAILABLE, result, 
            "getTicketState должен возвращать AVAILABLE для пустого ItemStack");
    }
    
    @Test
    void testGetTicketState_WithNullNbt_ReturnsAvailable() {
        // Arrange
        when(mockItemStack.getNbt()).thenReturn(null);
        
        // Act
        QuestTicketState result = QuestTicketItem.getTicketState(mockItemStack);
        
        // Assert
        assertEquals(QuestTicketState.AVAILABLE, result, 
            "getTicketState должен возвращать AVAILABLE для null NBT");
    }
    
    @Test
    void testGetAcceptTime_WithEmptyStack_ReturnsZero() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act
        long result = QuestTicketItem.getAcceptTime(mockItemStack);
        
        // Assert
        assertEquals(0L, result, "getAcceptTime должен возвращать 0 для пустого ItemStack");
    }
    
    @Test
    void testGetAcceptTime_WithNullNbt_ReturnsZero() {
        // Arrange
        when(mockItemStack.getNbt()).thenReturn(null);
        
        // Act
        long result = QuestTicketItem.getAcceptTime(mockItemStack);
        
        // Assert
        assertEquals(0L, result, "getAcceptTime должен возвращать 0 для null NBT");
    }
    
    @Test
    void testAddVisualCompletionEffect_WithEmptyStack_DoesNotThrow() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            QuestTicketItem.addVisualCompletionEffect(mockItemStack);
        }, "addVisualCompletionEffect не должен выбрасывать исключения для пустого ItemStack");
    }
    
    @Test
    void testHasVisualEffects_WithEmptyStack_ReturnsFalse() {
        // Arrange
        when(mockItemStack.isEmpty()).thenReturn(true);
        
        // Act
        boolean result = QuestTicketItem.hasVisualEffects(mockItemStack);
        
        // Assert
        assertFalse(result, "hasVisualEffects должен возвращать false для пустого ItemStack");
    }
    
    @Test
    void testHasVisualEffects_WithNullNbt_ReturnsFalse() {
        // Arrange
        when(mockItemStack.getNbt()).thenReturn(null);
        
        // Act
        boolean result = QuestTicketItem.hasVisualEffects(mockItemStack);
        
        // Assert
        assertFalse(result, "hasVisualEffects должен возвращать false для null NBT");
    }
}